/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import static java.util.Collections.emptyList;

/**
 * Conversion of a single ConnId connector object to midPoint resource object.
 */
class ConnIdToMidPointConversion {

    private static final Trace LOGGER = TraceManager.getTrace(ConnIdToMidPointConversion.class);

    /** The input (the object that came from ConnId) */
    @NotNull private final ConnectorObject connectorObject;

    /** The output resource object that is being filled-in */
    @NotNull private final PrismObject<ShadowType> resourceObject;

    /** The "bean" version of the output object */
    @NotNull private final ShadowType resourceObjectBean;

    /** The attributes container of the output resource object */
    @NotNull private final ResourceAttributeContainer attributesContainer;

    /** The definition of the attributes container */
    @NotNull private final ResourceAttributeContainerDefinition attributesContainerDefinition;

    /** Whether to convert empty and not present attributes (according to the schema) */
    private final boolean full;

    /** True if attribute names are case-insensitive */
    private final boolean caseIgnoreAttributeNames;

    /** True if we are in "legacy schema" mode. See e.g. https://docs.evolveum.com/connectors/connid/1.x/connector-development-guide/#schema-best-practices */
    private final boolean legacySchema;

    /** The context (various useful beans) */
    private final ConnIdConvertor connIdConvertor;

    private final List<ObjectClassComplexTypeDefinition> auxiliaryObjectClassDefinitions = new ArrayList<>();

    ConnIdToMidPointConversion(@NotNull ConnectorObject connectorObject, @NotNull PrismObject<ShadowType> resourceObject,
            boolean full, boolean caseIgnoreAttributeNames, boolean legacySchema, ConnIdConvertor connIdConvertor)
            throws SchemaException {
        this.connectorObject = connectorObject;
        this.resourceObject = resourceObject;
        this.resourceObjectBean = resourceObject.asObjectable();
        this.attributesContainer = (ResourceAttributeContainer) (PrismContainer)
                resourceObject.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        this.attributesContainerDefinition = attributesContainer.getDefinition();
        this.full = full;
        this.caseIgnoreAttributeNames = caseIgnoreAttributeNames;
        this.legacySchema = legacySchema;
        this.connIdConvertor = connIdConvertor;
    }

    @NotNull PrismObject<ShadowType> execute() throws SchemaException {

        convertObjectClasses();

        for (Attribute connIdAttr : connectorObject.getAttributes()) {
            convertAttribute(connIdAttr);
        }

        convertUid();

        return resourceObject;
    }

    private void convertObjectClasses() throws SchemaException {
        resourceObjectBean.setObjectClass(attributesContainerDefinition.getTypeName());
        convertAuxiliaryObjectClasses();
    }

    private void convertAuxiliaryObjectClasses() throws SchemaException {
        List<QName> auxiliaryObjectClasses = resourceObjectBean.getAuxiliaryObjectClass();
        ConnIdNameMapper nameMapper = connIdConvertor.connIdNameMapper;
        for (Object connIdAuxiliaryObjectClass : getConnIdAuxiliaryObjectClasses()) {
            QName auxiliaryObjectClassQname = nameMapper
                    .objectClassToQname(new ObjectClass((String) connIdAuxiliaryObjectClass),
                            connIdConvertor.resourceSchemaNamespace, legacySchema);
            auxiliaryObjectClasses.add(auxiliaryObjectClassQname);
            ObjectClassComplexTypeDefinition auxiliaryObjectClassDefinition = nameMapper.getResourceSchema()
                    .findObjectClassDefinition(auxiliaryObjectClassQname);
            if (auxiliaryObjectClassDefinition == null) {
                throw new SchemaException(
                        "Resource object " + connectorObject + " refers to auxiliary object class " + auxiliaryObjectClassQname
                                + " which is not in the schema");
            }
            auxiliaryObjectClassDefinitions.add(auxiliaryObjectClassDefinition);
        }
    }

    private List<?> getConnIdAuxiliaryObjectClasses() {
        for (Attribute connIdAttr : connectorObject.getAttributes()) {
            if (connIdAttr.is(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME)) {
                return connIdAttr.getValue();
            }
        }
        return emptyList();
    }

    private void convertAttribute(Attribute connIdAttr) throws SchemaException {
        List<Object> values = emptyIfNull(connIdAttr.getValue());
        String connIdAttrName = connIdAttr.getName();
        LOGGER.trace("Reading ICF attribute {}: {}", connIdAttrName, values);

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
        if (password == null) {
            if (isIncomplete(connIdAttr)) {
                // There is no password value in the ConnId attribute. But it was indicated that
                // that attribute is incomplete. Therefore we can assume that there in fact is a value.
                // We just do not know it.
                ShadowUtil.setPasswordIncomplete(resourceObjectBean);
                LOGGER.trace("Converted password: (incomplete)");
            }
        } else {
            ShadowUtil.setPassword(resourceObjectBean, password);
            LOGGER.trace("Converted password: {}", password);
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
        ActivationType activation = ShadowUtil.getOrCreateActivation(resourceObjectBean);
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
        ActivationType activation = ShadowUtil.getOrCreateActivation(resourceObjectBean);
        activation.setValidFrom(XmlTypeConverter.createXMLGregorianCalendar(millis));
    }

    private void convertDisableDate(Attribute connIdAttr) throws SchemaException {
        Long millis = getSingleValue(connIdAttr, Long.class);
        if (millis == null) {
            return;
        }
        ActivationType activation = ShadowUtil.getOrCreateActivation(resourceObjectBean);
        activation.setValidTo(XmlTypeConverter.createXMLGregorianCalendar(millis));
    }

    private void convertLockOut(Attribute connIdAttr) throws SchemaException {
        Boolean lockOut = getSingleValue(connIdAttr, Boolean.class);
        if (lockOut == null) {
            return;
        }
        ActivationType activation = ShadowUtil.getOrCreateActivation(resourceObjectBean);
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
        ObjectClassComplexTypeDefinition ocDef = attributesContainerDefinition.getComplexTypeDefinition();
        ResourceAttributeDefinition<String> uidDefinition = ConnIdUtil.getUidDefinition(ocDef);
        if (uidDefinition == null) {
            throw new SchemaException("No definition for ConnId UID attribute found in definition " + ocDef);
        }
        if (!attributesContainer.getValue().contains(uidDefinition.getItemName())) {
            ResourceAttribute<String> uidResourceObjectAttribute = uidDefinition.instantiate();
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
        if (connIdValue instanceof ZonedDateTime) {
            return XmlTypeConverter.createXMLGregorianCalendar((ZonedDateTime) connIdValue);
        }
        if (connIdValue instanceof GuardedString) {
            return fromGuardedString((GuardedString) connIdValue);
        }
        if (connIdValue instanceof Map) {
            // TODO: check type that this is really PolyString
            //noinspection unchecked
            return polyStringFromConnIdMap((Map<String, String>) connIdValue);
        }
        return connIdValue;
    }

    private ProtectedStringType fromGuardedString(GuardedString icfValue) {
        final ProtectedStringType ps = new ProtectedStringType();
        icfValue.access(passwordChars -> {
            try {
                ps.setClearValue(new String(passwordChars));
                connIdConvertor.protector.encrypt(ps);
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
            String language = connIdConvertor.localizationService.getDefaultLocale().getLanguage();
            String origForDefaultLanguage = lang.get(language);
            String computedOrig = origForDefaultLanguage != null ? origForDefaultLanguage : lang.values().iterator().next();
            return new PolyString(computedOrig, null, null, lang);
        }
    }

    private void convertStandardAttribute(Attribute connIdAttr, String connIdAttrName, List<Object> values) throws SchemaException {
        ItemName convertedAttrName = ItemName.fromQName(
                connIdConvertor.connIdNameMapper.convertAttributeNameToQName(connIdAttrName, attributesContainerDefinition));
        ResourceAttributeDefinition<Object> convertedAttributeDefinition = findAttributeDefinition(connIdAttrName, convertedAttrName);

        QName normalizedAttributeName;
        if (caseIgnoreAttributeNames) {
            normalizedAttributeName = convertedAttributeDefinition.getItemName(); // this is the normalized version
        } else {
            normalizedAttributeName = convertedAttrName;
        }

        ResourceAttribute<Object> convertedAttribute = convertedAttributeDefinition.instantiate(normalizedAttributeName);

        convertedAttribute.setIncomplete(isIncomplete(connIdAttr));

        // Note: we skip uniqueness checks here because the attribute in the resource object is created from scratch.
        // I.e. its values will be unique (assuming that values coming from the resource are unique).

        // if full == true, we need to convert whole connector object to the
        // resource object also with the null-values attributes
        if (full) {
            for (Object connIdValue : values) {
                // Convert the value. While most values do not need conversions, some of them may need it (e.g. GuardedString)
                Object convertedValue = convertValueFromConnId(connIdValue);
                convertedAttribute.addRealValueSkipUniquenessCheck(convertedValue);
            }

            LOGGER.trace("Converted attribute {}", convertedAttribute);
            attributesContainer.getValue().add(convertedAttribute);
        } else {
            // In this case (full=false) we need only the attributes with non-null values.
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
    }

    @NotNull
    private ResourceAttributeDefinition<Object> findAttributeDefinition(String connIdAttrName, ItemName convertedAttrName)
            throws SchemaException {
        ResourceAttributeDefinition<Object> attributeDefinition = attributesContainerDefinition
                .findAttributeDefinition(convertedAttrName, caseIgnoreAttributeNames);
        if (attributeDefinition != null) {
            return attributeDefinition;
        }

        // Try to locate definition in auxiliary object classes
        for (ObjectClassComplexTypeDefinition auxiliaryObjectClassDefinition : auxiliaryObjectClassDefinitions) {
            ResourceAttributeDefinition<Object> auxAttributeDefinition = auxiliaryObjectClassDefinition
                    .findAttributeDefinition(convertedAttrName, caseIgnoreAttributeNames);
            if (auxAttributeDefinition != null) {
                return auxAttributeDefinition;
            }
        }

        throw new SchemaException("Unknown attribute " + convertedAttrName + " in definition of object class "
                + attributesContainerDefinition.getTypeName() + ". Original ConnId name: " + connIdAttrName
                + " in resource object identified by " + connectorObject.getName(), convertedAttrName);
    }

    /**
     * Returns the partially converted resource object with an indication of a conversion failure.
     * @param result Operation result reflecting the failure.
     */
    @NotNull PrismObject<ShadowType> reportErrorInFetchResult(OperationResult result) {
        ObjectTypeUtil.recordFetchError(resourceObject, result);
        if (resourceObject.asObjectable().getName() == null) {
            resourceObject.asObjectable().setName(PolyStringType.fromOrig(connectorObject.getUid().getUidValue()));
        }
        return resourceObject;
    }
}
