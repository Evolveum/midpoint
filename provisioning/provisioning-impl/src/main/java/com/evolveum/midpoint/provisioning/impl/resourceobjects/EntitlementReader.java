/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import static com.evolveum.midpoint.provisioning.impl.resourceobjects.EntitlementUtils.createEntitlementQuery;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

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
    @NotNull private final ShadowAssociationsContainer associationsContainer;

    @NotNull private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    private EntitlementReader(@NotNull ResourceObject subject, @NotNull ProvisioningContext subjectCtx) {
        this.subject = subject;
        this.subjectCtx = subjectCtx;
        this.associationsContainer = subjectCtx.getObjectDefinitionRequired()
                .toShadowAssociationsContainerDefinition()
                .instantiate();
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

        for (ShadowAssociationDefinition associationDef : subjectCtx.getAssociationDefinitions()) {
            ShadowKindType entitlementKind = associationDef.getKind();
            for (String entitlementIntent : associationDef.getIntents()) {
                LOGGER.trace("Resolving association {} for kind {} and intent {}",
                        associationDef.getName(), entitlementKind, entitlementIntent);
                ProvisioningContext entitlementCtx = subjectCtx.spawnForKindIntent(entitlementKind, entitlementIntent);
                switch (associationDef.getDirection()) {
                    case SUBJECT_TO_OBJECT ->
                            readSubjectToObject(
                                    associationDef.getAssociationAttributeName(),
                                    associationDef.getValueAttributeName(),
                                    associationDef,
                                    entitlementCtx);
                    case OBJECT_TO_SUBJECT -> {
                        QName shortcutAssociationAttribute = associationDef.getShortcutAssociationAttributeName();
                        if (shortcutAssociationAttribute != null) {
                            readSubjectToObject(
                                    shortcutAssociationAttribute,
                                    associationDef.getShortcutValueAttributeNameRequired(),
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

        if (!associationsContainer.isEmpty()) {
            subject.getPrismObject().add(associationsContainer);
        }
        LOGGER.trace("Finished entitlements read operation; the association container now has {} value(s)",
                associationsContainer.size());
    }

    /**
     * Creates a value in `associationContainer`. It simply uses values in "association" (referencing) attribute
     * to construct identifiers pointing to the entitlement object.
     *
     * @param referencingAttrName The "referencing" attribute (aka association attribute), present on subject, e.g. memberOf.
     * @param referencedAttrName The "referenced" attribute (aka value attribute) present on object, e.g. dn (Group DN).
     *
     * @see #addAssociationValueFromIdentifier(PrismPropertyValue, ResourceAttributeDefinition, QName)
     */
    private <T> void readSubjectToObject(
            @NotNull QName referencingAttrName,
            @NotNull QName referencedAttrName,
            ShadowAssociationDefinition associationDef,
            ProvisioningContext entitlementCtx) throws SchemaException {

        QName associationName = associationDef.getName();

        ResourceObjectDefinition subjectDef = subjectCtx.getObjectDefinitionRequired();
        ResourceObjectDefinition entitlementDef = entitlementCtx.getObjectDefinitionRequired();

        ResourceAttributeContainer attributesContainer = subject.getAttributesContainer();

        subjectDef.findAttributeDefinitionRequired(
                referencingAttrName,
                () -> " in association '" + associationName + "' in " + subjectCtx + " [association attribute]");

        ResourceAttributeDefinition<T> referencedAttrDef =
                entitlementDef.findAttributeDefinitionRequired(
                        referencedAttrName,
                        () -> " in association '" + associationName + "' in " + subjectCtx + " [value attribute]");

        ResourceAttribute<T> referencingAttr = attributesContainer.findAttribute(referencingAttrName);
        if (referencingAttr != null && !referencingAttr.isEmpty()) {
            for (PrismPropertyValue<T> referencingAttrValue : referencingAttr.getValues()) {
                // NOTE: Those values are not filtered according to kind/intent of the association target.
                // Therefore there may be values that do not belong here (see MID-5790). But that is OK for now.
                // We will filter those values out later when read the shadows and determine shadow OID.
                addAssociationValueFromIdentifier(
                        referencingAttrValue, referencedAttrDef, associationDef.getName());
            }
        } else {
            // Nothing to do. No attribute to base the association on.
            LOGGER.trace("Association attribute {} is empty, skipping association {}", referencingAttrName, associationName);
        }
    }

    /**
     * Creates values in {@link #associationsContainer}. It searches for entitlements having the "association"
     * (referencing) attribute value - e.g. `ri:members` - containing the value in subject
     * "value" (referenced) attribute - e.g. `ri:dn`.
     */
    private <T> void readObjectToSubject(
            ShadowAssociationDefinition associationDef,
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

        QName referencingAttrName = associationDef.getAssociationAttributeName(); // e.g. ri:members
        QName referencedAttrName = associationDef.getValueAttributeName(); // e.g. ri:dn

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

        ResourceObjectHandler handler = (objectFound, lResult) -> {
            objectFound.initialize(entitlementCtx.getTask(), lResult);
            if (objectFound.isOk()) {
                ExistingResourceObject resourceObject = objectFound.getResourceObject();
                try {
                    addAssociationValueFromEntitlementObject(associationName, resourceObject);
                } catch (SchemaException e) {
                    throw new LocalTunnelException(e);
                }

                LOGGER.trace("Processed entitlement-to-subject association for subject {} and entitlement (object) {}",
                        subject.getHumanReadableNameLazily(),
                        resourceObject.getHumanReadableNameLazily());
            } else {
                // TODO better handling
                throw new SystemException(
                        "Couldn't process entitlement: " + objectFound + ": " + objectFound.getExceptionEncountered());
            }

            return true;
        };
        LOGGER.trace("Processing object-to-subject association for account {}: query {}",
                ShadowUtil.getHumanReadableNameLazily(subject.getPrismObject()), explicitQuery);
        try {
            b.resourceObjectConverter.searchResourceObjects(
                    entitlementCtx, handler, explicitQuery, false, FetchErrorReportingMethodType.EXCEPTION, result);
        } catch (LocalTunnelException e) {
            throw e.schemaException;
        }
    }

    /**
     * Creates association value from known identifier value; and adds it into the respective association
     * in `associationContainer`.
     *
     *     PCV:
     *       identifiers: { valueAttr: value }
     *
     * For example, if
     *
     * - identifier value = "cn=wheel,ou=Groups,dc=example,dc=com"
     * - referenced identifier is "dn" (of a group)
     * - association name = "ri:group"
     *
     * then the result would be:
     *
     *     ri:group:
     *       PCV: identifiers: { dn: "cn=wheel,ou=Groups,dc=example,dc=com" }
     */
    private <T> void addAssociationValueFromIdentifier(
            PrismPropertyValue<T> identifierValue,
            ResourceAttributeDefinition<T> referencedIdentifierDef,
            QName associationName) throws SchemaException {
        var associationValue =
                associationsContainer
                        .findOrCreateAssociation(associationName)
                        .createNewValueWithIdentifier(
                                referencedIdentifierDef.instantiateFromValue(identifierValue.clone()));
        LOGGER.trace("Association attribute value resolved to association value {}", associationValue);
    }

    /**
     * Creates association value from resolved target object; and adds it into the respective association
     * in `associationContainer`.
     *
     * For example, if
     *
     * - target object is the group of = "cn=wheel,ou=Groups,dc=example,dc=com"
     * - association name = "ri:groups"
     *
     * then the result would be:
     *
     *     ri:group:
     *       PCV: identifiers: { dn: "cn=wheel,ou=Groups,dc=example,dc=com" }
     */
    private void addAssociationValueFromEntitlementObject(
            @NotNull QName associationName, @NotNull ExistingResourceObject entitlementObject) throws SchemaException {
        ResourceAttributeContainer identifiersContainer = entitlementObject.getIdentifiersAsContainer();
        // Remember the full shadow. This is used later as an optimization to create the shadow in repo
        identifiersContainer.setUserData(ResourceObjectConverter.ENTITLEMENT_OBJECT_KEY, entitlementObject);
        associationsContainer
                .findOrCreateAssociation(associationName)
                .createNewValueWithIdentifiers(identifiersContainer);
    }

    static class LocalTunnelException extends RuntimeException {
        SchemaException schemaException;
        LocalTunnelException(SchemaException cause) {
            super(cause);
            this.schemaException = cause;
        }
    }
}
