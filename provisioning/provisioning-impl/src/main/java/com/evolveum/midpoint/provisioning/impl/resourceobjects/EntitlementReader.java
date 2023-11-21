/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ReadCapabilityType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementUtils.createEntitlementQuery;
import static com.evolveum.midpoint.util.MiscUtil.schemaCheck;

/**
 * Reads the entitlements of a subject (resource object).
 */
class EntitlementReader {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementReader.class);

    /** The resource object for which we are trying to obtain the entitlements. */
    @NotNull private final ResourceObject subject;

    /** {@link ProvisioningContext} of the subject fetch/search/whatever operation. */
    @NotNull private final ProvisioningContext subjectCtx;

    /** The association container being created and filled-in. */
    @NotNull private final PrismContainer<ShadowAssociationType> associationContainer;

    @NotNull private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    private EntitlementReader(@NotNull ResourceObject subject, @NotNull ProvisioningContext subjectCtx) {
        this.subject = subject;
        this.subjectCtx = subjectCtx;
        try {
            this.associationContainer = subject.getPrismObject().getDefinition()
                    .<ShadowAssociationType>findContainerDefinition(ShadowType.F_ASSOCIATION)
                    .instantiate();
        } catch (SchemaException e) {
            throw SystemException.unexpected(e);
        }
    }

    /**
     * Fills-in the subject's associations container (`association`) based on the relevant resource object attribute values.
     * Note that for "object to subject" entitlements this involves a search operation.
     */
    public static void read(
            @NotNull ResourceObject subject,
            @NotNull ProvisioningContext subjectCtx,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new EntitlementReader(subject, subjectCtx)
                .doRead(result);
    }

    private void doRead(OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        LOGGER.trace("Starting entitlements read operation");

        for (ResourceAssociationDefinition associationDef : subjectCtx.getAssociationDefinitions()) {
            ShadowKindType entitlementKind = associationDef.getKind();
            for (String entitlementIntent : associationDef.getIntents()) {
                LOGGER.trace("Resolving association {} for kind {} and intent {}",
                        associationDef.getName(), entitlementKind, entitlementIntent);
                ProvisioningContext entitlementCtx = subjectCtx.spawnForKindIntent(entitlementKind, entitlementIntent);
                ResourceObjectAssociationType associationDefBean = associationDef.getDefinitionBean();
                switch (associationDef.getDirection()) {
                    case SUBJECT_TO_OBJECT ->
                            readSubjectToObject(
                                    associationDefBean.getAssociationAttribute(),
                                    associationDefBean.getValueAttribute(),
                                    associationDef,
                                    entitlementCtx);
                    case OBJECT_TO_SUBJECT -> {
                        QName shortcutAssociationAttribute = associationDefBean.getShortcutAssociationAttribute();
                        if (shortcutAssociationAttribute != null) {
                            readSubjectToObject(
                                    shortcutAssociationAttribute,
                                    associationDefBean.getShortcutValueAttribute(),
                                    associationDef,
                                    entitlementCtx);
                        } else {
                            readObjectToSubject(associationDef, entitlementCtx, result);
                        }
                    }
                    default ->
                            throw new AssertionError(
                                    "Unknown entitlement direction %s in association %s in %s".formatted(
                                            associationDef.getDirection(), associationDef, subjectCtx));
                }
            }
        }

        if (!associationContainer.isEmpty()) {
            subject.getPrismObject().add(associationContainer);
        }
        LOGGER.trace("Finished entitlements read operation; the association container now has {} value(s)",
                associationContainer.size());
    }

    /**
     * Creates a value in `associationContainer`. It simply uses values in "association" (referencing) attribute
     * to construct identifiers pointing to the entitlement object.
     *
     * @param referencingAttrName The "referencing" attribute (aka association attribute), present on subject, e.g. memberOf.
     * @param referencedAttrName The "referenced" attribute (aka value attribute) present on object, e.g. dn (Group DN).
     *
     * @see #createAssociationValueFromIdentifier(PrismPropertyValue, ResourceAttributeDefinition, QName, ResourceObjectDefinition)
     */
    private <T> void readSubjectToObject(
            QName referencingAttrName,
            QName referencedAttrName,
            ResourceAssociationDefinition associationDef,
            ProvisioningContext entitlementCtx) throws SchemaException {

        QName associationName = associationDef.getName();

        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();
        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        ResourceAttributeContainer attributesContainer = subject.getAttributesContainer();

        schemaCheck(referencingAttrName != null,
                "No association attribute defined in association '%s' in %s", associationName, subjectCtx);
        schemaCheck(referencedAttrName != null,
                "No value attribute defined in association '%s' in %s", associationName, subjectCtx);

        subjectDef.findAttributeDefinitionRequired(
                referencingAttrName,
                () -> " in association '" + associationName + "' in " + subjectCtx + " [association attribute]");

        //noinspection unchecked
        ResourceAttributeDefinition<T> referencedAttrDef =
                (ResourceAttributeDefinition<T>)
                        entitlementDef.findAttributeDefinitionRequired(
                                referencedAttrName,
                                () -> " in association '" + associationName + "' in " + subjectCtx + " [value attribute]");

        ResourceAttribute<T> referencingAttr = attributesContainer.findAttribute(referencingAttrName);
        if (referencingAttr != null && !referencingAttr.isEmpty()) {
            for (PrismPropertyValue<T> referencingAttrValue : referencingAttr.getValues()) {
                PrismContainerValue<ShadowAssociationType> associationContainerValue =
                        createAssociationValueFromIdentifier(
                                referencingAttrValue, referencedAttrDef, associationName, entitlementDef);

                // NOTE: Those values are not filtered according to kind/intent of the association target.
                // Therefore there may be values that do not belong here (see MID-5790). But that is OK for now.
                // We will filter those values out later when read the shadows and determine shadow OID.
                LOGGER.trace("Association attribute value resolved to association container value {}", associationContainerValue);
            }
        } else {
            // Nothing to do. No attribute to base the association on.
            LOGGER.trace("Association attribute {} is empty, skipping association {}", referencingAttrName, associationName);
        }
    }

    /**
     * Creates values in {@link #associationContainer}. It searches for entitlements having the "association"
     * (referencing) attribute value - e.g. `ri:members` - containing the value in subject
     * "value" (referenced) attribute - e.g. `ri:dn`.
     */
    private <T> void readObjectToSubject(
            ResourceAssociationDefinition associationDef,
            ProvisioningContext entitlementCtx,
            OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        QName associationName = associationDef.getName();

        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();
        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        ResourceAttributeContainer attributesContainer = subject.getAttributesContainer();

        QName associationAuxiliaryObjectClass = associationDef.getAuxiliaryObjectClass();
        if (associationAuxiliaryObjectClass != null && !subjectDef.hasAuxiliaryObjectClass(associationAuxiliaryObjectClass)) {
            LOGGER.trace("Ignoring association {} because subject does not have auxiliary object class {}, it has {}",
                    associationName, associationAuxiliaryObjectClass, subjectDef.getAuxiliaryDefinitions());
            return;
        }

        QName referencingAttrName = associationDef.getDefinitionBean().getAssociationAttribute(); // e.g. ri:members
        QName referencedAttrName = associationDef.getDefinitionBean().getValueAttribute(); // e.g. ri:dn

        schemaCheck(referencingAttrName != null,
                "No association attribute defined in association '%s' in %s", associationName, subjectCtx);
        schemaCheck(referencedAttrName != null,
                "No value attribute defined in association '%s' in %s", associationName, subjectCtx);

        ResourceAttributeDefinition<?> referencingAttrDef = entitlementDef.findAttributeDefinitionRequired(
                referencingAttrName,
                () -> " in association '" + associationName + "' in " + entitlementCtx + " [association attribute]");

        ResourceAttribute<T> referencedAttr = attributesContainer.findAttribute(referencedAttrName);
        if (referencedAttr == null || referencedAttr.isEmpty()) {
            LOGGER.trace("Ignoring association {} because subject does not have any value in attribute {}",
                    associationName, referencedAttrName);
            return;
        }
        if (referencedAttr.size() > 1) {
            throw new SchemaException(
                    "Referenced value attribute %s has more than one value; it is the attribute defined in association '%s' in %s"
                            .formatted(referencedAttrName, associationName, subjectCtx));
        }
        PrismPropertyValue<T> referencedAttrValue = referencedAttr.getValue();
        ObjectQuery query = createEntitlementQuery(referencedAttrValue, referencingAttrDef);

        executeSearchForEntitlements(query, associationName, entitlementCtx, result);
    }

    /**
     * Executes the search for entitlements using the prepared query.
     */
    private void executeSearchForEntitlements(
            ObjectQuery explicitQuery,
            QName associationName,
            ProvisioningContext entitlementCtx,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {

        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        QueryWithConstraints queryWithConstraints =
                b.delineationProcessor.determineQueryWithConstraints(entitlementCtx, explicitQuery, result);

        UcfObjectHandler handler = (ucfObject, lResult) -> {
            try {
                createAssociationValueFromEntitlementObject(ucfObject.getResourceObject(), associationName, entitlementDef);
            } catch (SchemaException e) {
                throw new TunnelException(e);
            }

            LOGGER.trace("Processed entitlement-to-subject association for account {} and entitlement {}",
                    ShadowUtil.getHumanReadableNameLazily(subject.getPrismObject()),
                    ShadowUtil.getHumanReadableNameLazily(ucfObject.getPrismObject()));

            return true;
        };

        ConnectorInstance connector = subjectCtx.getConnector(ReadCapabilityType.class, result);
        try {
            LOGGER.trace("Processing object-to-subject association for account {}: query {}",
                    ShadowUtil.getHumanReadableNameLazily(subject.getPrismObject()), queryWithConstraints.query);
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
    private void createAssociationValueFromEntitlementObject(
            UcfResourceObject entitlementObject,
            QName associationName,
            ResourceObjectDefinition entitlementDef) throws SchemaException {

        PrismContainerValue<ShadowAssociationType> associationContainerValue = associationContainer.createNewValue();
        associationContainerValue.asContainerable().setName(associationName);
        associationContainerValue.add(
                createIdentifiersContainerForTargetObject(entitlementObject, entitlementDef));
    }

    /**
     * Creates the identifiers container from given value of an identifier.
     */
    private <T> @NotNull ResourceAttributeContainer createIdentifiersContainerForIdentifierValue(
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
    private @NotNull ResourceAttributeContainer createIdentifiersContainerForTargetObject(
            UcfResourceObject entitlementObject,
            ResourceObjectDefinition entitlementDef) throws SchemaException {

        ResourceAttributeContainer identifiersContainer = ObjectFactory.createResourceAttributeContainer(
                ShadowAssociationType.F_IDENTIFIERS, entitlementDef.toResourceAttributeContainerDefinition());
        identifiersContainer.getValue().addAll(
                Item.cloneCollection(entitlementObject.getAllIdentifiers()));

        // Remember the full shadow. This is used later as an optimization to create the shadow in repo
        identifiersContainer.setUserData(ResourceObjectConverter.ENTITLEMENT_OBJECT_KEY, entitlementObject);
        return identifiersContainer;
    }
}
