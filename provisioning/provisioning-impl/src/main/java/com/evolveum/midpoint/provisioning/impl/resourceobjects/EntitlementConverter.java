/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectDiscriminator;
import com.evolveum.midpoint.provisioning.impl.ResourceObjectOperations;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;

/**
 * Class that collects the entitlement-related methods used by ResourceObjectConverter
 *
 * Should NOT be used by any class other than ResourceObjectConverter.
 *
 * @author Radovan Semancik
 *
 */
@Component
class EntitlementConverter {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementConverter.class);

    @Autowired private ResourceObjectReferenceResolver resourceObjectReferenceResolver;
    @Autowired private DelineationProcessor delineationProcessor;
    @Autowired private PrismContext prismContext;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;

    //region Read (get)
    //////////
    // GET
    /////////

    /**
     * Creates an associations container (`association`) based on the relevant resource object attribute values.
     * Note that for "object to subject" entitlements this involves a search operation.
     */
    void postProcessEntitlementsRead(
            PrismObject<ShadowType> resourceObject,
            ProvisioningContext subjectCtx,
            OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        LOGGER.trace("Starting postProcessEntitlementRead");

        PrismContainer<ShadowAssociationType> associationContainer = resourceObject.getDefinition()
                .<ShadowAssociationType>findContainerDefinition(ShadowType.F_ASSOCIATION)
                .instantiate();

        for (ResourceAssociationDefinition associationDef : subjectCtx.getAssociationDefinitions()) {
            ShadowKindType entitlementKind = associationDef.getKind();
            for (String entitlementIntent: associationDef.getIntents()) {
                LOGGER.trace("Resolving association {} for kind {} and intent {}",
                        associationDef.getName(), entitlementKind, entitlementIntent);
                ProvisioningContext entitlementCtx = subjectCtx.spawnForKindIntent(entitlementKind, entitlementIntent);
                ResourceObjectAssociationDirectionType direction = associationDef.getDirection();
                if (direction == ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT) {
                    postProcessReadSubjectToEntitlement(
                            associationContainer,
                            resourceObject,
                            associationDef.getDefinitionBean().getAssociationAttribute(),
                            associationDef.getDefinitionBean().getValueAttribute(),
                            associationDef,
                            subjectCtx,
                            entitlementCtx);
                } else if (direction == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
                    if (associationDef.getDefinitionBean().getShortcutAssociationAttribute() != null) {
                        postProcessReadSubjectToEntitlement(
                                associationContainer,
                                resourceObject,
                                associationDef.getDefinitionBean().getShortcutAssociationAttribute(),
                                associationDef.getDefinitionBean().getShortcutValueAttribute(),
                                associationDef,
                                subjectCtx,
                                entitlementCtx
                        );
                    } else {
                        postProcessReadEntitlementToSubject(
                                associationContainer,
                                resourceObject,
                                associationDef,
                                subjectCtx,
                                entitlementCtx,
                                result);
                    }
                } else {
                    throw new IllegalArgumentException("Unknown entitlement direction " + direction + " in association "
                            + associationDef + " in " + subjectCtx);
                }
            }
        }

        if (!associationContainer.isEmpty()) {
            resourceObject.add(associationContainer);
        }
        LOGGER.trace("Finished postProcessEntitlementRead with association container having {} item(s)",
                associationContainer.size());
    }

    /**
     * Creates a value in `associationContainer`. It simply uses values in "association" (referencing) attribute
     * to construct identifiers pointing to the entitlement object.
     *
     * @param referencingAttrName The "referencing" attribute (aka association attribute), present on subject, e.g. memberOf.
     * @param referencedAttrName The "referenced" attribute (aka value attribute) present on object, e.g. dn (Group DN).
     *
     * @see #createAssociationValueFromIdentifier(PrismContainer, PrismPropertyValue, ResourceAttributeDefinition, QName,
     * ResourceObjectDefinition)
     */
    private <S extends ShadowType,T> void postProcessReadSubjectToEntitlement(
            PrismContainer<ShadowAssociationType> associationContainer,
            PrismObject<S> resourceObject,
            QName referencingAttrName,
            QName referencedAttrName,
            ResourceAssociationDefinition associationDef,
            ProvisioningContext subjectCtx,
            ProvisioningContext entitlementCtx) throws SchemaException {

        QName associationName = associationDef.getName();

        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();
        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceObject);

        schemaCheck(referencingAttrName != null,
                "No association attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);
        schemaCheck(referencedAttrName != null,
                "No value attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);

        subjectDef.findAttributeDefinitionRequired(
                referencingAttrName,
                () -> " in entitlement association '" + associationName + "' in " + subjectCtx + " [association attribute]");

        //noinspection unchecked
        ResourceAttributeDefinition<T> referencedAttrDef =
                (ResourceAttributeDefinition<T>)
                        entitlementDef.findAttributeDefinitionRequired(
                                referencedAttrName,
                                () -> " in entitlement association '" + associationName + "' in " + subjectCtx + " [value attribute]");

        ResourceAttribute<T> referencingAttr = attributesContainer.findAttribute(referencingAttrName);
        if (referencingAttr != null && !referencingAttr.isEmpty()) {
            for (PrismPropertyValue<T> referencingAttrValue : referencingAttr.getValues()) {
                PrismContainerValue<ShadowAssociationType> associationContainerValue =
                        createAssociationValueFromIdentifier(
                                associationContainer, referencingAttrValue, referencedAttrDef, associationName, entitlementDef);

                // NOTE: Those values are not filtered according to kind/intent of the association target. Therefore there may be values
                // that do not belong here (see MID-5790). But that is OK for now. We will filter those values out later when
                // read the shadows and determine shadow OID
                LOGGER.trace("Association attribute value resolved to association container value {}", associationContainerValue);
            }
        } else {
            // Nothing to do. No attribute to base the association on.
            LOGGER.trace("Association attribute {} is empty, skipping association {}", referencingAttrName, associationName);
        }
    }

    /**
     * Creates values in `associationContainer`. It searches for entitlements having the "association" (referencing) attribute
     * value - e.g. `ri:members` - containing the value in subject "value" (referenced) attribute - e.g. `ri:dn`.
     */
    private <S extends ShadowType, T> void postProcessReadEntitlementToSubject(
            PrismContainer<ShadowAssociationType> associationContainer,
            PrismObject<S> resourceObject,
            ResourceAssociationDefinition associationDef,
            ProvisioningContext subjectCtx,
            ProvisioningContext entitlementCtx,
            OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        QName associationName = associationDef.getName();

        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();
        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceObject);

        QName associationAuxiliaryObjectClass = associationDef.getAuxiliaryObjectClass();
        if (associationAuxiliaryObjectClass != null && !subjectDef.hasAuxiliaryObjectClass(associationAuxiliaryObjectClass)) {
            LOGGER.trace("Ignoring association {} because subject does not have auxiliary object class {}, it has {}",
                    associationName, associationAuxiliaryObjectClass, subjectDef.getAuxiliaryDefinitions());
            return;
        }

        QName referencingAttrName = associationDef.getDefinitionBean().getAssociationAttribute(); // e.g. ri:members
        QName referencedAttrName = associationDef.getDefinitionBean().getValueAttribute(); // e.g. ri:dn

        schemaCheck(referencingAttrName != null,
                "No association attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);
        schemaCheck(referencedAttrName != null,
                "No value attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);

        ResourceAttributeDefinition<?> referencingAttrDef = entitlementDef.findAttributeDefinitionRequired(
                referencingAttrName,
                () -> " in entitlement association '" + associationName + "' in " + entitlementCtx + " [association attribute]");

        ResourceAttribute<T> referencedAttr = attributesContainer.findAttribute(referencedAttrName);
        if (referencedAttr == null || referencedAttr.isEmpty()) {
            LOGGER.trace("Ignoring association {} because subject does not have any value in attribute {}",
                    associationName, referencedAttrName);
            return;
        }
        if (referencedAttr.size() > 1) {
            throw new SchemaException("Referenced value attribute " + referencedAttrName + " has more than one value; "
                    + "it is the attribute defined in entitlement association '" + associationName + "' in " + subjectCtx);
        }
        ResourceAttributeDefinition<T> referencedAttrDef = referencedAttr.getDefinition();
        PrismPropertyValue<T> referencedAttrValue = referencedAttr.getAnyValue();

        ObjectQuery query = createEntitlementQuery(referencedAttrValue, referencedAttrDef, referencingAttrDef, associationDef);

        executeSearchForEntitlements(
                associationContainer, resourceObject, query, associationName, subjectCtx, entitlementCtx, result);
    }

    /**
     * Executes the search for entitlements using the prepared query.
     */
    private <S extends ShadowType> void executeSearchForEntitlements(
            PrismContainer<ShadowAssociationType> associationContainer,
            PrismObject<S> resourceObject,
            ObjectQuery explicitQuery,
            QName associationName,
            ProvisioningContext subjectCtx,
            ProvisioningContext entitlementCtx,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {

        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        QueryWithConstraints queryWithConstraints =
                delineationProcessor.determineQueryWithConstraints(entitlementCtx, explicitQuery, result);

        UcfObjectHandler handler = (ucfObject, lResult) -> {
            PrismObject<ShadowType> entitlementResourceObject = ucfObject.getResourceObject();

            try {
                createAssociationValueFromTarget(associationContainer, entitlementResourceObject, associationName, entitlementDef);
            } catch (SchemaException e) {
                throw new TunnelException(e);
            }

            LOGGER.trace("Processed entitlement-to-subject association for account {} and entitlement {}",
                    ShadowUtil.getHumanReadableNameLazily(resourceObject),
                    ShadowUtil.getHumanReadableNameLazily(entitlementResourceObject));

            return true;
        };

        ConnectorInstance connector = subjectCtx.getConnector(ReadCapabilityType.class, result);
        try {
            LOGGER.trace("Processing entitlement-to-subject association for account {}: query {}",
                    ShadowUtil.getHumanReadableNameLazily(resourceObject), queryWithConstraints.query);
            try {
                connector.search(
                        entitlementDef,
                        queryWithConstraints.query,
                        handler,
                        entitlementCtx.createAttributesToReturn(),
                        null,
                        queryWithConstraints.constraints,
                        UcfFetchErrorReportingMethod.EXCEPTION,
                        subjectCtx.getUcfExecutionContext(),
                        result);
            } catch (GenericFrameworkException e) {
                throw new GenericConnectorException("Generic error in the connector " + connector + ". Reason: "
                        + e.getMessage(), e);
            }
        } catch (TunnelException e) {
            throw (SchemaException)e.getCause();
        }
    }

    /**
     * Creates association value from known identifier value; and inserts it into `associationContainer`.
     *
     *     PCV:
     *       name: association name
     *       identifiers: { valueAttr: value }
     *
     * For example, if
     *
     * - identifier value = "cn=wheel,ou=Groups,dc=example,dc=com"
     * - referenced identifier is "dn" (of a group)
     * - association name = "ri:groups"
     *
     * then the result would be:
     *
     *     PCV:
     *       name: ri:groups
     *       identifiers: { dn: "cn=wheel,ou=Groups,dc=example,dc=com" }
     */
    private <T> PrismContainerValue<ShadowAssociationType> createAssociationValueFromIdentifier(
            PrismContainer<ShadowAssociationType> associationContainer,
            PrismPropertyValue<T> identifierValue,
            ResourceAttributeDefinition<T> referencedIdentifierDef,
            QName associationName,
            ResourceObjectDefinition entitlementDef) throws SchemaException {

        PrismContainerValue<ShadowAssociationType> associationContainerValue = associationContainer.createNewValue();
        associationContainerValue.asContainerable().setName(associationName);
        associationContainerValue.add(
                createIdentifiersContainerForIdentifierValue(identifierValue, referencedIdentifierDef, entitlementDef));

        return associationContainerValue;
    }

    /**
     * Creates association value from resolved target object; and inserts it into `associationContainer`.
     *
     * For example, if
     *
     * - target object is the group of = "cn=wheel,ou=Groups,dc=example,dc=com"
     * - association name = "ri:groups"
     *
     * then the result would be:
     *
     *     PCV:
     *       name: ri:groups
     *       identifiers: { dn: "cn=wheel,ou=Groups,dc=example,dc=com" }
     */
    private void createAssociationValueFromTarget(
            PrismContainer<ShadowAssociationType> associationContainer,
            PrismObject<ShadowType> targetResourceObject,
            QName associationName,
            ResourceObjectDefinition entitlementDef) throws SchemaException {

        PrismContainerValue<ShadowAssociationType> associationContainerValue = associationContainer.createNewValue();
        associationContainerValue.asContainerable().setName(associationName);
        associationContainerValue.add(
                createIdentifiersContainerForTargetObject(targetResourceObject, entitlementDef));
    }

    /**
     * Creates the identifiers container from given value of an identifier.
     */
    @NotNull
    private <T> ResourceAttributeContainer createIdentifiersContainerForIdentifierValue(
            PrismPropertyValue<T> identifierValue,
            ResourceAttributeDefinition<T> referencedIdentifierDef,
            ResourceObjectDefinition entitlementDef) throws SchemaException {
        ResourceAttributeContainer identifiersContainer = ObjectFactory.createResourceAttributeContainer(
                ShadowAssociationType.F_IDENTIFIERS, entitlementDef.toResourceAttributeContainerDefinition());
        ResourceAttribute<T> referencedIdentifier = referencedIdentifierDef.instantiate();
        referencedIdentifier.add(identifierValue.clone());
        identifiersContainer.add(referencedIdentifier);
        return identifiersContainer;
    }

    /**
     * Creates the identifiers container for resolved target entitlement object.
     */
    @NotNull
    private ResourceAttributeContainer createIdentifiersContainerForTargetObject(
            PrismObject<ShadowType> targetResourceObject,
            ResourceObjectDefinition entitlementDef) throws SchemaException {

        ResourceAttributeContainer identifiersContainer = ObjectFactory.createResourceAttributeContainer(
                ShadowAssociationType.F_IDENTIFIERS, entitlementDef.toResourceAttributeContainerDefinition());
        identifiersContainer.getValue().addAll(
                Item.cloneCollection(
                        emptyIfNull(
                                ShadowUtil.getAllIdentifiers(targetResourceObject))));

        // Remember the full shadow. This is used later as an optimization to create the shadow in repo
        identifiersContainer.setUserData(ResourceObjectConverter.FULL_SHADOW_KEY, targetResourceObject);
        return identifiersContainer;
    }

    /**
     * Creates a query that will select the entitlements for the subject.
     *
     * Entitlements point to subject using referencing ("association") attribute e.g. `ri:members`.
     * Subject is pointed to using referenced ("value") attribute, e.g. `ri:dn`.
     *
     * @param referencedAttrValue Value of the referenced ("value") attribute. E.g. uid=jack,ou=People,dc=example,dc=org.
     * @param referencedAttrDef Definition of the referenced ("value") attribute, e.g. ri:dn in account object class.
     * @param referencingAttrDef Definition of the referencing ("association") attribute, e.g. "members"
     */
    private <TV,TA> ObjectQuery createEntitlementQuery(
            PrismPropertyValue<TV> referencedAttrValue,
            ResourceAttributeDefinition<TV> referencedAttrDef,
            ResourceAttributeDefinition<TA> referencingAttrDef,
            ResourceAssociationDefinition associationDef)
            throws SchemaException{

        // This is the value we look for in the entitlements (e.g. specific DN that should be their member).
        TA normalizedRealValue =
                getRealNormalizedConvertedValue(referencedAttrValue, referencedAttrDef, referencingAttrDef, associationDef);

        LOGGER.trace("Going to look for entitlements using value: {} ({}) def={}",
                normalizedRealValue, normalizedRealValue.getClass(), referencingAttrDef);
        ObjectQuery query = prismContext.queryFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, referencingAttrDef.getItemName()), referencingAttrDef)
                    .eq(normalizedRealValue)
                .build();
        query.setAllowPartialResults(true);
        return query;
    }

    /**
     * Converts the value from source form (e.g. account DN attr) to target form (e.g. group member attr),
     * and normalize according to the matching rule defined for the association.
     *
     * TODO what about matching rule for the target definition?
     */
    private <TV, TA> TA getRealNormalizedConvertedValue(
            PrismPropertyValue<TV> value,
            ResourceAttributeDefinition<TV> sourceDef,
            ResourceAttributeDefinition<TA> targetDef,
            ResourceAssociationDefinition associationDef) throws SchemaException {
        MatchingRule<TA> matchingRule = matchingRuleRegistry.getMatchingRule(
                associationDef.getMatchingRule(),
                targetDef.getTypeName());
        PrismPropertyValue<TA> converted =
                PrismUtil.convertPropertyValue(value, sourceDef, targetDef);
        return matchingRule.normalize(converted.getValue());
    }
    //endregion

    //region Add
    //////////
    // ADD
    /////////

    /**
     * TODO
     */
    void processEntitlementsAdd(PrismObject<ShadowType> subject, ProvisioningContext subjectCtx)
            throws SchemaException {
        PrismContainer<ShadowAssociationType> associationContainer = subject.findContainer(ShadowType.F_ASSOCIATION);
        if (associationContainer == null || associationContainer.isEmpty()) {
            return;
        }

        OperationMap operationMap = new OperationMap();
        collectEntitlementToAttrsDelta(operationMap, associationContainer.getValues(), ModificationType.ADD, subjectCtx);
        for (PropertyModificationOperation<?> operation : operationMap.getOperations()) {
            operation.getPropertyDelta().applyTo(subject);
        }
    }

    /**
     * TODO
     */
    ShadowType collectEntitlementsAsObjectOperationInShadowAdd(
            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap,
            ShadowType subject,
            ProvisioningContext ctx,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        PrismContainer<ShadowAssociationType> associationContainer =
                subject.asPrismObject().findContainer(ShadowType.F_ASSOCIATION);
        if (associationContainer == null || associationContainer.isEmpty()) {
            return subject;
        }
        return collectEntitlementsAsObjectOperation(
                roMap,
                associationContainer.getValues(),
                null,
                subject,
                ModificationType.ADD,
                ctx, result);
    }
    //endregion


    //region Modify
    //////////
    // MODIFY
    /////////

    /**
     * Collects entitlement changes from the shadow to entitlement section into attribute operations.
     * NOTE: only collects  SUBJECT_TO_ENTITLEMENT entitlement direction.
     */
    void collectEntitlementChange(
            Collection<Operation> operations,
            ContainerDelta<ShadowAssociationType> itemDelta,
            ProvisioningContext ctx)
            throws SchemaException {
        OperationMap operationsMap = new OperationMap();

        if (CollectionUtils.isNotEmpty(itemDelta.getValuesToReplace())) {
            LOGGER.error("Replace delta not supported for association, modifications {},\n provisioning context: {}", itemDelta, ctx);
            throw new SchemaException("Cannot perform replace delta for association, replace values: " + itemDelta.getValuesToReplace());
        }
        collectEntitlementToAttrsDelta(operationsMap, itemDelta.getValuesToAdd(), ModificationType.ADD, ctx);
        collectEntitlementToAttrsDelta(operationsMap, itemDelta.getValuesToDelete(), ModificationType.DELETE, ctx);
        collectEntitlementToAttrsDelta(operationsMap, itemDelta.getValuesToReplace(), ModificationType.REPLACE, ctx);

        operations.addAll(operationsMap.getOperations());
    }

    /**
     * TODO
     */
    ShadowType collectEntitlementsAsObjectOperation(
            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap,
            ContainerDelta<ShadowAssociationType> containerDelta,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            ProvisioningContext ctx,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        subjectShadowAfter = collectEntitlementsAsObjectOperation(roMap, containerDelta.getValuesToAdd(),
                subjectShadowBefore, subjectShadowAfter, ModificationType.ADD, ctx, result);
        subjectShadowAfter = collectEntitlementsAsObjectOperation(roMap, containerDelta.getValuesToDelete(),
                subjectShadowBefore, subjectShadowAfter, ModificationType.DELETE, ctx, result);
        subjectShadowAfter = collectEntitlementsAsObjectOperation(roMap, containerDelta.getValuesToReplace(),
                subjectShadowBefore, subjectShadowAfter, ModificationType.REPLACE, ctx, result);
        return subjectShadowAfter;
    }
    //endregion

    //region Delete
    /////////
    // DELETE
    /////////

    /**
     * This is somehow different that all the other methods. We are not following the content of a shadow or delta.
     * We are following the definitions. This is to avoid the need to read the object that is going to be deleted.
     * In fact, the object should not be there any more, but we still want to clean up entitlement membership
     * based on the information from the shadow.
     *
     * @param <T> Type of the association (referencing) attribute
     */
    <T> void collectEntitlementsAsObjectOperationDelete(
            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap,
            ShadowType subjectShadow,
            ProvisioningContext subjectCtx,
            OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();

        Collection<ResourceAssociationDefinition> associationDefs =
                subjectDef.getAssociationDefinitions();
        if (associationDefs.isEmpty()) {
            LOGGER.trace("No associations in deleted shadow, nothing to do");
            return;
        }
        ResourceAttributeContainer subjectAttributesContainer = ShadowUtil.getAttributesContainer(subjectShadow);
        for (ResourceAssociationDefinition associationDef: associationDefs) {
            if (associationDef.getDirection() != ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
                // We can ignore these. They will die together with the object. No need to explicitly delete them.
                LOGGER.trace("Ignoring subject-to-object association in deleted shadow");
                continue;
            }
            if (!associationDef.requiresExplicitReferentialIntegrity()) {
                LOGGER.trace("Ignoring association in deleted shadow because it does not require explicit"
                        + " referential integrity assurance");
                continue;
            }
            if (associationDef.getAuxiliaryObjectClass() != null &&
                    !subjectDef.hasAuxiliaryObjectClass(associationDef.getAuxiliaryObjectClass())) {
                LOGGER.trace("Ignoring association in deleted shadow because subject does not have {} auxiliary object class",
                        associationDef.getAuxiliaryObjectClass());
                continue;
            }

            QName associationName = associationDef.getName();
            ShadowKindType entitlementKind = associationDef.getKind();
            for (String entitlementIntent: associationDef.getIntents()) {
                // TODO deduplicate the code
                ProvisioningContext entitlementCtx = subjectCtx.spawnForKindIntent(entitlementKind, entitlementIntent); // TODO error handling
                ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

                QName assocAttrName = associationDef.getDefinitionBean().getAssociationAttribute();
                QName valueAttrName = associationDef.getDefinitionBean().getValueAttribute();

                schemaCheck(assocAttrName != null,
                        "No association attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);
                schemaCheck(valueAttrName != null,
                        "No value attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);

                //noinspection unchecked
                ResourceAttributeDefinition<T> assocAttrDef =
                        (ResourceAttributeDefinition<T>)
                                entitlementDef.findAttributeDefinitionRequired(
                                        assocAttrName,
                                        () -> " in entitlement association '" + associationName + "' in " + entitlementCtx + " [association attribute]");

                final ResourceAttribute<T> valueAttr = subjectAttributesContainer.findAttribute(valueAttrName);
                if (valueAttr == null || valueAttr.isEmpty()) {
                    // We really want to throw the exception here. We cannot ignore this. If we ignore it then there may be
                    // entitlement membership value left undeleted and this situation will go undetected.
                    // Although we cannot really remedy the situation now, we at least throw an error so the problem is detected.
                    throw new SchemaException("Value attribute "+valueAttrName+" has no value; attribute defined in entitlement "
                            + "association '"+associationName+"' in "+subjectCtx);
                }
                if (valueAttr.size() > 1) {
                    throw new SchemaException("Value attribute "+valueAttrName+" has no more than one value; attribute defined"
                            + " in entitlement association '"+associationName+"' in "+subjectCtx);
                }

                ResourceAttributeDefinition<T> valueAttrDef = valueAttr.getDefinition();
                PrismPropertyValue<T> valueAttrValue = valueAttr.getAnyValue();

                ObjectQuery query = createEntitlementQuery(valueAttrValue, valueAttrDef, assocAttrDef, associationDef);

                QueryWithConstraints queryWithConstraints =
                        delineationProcessor.determineQueryWithConstraints(entitlementCtx, query, result);

                UcfObjectHandler handler = (ucfObject, lResult) -> {
                    PrismObject<ShadowType> entitlementShadow = ucfObject.getResourceObject();
                    Collection<? extends ResourceAttribute<?>> primaryIdentifiers = ShadowUtil.getPrimaryIdentifiers(entitlementShadow);
                    ResourceObjectDiscriminator disc = new ResourceObjectDiscriminator(entitlementDef.getTypeName(), primaryIdentifiers);
                    ResourceObjectOperations operations = roMap.get(disc);
                    if (operations == null) {
                        operations = new ResourceObjectOperations();
                        roMap.put(disc, operations);
                        operations.setResourceObjectContext(entitlementCtx);
                        Collection<? extends ResourceAttribute<?>> allIdentifiers = ShadowUtil.getAllIdentifiers(entitlementShadow);
                        operations.setAllIdentifiers(allIdentifiers);
                    }

                    PropertyDelta<T> attributeDelta = null;
                    for (Operation operation: operations.getOperations()) {
                        if (operation instanceof PropertyModificationOperation) {
                            PropertyModificationOperation<?> propOp = (PropertyModificationOperation<?>)operation;
                            if (propOp.getPropertyDelta().getElementName().equals(assocAttrName)) {
                                //noinspection unchecked
                                attributeDelta = (PropertyDelta<T>) propOp.getPropertyDelta();
                            }
                        }
                    }
                    if (attributeDelta == null) {
                        attributeDelta = assocAttrDef.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, assocAttrName));
                        PropertyModificationOperation<?> attributeModification = new PropertyModificationOperation<>(attributeDelta);
                        attributeModification.setMatchingRuleQName(associationDef.getMatchingRule());
                        operations.add(attributeModification);
                    }

                    attributeDelta.addValuesToDelete(valueAttr.getClonedValues());
                    LOGGER.trace("Association in deleted shadow delta:\n{}", attributeDelta.debugDumpLazily());

                    return true;
                };
                try {
                    LOGGER.trace("Searching for associations in deleted shadow, query: {}", queryWithConstraints.query);
                    ConnectorInstance connector = subjectCtx.getConnector(ReadCapabilityType.class, result);
                    connector.search(
                            entitlementDef,
                            queryWithConstraints.query,
                            handler,
                            entitlementCtx.createAttributesToReturn(),
                            null,
                            queryWithConstraints.constraints,
                            UcfFetchErrorReportingMethod.EXCEPTION,
                            subjectCtx.getUcfExecutionContext(),
                            result);
                } catch (TunnelException e) {
                    throw (SchemaException)e.getCause();
                } catch (GenericFrameworkException e) {
                    throw new GenericConnectorException(e.getMessage(), e);
                }
            }
        }
    }
    //endregion

    //region Common
    /////////
    // common
    /////////

    /**
     * Transforms operations on associations to operations on attributes.
     */
    private void collectEntitlementToAttrsDelta(
            OperationMap operationMap,
            Collection<PrismContainerValue<ShadowAssociationType>> associationValues,
            ModificationType modificationType,
            ProvisioningContext subjectCtx)
            throws SchemaException {
        for (PrismContainerValue<ShadowAssociationType> associationValue : emptyIfNull(associationValues)) {
            collectEntitlementToAttrDelta(operationMap, associationValue, modificationType, subjectCtx);
        }
    }

    /**
     *  Collects entitlement changes from the shadow to entitlement section into attribute operations.
     *  Collects a single value.
     *  NOTE: only collects  SUBJECT_TO_ENTITLEMENT entitlement direction.
     */
    private <T> void collectEntitlementToAttrDelta(
            OperationMap operationMap,
            PrismContainerValue<ShadowAssociationType> associationValue,
            ModificationType modificationType,
            ProvisioningContext subjectCtx) throws SchemaException {
        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();

        ShadowAssociationType associationBean = associationValue.asContainerable();
        QName associationName = associationBean.getName();
        schemaCheck(associationName != null, "No name in entitlement association %s", associationValue);

        ResourceAssociationDefinition associationDef =
                subjectDef.findAssociationDefinitionRequired(associationName, () -> " in " + subjectCtx);

        ResourceObjectAssociationDirectionType direction = associationDef.getDirection();
        if (direction != ResourceObjectAssociationDirectionType.SUBJECT_TO_OBJECT) {
            // Process just this one direction. The other direction means modification of another object and
            // therefore will be processed later.
            return;
        }

        QName assocAttrName = associationDef.getDefinitionBean().getAssociationAttribute();
        QName valueAttrName = associationDef.getDefinitionBean().getValueAttribute();

        schemaCheck(assocAttrName != null,
                "No association attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);
        schemaCheck(valueAttrName != null,
                "No value attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);

        ResourceAttributeDefinition<?> assocAttrDef = subjectDef.findAttributeDefinition(assocAttrName);
        if (assocAttrDef == null) {
            throw new SchemaException("Association attribute '" + assocAttrName + "'defined in entitlement association '"
                    + associationName + "' was not found in schema for " + subjectCtx);
        }

        //noinspection unchecked
        PropertyModificationOperation<T> attributeOperation = (PropertyModificationOperation<T>) operationMap.get(assocAttrName);
        if (attributeOperation == null) {
            //noinspection unchecked
            PropertyDelta<T> emptyDelta = (PropertyDelta<T>)
                    assocAttrDef.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, assocAttrName));
            attributeOperation = new PropertyModificationOperation<>(emptyDelta);
            attributeOperation.setMatchingRuleQName(associationDef.getMatchingRule());
            operationMap.put(assocAttrName, attributeOperation);
        }

        //MID-7144: Identifier container may not be resource attribute container, if its origin is serialized pending delta
        PrismContainer<?> identifiersContainer = associationValue
                .findContainer(ShadowAssociationType.F_IDENTIFIERS);
        PrismProperty<T> valueAttr = identifiersContainer.findProperty(ItemName.fromQName(valueAttrName));
        if (valueAttr == null) {
            throw new SchemaException("No value attribute " + valueAttrName + " present in entitlement association '"
                    + associationName + "' in shadow for " + subjectCtx);
        }

        if (modificationType == ModificationType.ADD) {
            attributeOperation.getPropertyDelta()
                    .addValuesToAdd(valueAttr.getClonedValues());
        } else if (modificationType == ModificationType.DELETE) {
            attributeOperation.getPropertyDelta()
                    .addValuesToDelete(valueAttr.getClonedValues());
        } else if (modificationType == ModificationType.REPLACE) {
            // TODO: check if already exists
            attributeOperation.getPropertyDelta()
                    .setValuesToReplace(valueAttr.getClonedValues());
        }
    }

    private ShadowType collectEntitlementsAsObjectOperation(
            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap,
            Collection<PrismContainerValue<ShadowAssociationType>> associationValuesInDeltaSet,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            ModificationType modificationType,
            ProvisioningContext ctx,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        for (PrismContainerValue<ShadowAssociationType> associationValue : emptyIfNull(associationValuesInDeltaSet)) {
            subjectShadowAfter = collectEntitlementAsObjectOperation(
                    roMap, associationValue, subjectShadowBefore, subjectShadowAfter, modificationType, ctx, result);
        }
        return subjectShadowAfter;
    }

    private <TV,TA> ShadowType collectEntitlementAsObjectOperation(
            Map<ResourceObjectDiscriminator, ResourceObjectOperations> roMap,
            PrismContainerValue<ShadowAssociationType> associationValue,
            ShadowType subjectShadowBefore,
            ShadowType subjectShadowAfter,
            ModificationType modificationType,
            ProvisioningContext subjectCtx,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceType resource = subjectCtx.getResource();
        ShadowAssociationType associationBean = associationValue.asContainerable();
        if (subjectCtx.getOperationContext() != null) {
            // todo this shouldn't be subjectCtx but the other one [viliam]
            subjectCtx.setAssociationShadowRef(associationBean.getShadowRef());
        }

        QName associationName = associationBean.getName();
        if (associationName == null) {
            throw new SchemaException("No name in entitlement association "+associationValue);
        }
        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();
        ResourceAssociationDefinition associationDef =
                subjectDef.findAssociationDefinitionRequired(associationName, () -> " in " + subjectCtx);

        ResourceObjectAssociationDirectionType direction = associationDef.getDirection();
        if (direction != ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
            // Process just this one direction. The other direction was processed before
            return subjectShadowAfter;
        }

        Collection<String> entitlementIntents = associationDef.getIntents();
        if (entitlementIntents == null || entitlementIntents.isEmpty()) {
            throw new SchemaException("No entitlement intent specified in association " + associationValue + " in " + resource);
        }
        ShadowKindType entitlementKind = associationDef.getKind();
        for (String entitlementIntent : entitlementIntents) {
            // TODO deduplicate the code
            ProvisioningContext entitlementCtx = subjectCtx.spawnForKindIntent(entitlementKind, entitlementIntent); // todo error handling
            ResourceObjectDefinition entitlementOcDef = entitlementCtx.getObjectDefinitionRequired();

            QName assocAttrName = associationDef.getDefinitionBean().getAssociationAttribute();
            QName valueAttrName = associationDef.getDefinitionBean().getValueAttribute();

            schemaCheck(assocAttrName != null,
                    "No association attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);
            schemaCheck(valueAttrName != null,
                    "No value attribute defined in entitlement association '%s' in %s", associationName, subjectCtx);

            //noinspection unchecked
            ResourceAttributeDefinition<TA> assocAttrDef =
                    (ResourceAttributeDefinition<TA>) entitlementOcDef.findAttributeDefinition(assocAttrName);
            if (assocAttrDef == null) {
                throw new SchemaException("Association attribute '"+assocAttrName+"'defined in entitlement association "
                        + "was not found in entitlement intent(s) '"+entitlementIntents+"' in schema for "+resource);
            }

            ResourceAttributeContainer identifiersContainer = getIdentifiersAttributeContainer(associationValue, entitlementOcDef);
            Collection<ResourceAttribute<?>> entitlementIdentifiersFromAssociation = identifiersContainer.getAttributes();

            ResourceObjectDiscriminator disc =
                    new ResourceObjectDiscriminator(entitlementOcDef.getTypeName(), entitlementIdentifiersFromAssociation);
            ResourceObjectOperations operations = roMap.get(disc);
            if (operations == null) {
                operations = new ResourceObjectOperations();
                operations.setResourceObjectContext(entitlementCtx);
                roMap.put(disc, operations);
            }

            // Which shadow would we use - shadowBefore or shadowAfter?
            //
            // If the operation is ADD or REPLACE, we use current version of the shadow (shadowAfter), because we want
            // to ensure that we add most-recent data to the subject.
            //
            // If the operation is DELETE, we have two possibilities:
            //  - if the resource provides referential integrity, the subject has already
            //    new data (because the object operation was already carried out), so we use shadowAfter
            //  - if the resource does not provide referential integrity, the subject has OLD data
            //    so we use shadowBefore
            ShadowType subjectShadow;
            if (modificationType != ModificationType.DELETE) {
                subjectShadow = subjectShadowAfter;
            } else {
                if (associationDef.requiresExplicitReferentialIntegrity()) {
                    // we must ensure the referential integrity
                    subjectShadow = subjectShadowBefore;
                } else {
                    // i.e. resource has ref integrity assured by itself
                    subjectShadow = subjectShadowAfter;
                }
            }

            ResourceAttribute<TV> valueAttr = ShadowUtil.getAttribute(subjectShadow, valueAttrName);
            if (valueAttr == null) {
                if (!ShadowUtil.isFullShadow(subjectShadow)) {
                    Collection<ResourceAttribute<?>> subjectIdentifiers = ShadowUtil.getAllIdentifiers(subjectShadow);
                    LOGGER.trace("Fetching {} ({})", subjectShadow, subjectIdentifiers);
                    subjectShadow = asObjectable(
                            resourceObjectReferenceResolver.fetchResourceObject(
                                    subjectCtx, subjectIdentifiers, null, subjectShadow.asPrismObject(), result));
                    subjectShadowAfter = subjectShadow;
                    valueAttr = ShadowUtil.getAttribute(subjectShadow, valueAttrName);
                }
                if (valueAttr == null) {
                    LOGGER.error("No value attribute {} in shadow\n{}", valueAttrName, subjectShadow.debugDump());
                    // TODO: check schema and try to fetch full shadow if necessary
                    throw new SchemaException("No value attribute " + valueAttrName + " in " + subjectShadow);
                }
            }

            PropertyDelta<TA> attributeDelta = null;
            for(Operation operation: operations.getOperations()) {
                if (operation instanceof PropertyModificationOperation) {
                    PropertyModificationOperation<?> propOp = (PropertyModificationOperation<?>) operation;
                    if (propOp.getPropertyDelta().getElementName().equals(assocAttrName)) {
                        //noinspection unchecked
                        attributeDelta = (PropertyDelta<TA>) propOp.getPropertyDelta();
                    }
                }
            }
            if (attributeDelta == null) {
                attributeDelta = assocAttrDef.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, assocAttrName));
            }

            PrismProperty<TA> changedAssocAttr = PrismUtil.convertProperty(valueAttr, assocAttrDef);

            if (modificationType == ModificationType.ADD) {
                attributeDelta.addValuesToAdd(changedAssocAttr.getClonedValues());
            } else if (modificationType == ModificationType.DELETE) {
                attributeDelta.addValuesToDelete(changedAssocAttr.getClonedValues());
            } else if (modificationType == ModificationType.REPLACE) {
                // TODO: check if already exists
                attributeDelta.setValuesToReplace(changedAssocAttr.getClonedValues());
            }

            var minorSubresult = result.createMinorSubresult("modifyEntitlement");
            try {
                if (ResourceTypeUtil.isAvoidDuplicateValues(resource)) {
                    ShadowType currentObjectShadow = operations.getCurrentShadow();
                    if (currentObjectShadow == null) {
                        LOGGER.trace("Fetching entitlement shadow {} to avoid value duplication (intent={})",
                                entitlementIdentifiersFromAssociation, entitlementIntent);
                        currentObjectShadow = asObjectable(
                                resourceObjectReferenceResolver.fetchResourceObject(
                                        entitlementCtx,
                                        entitlementIdentifiersFromAssociation,
                                        null,
                                        null,
                                        minorSubresult));
                        operations.setCurrentShadow(currentObjectShadow);
                    }
                    // TODO It seems that duplicate values are checked twice: once here and the second time
                    //  in ResourceObjectConverter.executeModify. Check that and fix if necessary.
                    PropertyDelta<TA> attributeDeltaAfterNarrow = ProvisioningUtil.narrowPropertyDelta(
                            attributeDelta, currentObjectShadow, associationDef.getMatchingRule(), matchingRuleRegistry);
                    if (attributeDeltaAfterNarrow == null || attributeDeltaAfterNarrow.isEmpty()) {
                        LOGGER.trace("Not collecting entitlement object operations ({}) association {}: "
                                        + "attribute delta is empty after narrow, orig delta: {}",
                                modificationType, associationName.getLocalPart(), attributeDelta);
                    }
                    attributeDelta = attributeDeltaAfterNarrow;
                }

                if (attributeDelta != null && !attributeDelta.isEmpty()) {
                    PropertyModificationOperation<?> attributeModification = new PropertyModificationOperation<>(attributeDelta);
                    attributeModification.setMatchingRuleQName(associationDef.getMatchingRule());
                    LOGGER.trace("Collecting entitlement object operations ({}) association {}: {}",
                            modificationType, associationName.getLocalPart(), attributeModification);
                    operations.add(attributeModification);
                }

                result.recordSuccess();
            } catch (ObjectNotFoundException | CommunicationException | SchemaException | SecurityViolationException |
                     ConfigurationException | ExpressionEvaluationException e) {
                // We need to handle this specially.
                // E.g. ObjectNotFoundException means that the entitlement object was not found,
                // not that the subject was not found. It we throw ObjectNotFoundException here it may be
                // interpreted by the consistency code to mean that the subject is missing. Which is not
                // true. And that may cause really strange reactions. In fact we do not want to throw the
                // exception at all, because the primary operation was obviously successful. So just
                // properly record the operation in the result.
                LOGGER.error("Error while modifying entitlement {}: {}", entitlementCtx, e.getMessage(), e);
                minorSubresult.recordException(e);
                roMap.remove(disc);

                return subjectShadowBefore;
            } catch (RuntimeException | Error e) {
                LOGGER.error("Error while modifying entitlement {}: {}", entitlementCtx, e.getMessage(), e);
                minorSubresult.recordException(e);
                throw e;
            } finally {
                minorSubresult.close();
            }
        }
        return subjectShadowAfter;
    }

    private @NotNull ResourceAttributeContainer getIdentifiersAttributeContainer(
            PrismContainerValue<ShadowAssociationType> associationCVal, ResourceObjectDefinition entitlementDef)
            throws SchemaException {
        PrismContainer<?> container = associationCVal.findContainer(ShadowAssociationType.F_IDENTIFIERS);
        if (container == null) {
            throw new SchemaException("No identifiers in association value: " + associationCVal);
        }
        if (container instanceof ResourceAttributeContainer) {
            return (ResourceAttributeContainer) container;
        }
        ResourceAttributeContainer attributesContainer =
                entitlementDef.toResourceAttributeContainerDefinition()
                        .instantiate(ShadowAssociationType.F_IDENTIFIERS);
        PrismContainerValue<?> cval = container.getValue();
        for (Item<?, ?> item : cval.getItems()) {
            //noinspection unchecked
            ResourceAttribute<Object> attribute =
                    ((ResourceAttributeDefinition<Object>)
                            entitlementDef.findAttributeDefinitionRequired(item.getElementName()))
                            .instantiate();
            for (Object val : item.getRealValues()) {
                attribute.addRealValue(val);
            }
            attributesContainer.add(attribute);
        }
        return attributesContainer;
    }

    //endregion

    /**
     * Keeps modification operations indexed by attribute names.
     */
    private static class OperationMap {
        private final Map<QName, PropertyModificationOperation<?>> operationMap = new HashMap<>();

        public Collection<? extends PropertyModificationOperation<?>> getOperations() {
            return operationMap.values();
        }

        public PropertyModificationOperation<?> get(QName attributeName) {
            return operationMap.get(attributeName);
        }

        public void put(QName attributeName, PropertyModificationOperation<?> operation) {
            operationMap.put(attributeName, operation);
        }
    }
}
