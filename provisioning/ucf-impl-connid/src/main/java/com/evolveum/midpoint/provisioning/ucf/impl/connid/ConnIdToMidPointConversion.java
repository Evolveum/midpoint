/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Conversion of a single ConnId connector object to midPoint resource object.
 */
class ConnIdToMidPointConversion {

    private static final Trace LOGGER = TraceManager.getTrace(ConnIdToMidPointConversion.class);

    /** The input (the object that came from ConnId) */
    @NotNull private final ConnectorObject connectorObject;

    /** The definition provided by the caller. May be augmented (via auxiliary object classes) by the conversion process. */
    @NotNull private final ResourceObjectDefinition originalResourceObjectDefinition;

    @NotNull private final CompleteResourceSchema resourceSchema;

    /** True if we are in "legacy schema" mode. See e.g. https://docs.evolveum.com/connectors/connid/1.x/connector-development-guide/#schema-best-practices */
    private final boolean legacySchema;

    /** Name mapper is used for various partial conversions (aux object class names, attribute names). */
    @NotNull private final ConnIdNameMapper connIdNameMapper;

    @NotNull private final ConnIdBeans b = ConnIdBeans.get();

    private Conversion conversion;

    ConnIdToMidPointConversion(
            @NotNull ConnectorObject connectorObject,
            @NotNull ResourceObjectDefinition originalResourceObjectDefinition,
            @NotNull CompleteResourceSchema resourceSchema,
            boolean legacySchema,
            @NotNull ConnIdNameMapper connIdNameMapper) {
        this.connectorObject = connectorObject;
        this.originalResourceObjectDefinition = originalResourceObjectDefinition;
        this.resourceSchema = resourceSchema;
        this.legacySchema = legacySchema;
        this.connIdNameMapper = connIdNameMapper;
    }

    void execute() throws SchemaException {
        conversion = new Conversion();
        conversion.convertAttributes();
    }

    /**
     * In order to report errors both in "exception" and "ucf_object" mode, this method throws an exception if the conversion
     * was not entirely successful. The client can then call {@link #getPartialUcfResourceObject(UcfErrorState)} to get the
     * partial result, with customized error state that may include client-supplied context.
     */
    @NotNull UcfResourceObject getUcfResourceObjectIfSuccess() throws SchemaException {
        return conversion.getResourceObjectIfSuccess();
    }

    @NotNull UcfResourceObject getPartialUcfResourceObject(UcfErrorState errorState) throws SchemaException {
        String uidValue = getUidValue();
        if (conversion != null) {
            return UcfResourceObject.of(conversion.resourceObject, uidValue, errorState);
        } else {
            // Something is seriously broken, so let's return just the empty object
            return UcfResourceObject.of(originalResourceObjectDefinition.createBlankShadow(uidValue), uidValue, errorState);
        }
    }

    private @NotNull String getUidValue() {
        // UID value is not null according to ConnId
        return Objects.requireNonNull(connectorObject.getUid().getUidValue());
    }

    /**
     * The whole conversion (all except auxiliary object classes). It is a separate nested class to allow having
     * all relevant private fields final.
     */
    private class Conversion {

        /** The "authoritative" resource object definition, taking into account auxiliary object classes. */
        @NotNull private final ResourceObjectDefinition resourceObjectDefinition;

        /** The output resource object that is being filled-in. May be partial in case of error. */
        @NotNull private final ShadowType resourceObject;

        /** The attributes container of {@link #resourceObject}. */
        @NotNull private final ResourceAttributeContainer attributesContainer;

        @NotNull private final List<AttributeConversionFailure> failures = new ArrayList<>();

        private Conversion() throws SchemaException {
            var auxiliaryClassesDefinitions = resolveAuxiliaryObjectClasses();
            this.resourceObjectDefinition = originalResourceObjectDefinition.composite(auxiliaryClassesDefinitions);
            this.resourceObject = resourceObjectDefinition.createBlankShadow().asObjectable();
            this.attributesContainer = ShadowUtil.getOrCreateAttributesContainer(resourceObject);

            auxiliaryClassesDefinitions.forEach(
                    def -> resourceObject.getAuxiliaryObjectClass().add(def.getTypeName()));
        }

        private List<ResourceObjectDefinition> resolveAuxiliaryObjectClasses() throws SchemaException {
            List<ResourceObjectDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>();
            for (Object connIdAuxiliaryObjectClassName : getConnIdAuxiliaryObjectClasses()) {
                QName auxiliaryObjectClassQname =
                        connIdNameMapper.objectClassToQname(new ObjectClass((String) connIdAuxiliaryObjectClassName), legacySchema);
                auxiliaryObjectClassDefinitions.add(
                        resourceSchema.findObjectClassDefinitionRequired(
                                auxiliaryObjectClassQname,
                                () -> " (auxiliary object class in " + connectorObject + ")"));
            }
            return auxiliaryObjectClassDefinitions;
        }

        private @NotNull List<?> getConnIdAuxiliaryObjectClasses() {
            for (Attribute connIdAttr : connectorObject.getAttributes()) {
                if (connIdAttr.is(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME)) {
                    return Objects.requireNonNullElse(connIdAttr.getValue(), List.of());
                }
            }
            return List.of();
        }

        private void convertAttributes() throws SchemaException {
            for (Attribute connIdAttr : connectorObject.getAttributes()) {
                try {
                    convertAttribute(connIdAttr);
                } catch (Throwable t) {
                    failures.add(new AttributeConversionFailure(connIdAttr.getName(), t));
                }
            }
            convertUid();
        }

        private void convertAttribute(Attribute connIdAttr) throws SchemaException {
            List<Object> values = emptyIfNull(connIdAttr.getValue());
            String connIdAttrName = connIdAttr.getName();
            LOGGER.trace("Reading ConnId attribute {}: {}", connIdAttrName, values);

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
            } else {
                convertStandardAttribute(connIdAttr, connIdAttrName, values);
            }
        }

        private void convertPassword(Attribute connIdAttr) throws SchemaException {
            ProtectedStringType password = getSingleValue(connIdAttr, ProtectedStringType.class);
            if (password != null) {
                ShadowUtil.setPassword(resourceObject, password);
                LOGGER.trace("Converted password: {}", password);
            } else if (isIncomplete(connIdAttr)) {
                // There is no password value in the ConnId attribute. But it was indicated that
                // that attribute is incomplete. Therefore we can assume that there in fact is a value.
                // We just do not know it.
                ShadowUtil.setPasswordIncomplete(resourceObject);
                LOGGER.trace("Converted password: (incomplete)");
            }
        }

        private boolean isIncomplete(Attribute connIdAttr) {
            // equals() instead of == is needed. The AttributeValueCompleteness enum may be loaded by different classloader
            return AttributeValueCompleteness.INCOMPLETE.equals(connIdAttr.getAttributeValueCompleteness());
        }

        private void convertEnable(Attribute connIdAttr) throws SchemaException {
            Boolean enabled = getSingleValue(connIdAttr, Boolean.class);
            if (enabled == null) {
                return;
            }
            ActivationType activation = ShadowUtil.getOrCreateActivation(resourceObject);
            ActivationStatusType activationStatus;
            if (enabled) {
                activationStatus = ActivationStatusType.ENABLED;
            } else {
                activationStatus = ActivationStatusType.DISABLED;
            }
            activation.setAdministrativeStatus(activationStatus);
            activation.setEffectiveStatus(activationStatus);
            LOGGER.trace("Converted activation administrativeStatus/effectiveStatus: {}", activationStatus);
        }

        private void convertEnableDate(Attribute connIdAttr) throws SchemaException {
            Long millis = getSingleValue(connIdAttr, Long.class);
            if (millis == null) {
                return;
            }
            ActivationType activation = ShadowUtil.getOrCreateActivation(resourceObject);
            activation.setValidFrom(XmlTypeConverter.createXMLGregorianCalendar(millis));
        }

        private void convertDisableDate(Attribute connIdAttr) throws SchemaException {
            Long millis = getSingleValue(connIdAttr, Long.class);
            if (millis == null) {
                return;
            }
            ActivationType activation = ShadowUtil.getOrCreateActivation(resourceObject);
            activation.setValidTo(XmlTypeConverter.createXMLGregorianCalendar(millis));
        }

        private void convertLockOut(Attribute connIdAttr) throws SchemaException {
            Boolean lockOut = getSingleValue(connIdAttr, Boolean.class);
            if (lockOut == null) {
                return;
            }
            ActivationType activation = ShadowUtil.getOrCreateActivation(resourceObject);
            LockoutStatusType lockoutStatus;
            if (lockOut) {
                lockoutStatus = LockoutStatusType.LOCKED;
            } else {
                lockoutStatus = LockoutStatusType.NORMAL;
            }
            activation.setLockoutStatus(lockoutStatus);
            LOGGER.trace("Converted activation lockoutStatus: {}", lockoutStatus);
        }

        /**
         * Adds Uid if it is not there already. It can be already present, e.g. if Uid and Name represent the same attribute.
         */
        private void convertUid() throws SchemaException {
            Uid uid = connectorObject.getUid();
            ResourceAttributeDefinition<?> uidDefinition = ConnIdUtil.getUidDefinition(resourceObjectDefinition);
            if (uidDefinition == null) {
                throw new SchemaException("No definition for ConnId UID attribute found in " + resourceObjectDefinition);
            }
            if (!attributesContainer.getValue().contains(uidDefinition.getItemName())) {
                //noinspection unchecked
                ResourceAttribute<String> uidResourceObjectAttribute =
                        (ResourceAttribute<String>) uidDefinition.instantiate();
                uidResourceObjectAttribute.setRealValue(uid.getUidValue());
                attributesContainer.getValue().add(uidResourceObjectAttribute);
            }
        }

        private <T> T getSingleValue(Attribute icfAttr, Class<T> type) throws SchemaException {
            List<Object> values = icfAttr.getValue();
            if (values != null && !values.isEmpty()) {
                if (values.size() > 1) {
                    throw new SchemaException("Expected single value for " + icfAttr.getName());
                }
                Object val = convertValueFromConnId(values.get(0));
                if (val == null) {
                    return null;
                }
                if (type.isAssignableFrom(val.getClass())) {
                    //noinspection unchecked
                    return (T) val;
                } else {
                    throw new SchemaException("Expected type " + type.getName() + " for " + icfAttr.getName()
                            + " but got " + val.getClass().getName());
                }
            } else {
                return null;
            }
        }

        private Object convertValueFromConnId(Object connIdValue) {
            if (connIdValue == null) {
                return null;
            }
            if (connIdValue instanceof ZonedDateTime zonedDateTime) {
                return XmlTypeConverter.createXMLGregorianCalendar(zonedDateTime);
            }
            if (connIdValue instanceof GuardedString guardedString) {
                return fromGuardedString(guardedString);
            }
            if (connIdValue instanceof Map<?, ?> map) {
                // TODO: check type that this is really PolyString
                //noinspection unchecked
                return polyStringFromConnIdMap((Map<String, String>) map);
            }
            return connIdValue;
        }

        private ProtectedStringType fromGuardedString(GuardedString icfValue) {
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

        private void convertStandardAttribute(Attribute connIdAttr, String connIdAttrName, List<Object> values) throws SchemaException {
            ItemName convertedAttrName = ItemName.fromQName(
                    connIdNameMapper.convertAttributeNameToQName(connIdAttrName, resourceObjectDefinition));
            //noinspection unchecked
            ResourceAttributeDefinition<Object> convertedAttributeDefinition =
                    (ResourceAttributeDefinition<Object>) findAttributeDefinition(connIdAttrName, convertedAttrName);

            QName normalizedAttributeName;
            if (resourceSchema.isCaseIgnoreAttributeNames()) {
                normalizedAttributeName = convertedAttributeDefinition.getItemName(); // this is the normalized version
            } else {
                normalizedAttributeName = convertedAttrName;
            }

            ResourceAttribute<Object> convertedAttribute = convertedAttributeDefinition.instantiate(normalizedAttributeName);

            convertedAttribute.setIncomplete(isIncomplete(connIdAttr));

            // Note: we skip uniqueness checks here because the attribute in the resource object is created from scratch.
            // I.e. its values will be unique (assuming that values coming from the resource are unique).

            for (Object connIdValue : values) {
                if (connIdValue != null) {
                    // Convert the value. While most values do not need conversions, some of them may need it (e.g. GuardedString)
                    Object convertedValue = convertValueFromConnId(connIdValue);
                    convertedAttribute.addRealValueSkipUniquenessCheck(convertedValue);
                }
            }

            if (!convertedAttribute.getValues().isEmpty() || convertedAttribute.isIncomplete()) {
                LOGGER.trace("Converted attribute {}", convertedAttribute);
                attributesContainer.getValue().add(convertedAttribute);
            }
        }

        private @NotNull ResourceAttributeDefinition<?> findAttributeDefinition(String connIdAttrName, ItemName convertedAttrName)
                throws SchemaException {
            ResourceAttributeDefinition<Object> attributeDefinition =
                    resourceObjectDefinition.findAttributeDefinition(convertedAttrName, resourceSchema.isCaseIgnoreAttributeNames());
            if (attributeDefinition != null) {
                return attributeDefinition;
            }

            throw new SchemaException(
                    ("Unknown attribute %s in definition of object class %s. "
                            + "Original ConnId name: %s in resource object identified by %s").formatted(
                            convertedAttrName, resourceObjectDefinition.getTypeName(),
                            connIdAttrName, connectorObject.getName()),
                    convertedAttrName);
        }

        /** Throws an aggregated exception in case of failures. */
        @NotNull UcfResourceObject getResourceObjectIfSuccess() throws SchemaException {
            if (failures.isEmpty()) {
                return UcfResourceObject.of(
                        resourceObject, getUidValue(), UcfErrorState.success());
            } else {
                var message = failures.stream()
                        .map(AttributeConversionFailure::getMessage)
                        .collect(Collectors.joining("; "));
                var firstCause = failures.get(0).exception;
                throw new SchemaException(message, firstCause);
            }
        }
    }

    private record AttributeConversionFailure(@NotNull String attributeName, @NotNull Throwable exception) {
        String getMessage() {
            return "Error converting attribute '" + attributeName + "': " + exception.getMessage();
        }
    }
}
