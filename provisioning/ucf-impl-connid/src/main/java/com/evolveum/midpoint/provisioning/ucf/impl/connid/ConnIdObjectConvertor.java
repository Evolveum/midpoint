/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.ucfAttributeNameToConnId;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.argNonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;

import org.identityconnectors.framework.common.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ucf.api.UcfFetchErrorReportingMethod;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ActivationUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Converts from ConnId connector objects to UCF resource objects (by delegating
 * to {@link ConnIdToUcfObjectConversion}) and back from UCF to ConnId (by itself).
 *
 * @author semancik
 */
class ConnIdObjectConvertor {

    private static final Trace LOGGER = TraceManager.getTrace(ConnIdObjectConvertor.class);

    private final ConnIdBeans b = ConnIdBeans.get();

    @NotNull private final ConnectorContext connectorContext;

    ConnIdObjectConvertor(@NotNull ConnectorContext connectorContext) {
        this.connectorContext = connectorContext;
    }

    /**
     * Converts ICF ConnectorObject to the midPoint ResourceObject.
     *
     * All the attributes are mapped using the same way as they are mapped in
     * the schema (which is actually no mapping at all now).
     *
     * If an optional ResourceObjectDefinition was provided, the resulting
     * ResourceObject is schema-aware (getDefinition() method works). If no
     * ResourceObjectDefinition was provided, the object is schema-less. TODO:
     * this still needs to be implemented.
     *
     * @param co ICF ConnectorObject to convert
     * @param ucfErrorReportingMethod If EXCEPTIONS (the default), any exceptions are thrown as such. But if FETCH_RESULT,
     * exceptions are represented in fetchResult property of the returned resource object.
     * Generally, when called as part of "searchObjectsIterative" in the context of
     * a task, we might want to use the latter case to give the task handler a chance to report
     * errors to the user (and continue processing of the correct objects).
     * @return new mapped ResourceObject instance.
     */
    @NotNull UcfResourceObject convertToUcfObject(
            @NotNull ConnectorObject co,
            @NotNull ResourceObjectDefinition objectDefinition,
            UcfFetchErrorReportingMethod ucfErrorReportingMethod,
            @NotNull ConnectorOperationContext operationContext,
            OperationResult parentResult) throws SchemaException {

        // This is because of suspicion that this operation sometimes takes a long time.
        // If it will not be the case, we can safely remove subresult construction here.
        OperationResult result = parentResult.subresult(ConnIdObjectConvertor.class.getName() + ".convertToUcfObject")
                .setMinor()
                .addArbitraryObjectAsParam("uid", co.getUid())
                .addArbitraryObjectAsParam("objectDefinition", objectDefinition)
                .addArbitraryObjectAsParam("ucfErrorReportingMethod", ucfErrorReportingMethod)
                .build();
        try {

            var conversion = new ConnIdToUcfObjectConversion(
                    co, objectDefinition, operationContext.connectorContext(), operationContext.getResourceSchemaRequired());
            try {
                conversion.execute();
                return conversion.getUcfResourceObjectIfSuccess();
            } catch (Throwable t) {
                if (ucfErrorReportingMethod == UcfFetchErrorReportingMethod.UCF_OBJECT) {
                    Throwable wrappedException = MiscUtil.createSame(t, createMessage(co, t));
                    result.recordException(wrappedException);
                    return conversion.getPartiallyConvertedUcfResourceObject(wrappedException);
                } else {
                    throw t; // handled just below
                }
            }

        } catch (Throwable t) {
            // We have no resource object to return (e.g. because it couldn't be instantiated). So really the only option
            // is to throw an exception.
            String message = createMessage(co, t);
            result.recordFatalError(message, t);
            MiscUtil.throwAsSame(t, message);
            throw t; // just to make compiler happy
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    @NotNull
    private String createMessage(@NotNull ConnectorObject co, Throwable t) {
        return "Couldn't convert resource object from ConnID to midPoint: uid=" + co.getUid() + ", name="
                + co.getName() + ", class=" + co.getObjectClass() + ": " + t.getMessage();
    }

    /**
     * Converts from UCF to ConnId.
     *
     * The `identifiersOnly` parameter is used to indicate that only identifiers should be converted. It is used when converting
     * referenced objects (like groups that the account is member of): in that case we need only the identifiers. Converting
     * other attributes is useless, and converting reference attributes leads to a failure, as they are usually not resolved.
     * (MID-10271)
     */
    @NotNull ConnIdObjectInformation convertToConnIdObjectInfo(@NotNull ShadowType shadow, boolean identifiersOnly)
            throws SchemaException {

        var objDef = ShadowUtil.getResourceObjectDefinition(shadow);

        var icfObjectClass =
                argNonNull(
                        ucfObjectClassNameToConnId(shadow, connectorContext.isLegacySchema()),
                        "Couldn't get icf object class from %s", shadow);

        Set<Attribute> attributes = new HashSet<>();
        try {

            LOGGER.trace("midPoint object before conversion:\n{}", shadow.debugDumpLazily());

            for (var simpleAttribute : ShadowUtil.getSimpleAttributes(shadow)) {
                if (!identifiersOnly || objDef.isIdentifier(simpleAttribute.getElementName())) {
                    attributes.add(convertSimpleAttributeToConnId(simpleAttribute, objDef));
                }
            }

            if (!identifiersOnly) {

                for (var referenceAttribute : ShadowUtil.getReferenceAttributes(shadow)) {
                    if (!referenceAttribute.getDefinitionRequired().isSimulated()) {
                        attributes.add(convertReferenceAttributeToConnId(referenceAttribute, objDef));
                    }
                }

                var passwordValue = ShadowUtil.getPasswordValue(shadow);
                if (passwordValue != null) {
                    var guardedPassword = ConnIdUtil.toGuardedString(passwordValue, "new password", b.protector);
                    if (guardedPassword != null) {
                        attributes.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, guardedPassword));
                    }
                }

                if (ActivationUtil.hasAdministrativeActivation(shadow)) {
                    attributes.add(
                            AttributeBuilder.build(
                                    OperationalAttributes.ENABLE_NAME, ActivationUtil.isAdministrativeEnabled(shadow)));
                }

                var validFrom = ActivationUtil.getValidFrom(shadow);
                if (validFrom != null) {
                    attributes.add(
                            AttributeBuilder.build(OperationalAttributes.ENABLE_DATE_NAME, XmlTypeConverter.toMillis(validFrom)));
                }

                var validTo = ActivationUtil.getValidTo(shadow);
                if (validTo != null) {
                    attributes.add(
                            AttributeBuilder.build(OperationalAttributes.DISABLE_DATE_NAME, XmlTypeConverter.toMillis(validTo)));
                }

                var lockoutStatus = ActivationUtil.getLockoutStatus(shadow);
                if (lockoutStatus != null) {
                    attributes.add(
                            AttributeBuilder.build(
                                    OperationalAttributes.LOCK_OUT_NAME, ActivationUtil.isLockedOut(lockoutStatus)));
                }

                var lastLoginDate = ShadowUtil.getLastLoginTimestampValue(shadow);
                if (lastLoginDate != null) {
                    attributes.add(
                            AttributeBuilder.build(
                                    PredefinedAttributes.LAST_LOGIN_DATE_NAME, XmlTypeConverter.toMillis(lastLoginDate)));
                }
            }

            LOGGER.trace("ConnId attributes after conversion:\n{}", lazy(() -> ConnIdUtil.dump(attributes)));

        } catch (SchemaException | RuntimeException ex) {
            throw new SchemaException(
                    "Error while converting shadow attributes for a %s. Reason: %s".formatted(
                            icfObjectClass.getObjectClassValue(), ex.getMessage()),
                    ex);
        }

        List<String> icfAuxiliaryObjectClasses = new ArrayList<>();
        for (QName auxiliaryObjectClassName : shadow.getAuxiliaryObjectClass()) {
            icfAuxiliaryObjectClasses.add(
                    ConnIdNameMapper.ucfObjectClassNameToConnId(auxiliaryObjectClassName, false)
                            .getObjectClassValue());
        }
        if (!icfAuxiliaryObjectClasses.isEmpty()) {
            attributes.add(new AttributeBuilder()
                    .setName(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME)
                    .addValue(icfAuxiliaryObjectClasses)
                    .build());
        }

        return new ConnIdObjectInformation(icfObjectClass, attributes, icfAuxiliaryObjectClasses);
    }

    private Attribute convertSimpleAttributeToConnId(ShadowSimpleAttribute<?> mpAttribute, ResourceObjectDefinition ocDef)
            throws SchemaException {
        String connIdAttrName = ucfAttributeNameToConnId(mpAttribute, ocDef);

        Set<Object> connIdAttributeValues = new HashSet<>();
        for (var mpAttrValue : mpAttribute.getValues()) {
            connIdAttributeValues.add(
                    ConnIdUtil.convertValueToConnId(mpAttrValue, b.protector, mpAttribute.getElementName()));
        }

        try {
            return AttributeBuilder.build(connIdAttrName, connIdAttributeValues);
        } catch (IllegalArgumentException e) {
            throw new SchemaException(e.getMessage(), e);
        }
    }

    private Attribute convertReferenceAttributeToConnId(ShadowReferenceAttribute mpAttribute, ResourceObjectDefinition ocDef)
            throws SchemaException {
        String connIdAttrName = ucfAttributeNameToConnId(mpAttribute, ocDef);

        Set<ConnectorObjectReference> connIdAttrValues = new HashSet<>();
        for (var mpRefAttrValue : mpAttribute.getReferenceValues()) {
            connIdAttrValues.add(
                    convertReferenceAttributeValueToConnId(mpRefAttrValue));
        }

        try {
            return AttributeBuilder.build(connIdAttrName, connIdAttrValues);
        } catch (IllegalArgumentException e) {
            throw new SchemaException(e.getMessage(), e);
        }
    }

    @NotNull ConnectorObjectReference convertReferenceAttributeValueToConnId(ShadowReferenceAttributeValue mpRefAttrValue)
            throws SchemaException {
        var embeddedShadow = mpRefAttrValue.getShadowBean();

        // Embedded objects are converted in full. Standalone ones are converted in "identifiers only" mode, because
        // they are not to be created by ConnId - they are going to be referenced only.
        var identifiersOnly =
                !ShadowUtil.getResourceObjectDefinition(embeddedShadow)
                        .getNativeObjectClassDefinition()
                        .isEmbedded();
        var connIdInfo = convertToConnIdObjectInfo(embeddedShadow, identifiersOnly);

        // TODO this object should be "by value" (ConnectorObject) for associated objects,
        //  and "by reference" (ConnectorObjectIdentification) for regular objects.
        //  Unfortunately, we cannot instantiate ConnectorObject here if UID is missing; and this
        //  is a common case when creating new objects. Hence, we use ConnectorObjectIdentification
        //  for both cases.
        var referencedObject = new ConnectorObjectIdentification(connIdInfo.objectClass, connIdInfo.attributes);
        return new ConnectorObjectReference(referencedObject);
    }

    /** Quite ugly method - we should have a single place from where to take the object class. TODO resolve */
    private @Nullable ObjectClass ucfObjectClassNameToConnId(ShadowType shadow, boolean legacySchema) {

        QName objectClassName = shadow.getObjectClass();
        if (objectClassName == null) {
            ShadowAttributesContainer attrContainer = ShadowUtil.getAttributesContainer(shadow);
            if (attrContainer == null) {
                return null;
            }
            objectClassName = attrContainer.getDefinition().getTypeName();
        }

        return ConnIdNameMapper.ucfObjectClassNameToConnId(objectClassName, legacySchema);
    }

    record ConnIdObjectInformation(
            @NotNull ObjectClass objectClass,
            @NotNull Set<Attribute> attributes,
            @NotNull List<String> auxiliaryObjectClasses) {
    }
}
