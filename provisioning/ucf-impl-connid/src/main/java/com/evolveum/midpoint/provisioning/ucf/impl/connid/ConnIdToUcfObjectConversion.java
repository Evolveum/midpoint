/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.connIdAttributeNameToUcf;
import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.connIdObjectClassNameToUcf;
import static com.evolveum.midpoint.util.DebugUtil.lazy;
import static com.evolveum.midpoint.util.MiscUtil.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObjectFragment;
import com.evolveum.midpoint.util.MiscUtil;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Conversion of a single
 *
 * - ConnId ({@link ConnectorObject})
 * - or ConnId ({@link ConnectorObjectIdentification})
 *
 * to
 *
 * - UCF {@link UcfResourceObject}
 * - or UCF {@link UcfResourceObjectIdentification}
 *
 * Calls itself recursively: when converting an object, its references (with embedded objects or references)
 * are converted using the very same mechanism as the containing object.
 * See {@link Conversion#convertReferenceToReferenceAttributeValue(ConnectorObjectReference)}.
 */
class ConnIdToUcfObjectConversion {

    private static final Trace LOGGER = TraceManager.getTrace(ConnIdToUcfObjectConversion.class);

    /** The input (the object or object identification that came from ConnId) */
    @NotNull private final BaseObject connectorObjectFragment;

    /** The definition provided by the caller. May be augmented (via auxiliary object classes) by the conversion process. */
    @NotNull private final ResourceObjectDefinition originalResourceObjectDefinition;

    @NotNull private final CompleteResourceSchema resourceSchema;

    @NotNull private final ConnectorContext connectorContext;

    @NotNull private final ItemFactory itemFactory = PrismContext.get().itemFactory();

    @NotNull private final ConnIdBeans b = ConnIdBeans.get();

    /** The actual conversion. For the reason of separate existence, see {@link Conversion}. */
    private Conversion conversion;

    ConnIdToUcfObjectConversion(
            @NotNull BaseObject connectorObject,
            @NotNull ResourceObjectDefinition originalResourceObjectDefinition,
            @NotNull ConnectorContext connectorContext,
            @NotNull CompleteResourceSchema resourceSchema) {
        this.connectorObjectFragment = connectorObject;
        this.originalResourceObjectDefinition = originalResourceObjectDefinition;
        this.connectorContext = connectorContext;
        this.resourceSchema = resourceSchema;
    }

    void execute() throws SchemaException {
        conversion = new Conversion();
        conversion.convertAttributes();
    }

    /**
     * In order to report errors both in "exception" and "ucf_object" mode, this method throws an exception if the conversion
     * was not entirely successful. The client can then call {@link #getPartiallyConvertedUcfResourceObject(Throwable)}
     * to get the partial result, with customized error state that may include client-supplied context.
     */
    @NotNull UcfResourceObject getUcfResourceObjectIfSuccess() throws SchemaException {
        return (UcfResourceObject) conversion.getResourceObjectFragmentIfSuccess();
    }

    /** As {@link #getUcfResourceObjectIfSuccess()} but for fragments, i.e., reference values. */
    private @NotNull UcfResourceObjectFragment getUcfResourceObjectFragmentIfSuccess() throws SchemaException {
        return conversion.getResourceObjectFragmentIfSuccess();
    }

    @NotNull UcfResourceObject getPartiallyConvertedUcfResourceObject(@NotNull Throwable exception) throws SchemaException {
        String uidValue = getUidValue();
        var errorState = UcfErrorState.error(exception);
        if (conversion != null) {
            // Hopefully, at least some attributes were converted.
            return UcfResourceObject.of(conversion.convertedObject, uidValue, errorState);
        } else {
            // Something is seriously broken, so let's return just the empty object.
            return UcfResourceObject.of(
                    originalResourceObjectDefinition.createBlankShadowWithPrimaryId(uidValue).getBean(),
                    uidValue,
                    errorState);
        }
    }

    private @NotNull String getUidValue() {
        if (connectorObjectFragment instanceof ConnectorObject co) {
            // UID value is not null according to ConnId
            return Objects.requireNonNull(co.getUid().getUidValue());
        } else {
            throw new IllegalStateException("Don't ask for UID for " + connectorObjectFragment);
        }
    }

    /**
     * The whole conversion (all except auxiliary object classes).
     *
     * It is a separate nested class to allow having all relevant private fields final.
     */
    private class Conversion {

        /** The "authoritative" resource object definition, taking into account auxiliary object classes. */
        @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

        /** The object that is being gradually built. */
        @NotNull private final ShadowType convertedObject;

        @NotNull private final List<AttributeConversionFailure> failures = new ArrayList<>();

        private Conversion() throws SchemaException {
            var auxiliaryClassesDefinitions = resolveAuxiliaryObjectClasses();
            this.resourceObjectDefinition = originalResourceObjectDefinition.composite(auxiliaryClassesDefinitions);
            this.convertedObject = resourceObjectDefinition.createBlankShadow().getBean();
            auxiliaryClassesDefinitions.forEach(
                    def -> convertedObject.getAuxiliaryObjectClass().add(def.getTypeName()));
        }

        private List<ResourceObjectDefinition> resolveAuxiliaryObjectClasses() throws SchemaException {
            List<ResourceObjectDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>();
            for (Object connIdAuxiliaryObjectClassName : getConnIdAuxiliaryObjectClasses()) {
                QName auxiliaryObjectClassQname =
                        connIdObjectClassNameToUcf((String) connIdAuxiliaryObjectClassName, isLegacySchema());
                auxiliaryObjectClassDefinitions.add(
                        resourceSchema.findObjectClassDefinitionRequired(
                                Objects.requireNonNull(auxiliaryObjectClassQname),
                                () -> " (auxiliary object class in " + connectorObjectFragment + ")"));
            }
            return auxiliaryObjectClassDefinitions;
        }

        private @NotNull List<?> getConnIdAuxiliaryObjectClasses() {
            for (Attribute connIdAttr : connectorObjectFragment.getAttributes()) {
                if (connIdAttr.is(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME)) {
                    return Objects.requireNonNullElse(connIdAttr.getValue(), List.of());
                }
            }
            return List.of();
        }

        private void convertAttributes() throws SchemaException {
            for (Attribute connIdAttr : connectorObjectFragment.getAttributes()) {
                try {
                    convertAttribute(connIdAttr);
                } catch (Throwable t) {
                    failures.add(new AttributeConversionFailure(connIdAttr.getName(), t));
                }
            }
            convertUid();
        }

        private void convertAttribute(Attribute connIdAttr) throws SchemaException {
            String connIdAttrName = connIdAttr.getName();
            LOGGER.trace("Converting ConnId attribute {}", connIdAttr);

            if (connIdAttrName.equals(Uid.NAME)) {
                // UID is handled specially (see convertUid method)
            } else if (connIdAttr.is(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME)) {
                // Already processed
            } else if (connIdAttrName.equals(OperationalAttributes.PASSWORD_NAME)) {
                // password has to go to the credentials section
                convertPassword(connIdAttr);
            } else if (connIdAttrName.equals(OperationalAttributes.ENABLE_NAME)) {
                convertEnable(connIdAttr);
            } else if (connIdAttrName.equals(OperationalAttributes.ENABLE_DATE_NAME)) {
                convertEnableDate(connIdAttr);
            } else if (connIdAttrName.equals(OperationalAttributes.DISABLE_DATE_NAME)) {
                convertDisableDate(connIdAttr);
            } else if (connIdAttrName.equals(OperationalAttributes.LOCK_OUT_NAME)) {
                convertLockOut(connIdAttr);
            } else if (connIdAttrName.equals(PredefinedAttributes.LAST_LOGIN_DATE_NAME)) {
                convertLastLoginDate(connIdAttr);
            } else {
                convertOtherAttribute(connIdAttr, connIdAttrName);
            }
        }

        private void convertPassword(Attribute connIdAttr) throws SchemaException {
            ProtectedStringType password = getSingleConvertedSimpleValue(connIdAttr, ProtectedStringType.class);
            if (password != null) {
                ShadowUtil.setPassword(convertedObject, password);
                LOGGER.trace("Converted password: {}", password);
            } else if (ConnIdAttributeUtil.isIncomplete(connIdAttr)) {
                // There is no password value in the ConnId attribute. But it was indicated that
                // that attribute is incomplete. Therefore we can assume that there in fact is a value.
                // We just do not know it.
                ShadowUtil.setPasswordIncomplete(convertedObject);
                LOGGER.trace("Converted password: (incomplete)");
            }
        }

        private void convertEnable(Attribute connIdAttr) throws SchemaException {
            Boolean enabled = getSingleConvertedSimpleValue(connIdAttr, Boolean.class);
            if (enabled == null) {
                return;
            }
            ActivationStatusType activationStatus = enabled ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED;
            ShadowUtil.getOrCreateActivation(convertedObject)
                    .administrativeStatus(activationStatus)
                    .effectiveStatus(activationStatus);
            LOGGER.trace("Converted activation administrativeStatus/effectiveStatus: {}", activationStatus);
        }

        private void convertEnableDate(Attribute connIdAttr) throws SchemaException {
            Long millis = getSingleConvertedSimpleValue(connIdAttr, Long.class);
            if (millis == null) {
                return;
            }
            ShadowUtil.getOrCreateActivation(convertedObject)
                    .validFrom(XmlTypeConverter.createXMLGregorianCalendar(millis));
        }

        private void convertDisableDate(Attribute connIdAttr) throws SchemaException {
            Long millis = getSingleConvertedSimpleValue(connIdAttr, Long.class);
            if (millis == null) {
                return;
            }
            ShadowUtil.getOrCreateActivation(convertedObject)
                    .validTo(XmlTypeConverter.createXMLGregorianCalendar(millis));
        }

        private void convertLockOut(Attribute connIdAttr) throws SchemaException {
            Boolean lockOut = getSingleConvertedSimpleValue(connIdAttr, Boolean.class);
            if (lockOut == null) {
                return;
            }
            LockoutStatusType lockoutStatus = lockOut ? LockoutStatusType.LOCKED : LockoutStatusType.NORMAL;
            ShadowUtil.getOrCreateActivation(convertedObject)
                    .lockoutStatus(lockoutStatus);
            LOGGER.trace("Converted activation lockoutStatus: {}", lockoutStatus);
        }

        private void convertLastLoginDate(Attribute connIdAttr) throws SchemaException {
            Long millis = getSingleConvertedSimpleValue(connIdAttr, Long.class);
            if (millis == null) {
                return;
            }
            ShadowUtil.getOrCreateShadowBehavior(convertedObject)
                            .lastLoginTimestamp(XmlTypeConverter.createXMLGregorianCalendar(millis));
        }

        /**
         * Adds Uid if it is not there already. It can be already present, e.g. if Uid and Name represent the same attribute.
         */
        private void convertUid() throws SchemaException {
            if (!(connectorObjectFragment instanceof ConnectorObject co)) {
                return;
            }
            Uid uid = co.getUid();
            ShadowSimpleAttributeDefinition<?> uidDefinition = ConnIdUtil.getUidDefinition(resourceObjectDefinition);
            if (uidDefinition == null) {
                throw new SchemaException("No definition for ConnId UID attribute found in " + resourceObjectDefinition);
            }
            var attributesContainer = ShadowUtil.getOrCreateAttributesContainer(convertedObject);
            if (!attributesContainer.getValue().contains(uidDefinition.getItemName())) {
                //noinspection unchecked
                ShadowSimpleAttribute<String> uidResourceObjectAttribute =
                        (ShadowSimpleAttribute<String>) uidDefinition.instantiate();
                uidResourceObjectAttribute.setRealValue(uid.getUidValue());
                attributesContainer.getValue().add(uidResourceObjectAttribute);
            }
        }

        private <T> T getSingleConvertedSimpleValue(Attribute connIdAttribute, Class<T> type)
                throws SchemaException {
            Object valueInConnId = ConnIdAttributeUtil.getSingleValue(connIdAttribute);
            Object valueInUcf = getRealValue((PrismPropertyValue<?>) convertAttributeValueFromConnId(valueInConnId));
            return MiscUtil.castSafely(valueInUcf, type, lazy(() -> " in attribute " + connIdAttribute.getName()));
        }

        private PrismValue convertAttributeValueFromConnId(Object connIdValue) throws SchemaException {
            if (connIdValue == null) {
                return null;
            }
            if (connIdValue instanceof ZonedDateTime zonedDateTime) {
                return itemFactory.createPropertyValue(
                        XmlTypeConverter.createXMLGregorianCalendar(zonedDateTime));
            }
            if (connIdValue instanceof GuardedString guardedString) {
                return itemFactory.createPropertyValue(
                        fromGuardedString(guardedString));
            }
            if (connIdValue instanceof Map<?, ?> map) {
                // TODO: check type that this is really PolyString
                //noinspection unchecked
                var ps = polyStringFromConnIdMap((Map<String, String>) map);
                return ps != null ? itemFactory.createPropertyValue(ps) : null;
            }
            if (connIdValue instanceof ConnectorObjectReference reference) {
                return convertReferenceToReferenceAttributeValue(reference);
            }
            if (connIdValue instanceof EmbeddedObject embedded) {
                return convertEmbeddedObjectToReferenceAttributeValue(embedded);
            }
            return itemFactory.createPropertyValue(connIdValue);
        }

        private @NotNull ProtectedStringType fromGuardedString(GuardedString icfValue) {
            final ProtectedStringType ps = new ProtectedStringType();
            icfValue.access(passwordChars -> {
                try {
                    ps.setClearValue(new String(passwordChars));
                    b.protector.encrypt(ps);
                } catch (EncryptionException e) {
                    throw new IllegalStateException("Protector failed to encrypt password");
                }
            });
            return ps;
        }

        private Object polyStringFromConnIdMap(Map<String, String> connIdMap) {
            String orig = null;
            Map<String, String> lang = null;
            for (Map.Entry<String, String> connIdMapEntry : connIdMap.entrySet()) {
                String key = connIdMapEntry.getKey();
                if (ConnIdUtil.POLYSTRING_ORIG_KEY.equals(key)) {
                    orig = connIdMapEntry.getValue();
                } else {
                    if (lang == null) {
                        lang = new HashMap<>();
                    }
                    lang.put(key, connIdMapEntry.getValue());
                }
            }
            if (orig != null) {
                return new PolyString(orig, null, null, lang);
            } else if (lang == null || lang.isEmpty()) {
                return null;
            } else {
                // No orig -- we need to determine it from lang.
                String language = b.localizationService.getDefaultLocale().getLanguage();
                String origForDefaultLanguage = lang.get(language);
                String computedOrig = origForDefaultLanguage != null ? origForDefaultLanguage : lang.values().iterator().next();
                return new PolyString(computedOrig, null, null, lang);
            }
        }

        private void convertOtherAttribute(Attribute connIdAttr, String connIdAttrName)
                throws SchemaException {
            var convertedAttrName = connIdAttributeNameToUcf(connIdAttrName, null, resourceObjectDefinition);

            //noinspection rawtypes
            ShadowAttributeDefinition mpDefinition = resourceObjectDefinition.findShadowAttributeDefinitionRequired(
                    convertedAttrName,
                    resourceSchema.isCaseIgnoreAttributeNames(),
                    lazy(() ->
                            connectorObjectFragment instanceof BaseConnectorObject baseConnectorObject
                                    ? "original ConnId name: '%s' in resource object identified by %s".formatted(
                                            connIdAttrName, baseConnectorObject.getIdentification())
                                    : "original ConnId name: '%s' in embedded object of class %s".formatted(
                                            connIdAttrName, connectorObjectFragment.getObjectClass())));

            var convertedAttr = mpDefinition.instantiate();
            var expectedClass = resolvePrimitiveIfNecessary(mpDefinition.getTypeClass());

            // Note: we skip uniqueness checks here because the attribute in the resource object is created from scratch.
            // I.e. its values will be unique (assuming that values coming from the resource are unique, and no two values
            // from resource are mapped into the same value in midPoint).
            for (Object connIdValue : emptyIfNull(connIdAttr.getValue())) {
                // Convert the value. While most values do not need conversions, some of them may need it (e.g. GuardedString)
                var convertedValue = convertAttributeValueFromConnId(connIdValue);
                if (convertedValue != null) {
                    // Note that the type compatibility is also checked in ResourceAttribute#checkConsistenceInternal.
                    // But that is usually turned off in production; and produces IllegalStateException. We need the
                    // SchemaException instead.
                    Class<?> realClass = resolvePrimitiveIfNecessary(convertedValue.getRealClass());
                    schemaCheck(realClass == null || expectedClass.isAssignableFrom(realClass),
                            "The value '%s' does not conform to the definition %s: expected type: %s, actual type: %s",
                            convertedValue, mpDefinition, expectedClass, realClass);
                    //noinspection unchecked
                    convertedAttr.addValueSkipUniquenessCheck(convertedValue);
                }
            }

            convertedAttr.setIncomplete(ConnIdAttributeUtil.isIncomplete(connIdAttr));
            if (!convertedAttr.hasNoValues() || convertedAttr.isIncomplete()) {
                LOGGER.trace("Converted attribute {}", convertedAttr);
                ShadowUtil.addAttribute(convertedObject, convertedAttr);
            }
        }

        private @NotNull ShadowReferenceAttributeValue convertReferenceToReferenceAttributeValue(ConnectorObjectReference reference)
                throws SchemaException {
            BaseConnectorObject targetObjectOrIdentification = reference.getValue();
            var targetObjectClassName =
                    connIdObjectClassNameToUcf(targetObjectOrIdentification.getObjectClass(), isLegacySchema());
            if (targetObjectClassName == null) {
                throw new UnsupportedOperationException(
                        "Reference attribute values without object class information are currently not supported: "
                                + targetObjectOrIdentification);
            }
            var targetObjectDefinition = resourceSchema.findDefinitionForObjectClassRequired(targetObjectClassName);

            var embeddedConversion =
                    new ConnIdToUcfObjectConversion(
                            targetObjectOrIdentification, targetObjectDefinition, connectorContext, resourceSchema);
            embeddedConversion.execute();
            // If the conversion is not successful, the conversion of the particular reference attribute - as a whole - fails
            // (and the error is handled just as if any attribute conversion failed).
            var ucfObjectFragment = embeddedConversion.getUcfResourceObjectFragmentIfSuccess();
            if (!(targetObjectOrIdentification instanceof ConnectorObject)) {
                ucfObjectFragment.setIdentificationOnly();
            }
            return ShadowReferenceAttributeValue.fromShadow(ucfObjectFragment);
        }

        // TODO deduplicate with the method above
        private @NotNull ShadowReferenceAttributeValue convertEmbeddedObjectToReferenceAttributeValue(
                EmbeddedObject embeddedObject) throws SchemaException {
            var targetObjectClassName =
                    connIdObjectClassNameToUcf(embeddedObject.getObjectClass(), isLegacySchema());
            if (targetObjectClassName == null) {
                throw new UnsupportedOperationException(
                        "Reference attribute values without object class information are currently not supported: "
                                + embeddedObject);
            }
            var targetObjectDefinition = resourceSchema.findDefinitionForObjectClassRequired(targetObjectClassName);

            var embeddedConversion =
                    new ConnIdToUcfObjectConversion(
                            embeddedObject, targetObjectDefinition, connectorContext, resourceSchema);
            embeddedConversion.execute();
            // If the conversion is not successful, the conversion of the particular reference attribute - as a whole - fails
            // (and the error is handled just as if any attribute conversion failed).
            var ucfObjectFragment = embeddedConversion.getUcfResourceObjectFragmentIfSuccess();
            return ShadowReferenceAttributeValue.fromShadow(ucfObjectFragment);
        }

        @NotNull UcfResourceObjectFragment getResourceObjectFragmentIfSuccess() throws SchemaException {
            if (failures.isEmpty()) {
                if (connectorObjectFragment instanceof ConnectorObject) {
                    return UcfResourceObject.of(convertedObject, getUidValue());
                } else {
                    return UcfResourceObjectIdentification.of(convertedObject);
                }
            } else {
                throw aggregatedSchemaException();
            }
        }

        private SchemaException aggregatedSchemaException() {
            var message = failures.stream()
                    .map(AttributeConversionFailure::getMessage)
                    .collect(Collectors.joining("; "));
            var firstCause = failures.get(0).exception;
            return new SchemaException(message, firstCause);
        }
    }

    private boolean isLegacySchema() {
        return connectorContext.isLegacySchema();
    }

    private record AttributeConversionFailure(@NotNull String attributeName, @NotNull Throwable exception) {
        String getMessage() {
            return "Error converting attribute '" + attributeName + "': " + exception.getMessage();
        }
    }
}
