/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import static com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper.*;
import static com.evolveum.midpoint.schema.processor.ObjectFactory.createNativeAttributeDefinition;

import java.util.*;
import java.util.function.Supplier;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.NativeResourceSchema.NativeResourceSchemaBuilder;
import com.evolveum.midpoint.schema.processor.ObjectFactory;

import com.evolveum.midpoint.util.DOMUtil;

import org.identityconnectors.framework.common.objects.*;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.NativeObjectClassDefinition.NativeObjectClassDefinitionBuilder;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Parses (already fetched) ConnId schema.
 *
 * To be used from within {@link ConnIdCapabilitiesAndSchemaParser}, as it feeds capability-related information back to it.
 *
 * @author Radovan Semancik
 */
class ConnIdSchemaParser {

    private static final Trace LOGGER = TraceManager.getTrace(ConnIdSchemaParser.class);

    /** The ConnId schema being parsed. */
    @NotNull private final Schema connIdSchema;

    /** Is this a legacy schema? It is determined anew, disregarding any configured value. */
    private final boolean legacySchema;

    /** Which object classes to parse? (empty = all) */
    @NotNull private final Collection<QName> objectClassesToParse;

    /** The [native] resource schema being built. */
    @NotNull private final NativeResourceSchemaBuilder schemaBuilder;

    /** When schema parsing is requested. */
    ConnIdSchemaParser(
            @NotNull Schema connIdSchema,
            @NotNull Collection<QName> objectClassesToParse,
            Boolean configuredLegacySchema) {
        this.connIdSchema = connIdSchema;
        this.legacySchema = Objects.requireNonNullElseGet(configuredLegacySchema, () -> detectLegacySchema(connIdSchema));
        this.objectClassesToParse = objectClassesToParse;
        this.schemaBuilder = ObjectFactory.createNativeResourceSchemaBuilder();
    }

    @NotNull ParsedSchemaInfo parse() throws SchemaException {

        SpecialAttributes specialAttributes = new SpecialAttributes();

        LOGGER.trace("Parsing ConnId resource schema (legacy mode: {})", legacySchema);
        LOGGER.trace("Parsing object classes: {}", objectClassesToParse);

        Set<ObjectClassInfo> objectClassInfoSet = connIdSchema.getObjectClassInfo();
        for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {
            String objectClassXsdNameLocal = connIdObjectClassNameToUcfLocal(objectClassInfo.getType(), legacySchema);

            if (shouldBeGenerated(typeNameInRi(objectClassXsdNameLocal))) {
                LOGGER.trace("Converting object class {} ({})", objectClassInfo.getType(), objectClassXsdNameLocal);
                new ObjectClassParser(objectClassXsdNameLocal, objectClassInfo, specialAttributes)
                        .parse();
            } else {
                LOGGER.trace("Skipping object class {} ({})", objectClassInfo.getType(), objectClassXsdNameLocal);
            }
        }

        schemaBuilder.computeReferenceTypes();

        NativeResourceSchema nativeSchema = schemaBuilder.getObjectBuilt();
        nativeSchema.freeze();

        return new ParsedSchemaInfo(nativeSchema, specialAttributes, legacySchema);
    }

    private boolean detectLegacySchema(Schema icfSchema) {
        Set<ObjectClassInfo> objectClassInfoSet = icfSchema.getObjectClassInfo();
        for (ObjectClassInfo objectClassInfo : objectClassInfoSet) {
            if (objectClassInfo.is(ObjectClass.ACCOUNT_NAME) || objectClassInfo.is(ObjectClass.GROUP_NAME)) {
                LOGGER.trace("This is legacy schema");
                return true;
            }
        }
        return false;
    }

    private boolean shouldBeGenerated(QName objectClassXsdName) {
        return objectClassesToParse.isEmpty()
                || objectClassesToParse.contains(objectClassXsdName);
    }

    /** Parses single ConnId object class - if applicable. */
    private class ObjectClassParser {

        @NotNull private final String objectClassXsdNameLocal;

        /** ConnId style definition. */
        @NotNull private final ObjectClassInfo connIdClassInfo;

        /** The midPoint-style object class definition being built. */
        @NotNull private final NativeObjectClassDefinitionBuilder ocDefBuilder;

        /** Already parsed attributes, indexed by their midPoint (UCF) QName. Used internally for some lookups. */
        @NotNull private final Map<QName, NativeShadowAttributeDefinitionImpl<?>> parsedItemDefinitions = new HashMap<>();

        /** Collecting information about supported special attributes (should be per object class, but currently is global). */
        @NotNull private final SpecialAttributes specialAttributes;

        /** What display order will individual attributes have. */
        int currentAttributeDisplayOrder = ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_START;

        /**
         * The definition of UID attribute, if present, and not overlapping with a different one.
         * In the case of overlap, the other attribute takes precedence, and its definition is used here.
         */
        private NativeShadowAttributeDefinitionImpl<?> uidDefinition;

        /** True if UID is the same as NAME or another attribute, and therefore its definition was taken from it. */
        private boolean uidDefinitionTakenFromAnotherAttribute;

        /** The definition of NAME attribute, if present. */
        private NativeShadowAttributeDefinitionImpl<?> nameDefinition;

        /**
         * The name of the object class is either like `AccountObjectClass`, `CustomPrivilegeObjectClass`, etc (for legacy mode)
         * or simply `account`, `privilege`, etc (for modern mode).
         */
        ObjectClassParser(
                @NotNull String objectClassXsdNameLocal,
                @NotNull ObjectClassInfo objectClassInfo,
                @NotNull SpecialAttributes specialAttributes) {
            this.objectClassXsdNameLocal = objectClassXsdNameLocal;
            this.connIdClassInfo = objectClassInfo;
            this.ocDefBuilder = schemaBuilder.newComplexTypeDefinitionLikeBuilder(objectClassXsdNameLocal);
            this.specialAttributes = specialAttributes;
        }

        private void parse() throws SchemaException {

            for (AttributeInfo connIdAttrInfo : connIdClassInfo.getAttributeInfo()) {
                boolean isSpecial = specialAttributes.updateWithAttribute(connIdAttrInfo);
                if (isSpecial) {
                    continue; // Skip this attribute, the capability presence is sufficient
                }
                var xsdAttrName = connIdAttributeNameToUcf(
                        connIdAttrInfo.getName(), connIdAttrInfo.getNativeName(), this::resolveFrameworkName);
                parseAttributeInfo(xsdAttrName, connIdAttrInfo);
            }

            completeObjectClassDefinition();

            schemaBuilder.add(ocDefBuilder);

            LOGGER.trace("  ... converted object class {}: {}", connIdClassInfo.getType(), ocDefBuilder);
        }

        private void completeObjectClassDefinition() {
            if (uidDefinition == null) {
                // Every object has UID in ConnId, therefore add a default definition if no other was specified.
                // Uid is a primary identifier of every object (this is the ConnId way).
                uidDefinition = createNativeAttributeDefinition(SchemaConstants.ICFS_UID, DOMUtil.XSD_STRING);
                uidDefinition.setMinOccurs(0); // It must not be present on create hence it cannot be mandatory.
                uidDefinition.setMaxOccurs(1);
                uidDefinition.setReadOnly();
                uidDefinition.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
                uidDefinition.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
            }
            if (uidDefinitionTakenFromAnotherAttribute) {
                // It was already added when that attribute was parsed.
            } else {
                ocDefBuilder.add(uidDefinition);
            }

            // Primary and secondary identifier information
            ocDefBuilder.setPrimaryIdentifierName(uidDefinition.getItemName());
            if (nameDefinition != null && !uidDefinition.getItemName().equals(nameDefinition.getItemName())) {
                ocDefBuilder.setSecondaryIdentifierName(nameDefinition.getItemName());
            }

            // Add other schema annotations
            ocDefBuilder.setNativeObjectClassName(connIdClassInfo.getType());
            if (nameDefinition != null) {
                ocDefBuilder.setDisplayNameAttributeName(nameDefinition.getItemName());
                ocDefBuilder.setNamingAttributeName(nameDefinition.getItemName());
            }
            ocDefBuilder.setAuxiliary(connIdClassInfo.isAuxiliary());
            ocDefBuilder.setEmbedded(connIdClassInfo.isEmbedded());

            // The __ACCOUNT__ objectclass in ConnId is a default account objectclass. So mark it appropriately.
            if (ObjectClass.ACCOUNT_NAME.equals(connIdClassInfo.getType())) {
                ocDefBuilder.setDefaultAccountDefinition(true);
            }
        }

        private ItemName resolveFrameworkName(String frameworkName) {
            for (NativeShadowAttributeDefinition parsedItemDefinition : parsedItemDefinitions.values()) {
                if (frameworkName.equals(parsedItemDefinition.getFrameworkAttributeName())) {
                    return parsedItemDefinition.getItemName();
                }
            }
            return null;
        }

        private void parseAttributeInfo(ItemName xsdItemName, AttributeInfo connIdAttrInfo) throws SchemaException {
            var connIdAttrName = connIdAttrInfo.getName();

            // This is the default type name for the reference attributes. Should be resource-wide unique.
            Supplier<String> referenceTypeNameSupplier = () -> "_" + objectClassXsdNameLocal + "_" + connIdAttrName;

            var xsdTypeName = ConnIdTypeMapper.connIdTypeToXsdTypeName(
                    connIdAttrInfo.getType(), connIdAttrInfo.getSubtype(), false, referenceTypeNameSupplier);

            var mpItemDef = createNativeAttributeDefinition(xsdItemName, xsdTypeName);

            if (!connIdAttrInfo.isReference()) {
                LOGGER.trace("  simple attribute conversion: ConnId: {}({}) -> XSD: {}({})",
                        connIdAttrName, connIdAttrInfo.getType().getSimpleName(),
                        PrettyPrinter.prettyPrintLazily(xsdItemName),
                        PrettyPrinter.prettyPrintLazily(xsdTypeName));
            } else {
                mpItemDef.setReferencedObjectClassName(
                        ConnIdNameMapper.connIdObjectClassNameToUcf(connIdAttrInfo.getReferencedObjectClassName(), legacySchema));
                // Currently we require the role in reference; later, we may try to derive it (or let provide it by engineer).
                mpItemDef.setReferenceParticipantRole(
                        determineParticipantRole(connIdAttrInfo));
                LOGGER.trace("  reference attribute conversion: ConnId: {} ({}) -> XSD: {}",
                        connIdAttrName, connIdAttrInfo.getSubtype(), PrettyPrinter.prettyPrintLazily(xsdItemName));
            }

            if (Uid.NAME.equals(connIdAttrName)) {
                // UID can be the same as a different attribute (maybe NAME, maybe some other). We take that one as authoritative.
                var existingDefinition = parsedItemDefinitions.get(xsdItemName);
                if (existingDefinition != null) {
                    LOGGER.trace("Using other attribute definition for UID: {}", existingDefinition);
                    existingDefinition.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                    uidDefinition = existingDefinition;
                    uidDefinitionTakenFromAnotherAttribute = true;
                    return; // done with this attribute
                } else {
                    uidDefinition = mpItemDef;
                    if (connIdAttrInfo.getNativeName() == null) {
                        mpItemDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_NAME);
                    }
                    mpItemDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                }
            } else {
                if (Name.NAME.equals(connIdAttrName)) {
                    nameDefinition = mpItemDef;
                }
                // Check for a conflict with UID definition. This definition will take precedence of the one from UID.
                // (But beware, we may not have come across UID definition yet. The code above cares for that.)
                if (uidDefinition != null && xsdItemName.equals(uidDefinition.getItemName())) {
                    LOGGER.trace("Using other attribute definition for UID: {}", mpItemDef);
                    mpItemDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_UID_DISPLAY_ORDER);
                    uidDefinition = mpItemDef;
                    uidDefinitionTakenFromAnotherAttribute = true;
                } else {
                    // Set the display order + display name appropriately
                    if (Name.NAME.equals(connIdAttrName)) {
                        if (connIdAttrInfo.getNativeName() == null) {
                            // Set a better display name for __NAME__. The "name" is s very overloaded term,
                            // so let's try to make things a bit clearer.
                            mpItemDef.setDisplayName(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_NAME);
                        }
                        mpItemDef.setDisplayOrder(ConnectorFactoryConnIdImpl.ICFS_NAME_DISPLAY_ORDER);
                    } else {
                        mpItemDef.setDisplayOrder(currentAttributeDisplayOrder);
                        currentAttributeDisplayOrder += ConnectorFactoryConnIdImpl.ATTR_DISPLAY_ORDER_INCREMENT;
                    }
                }
            }

            processCommonDefinitionParts(mpItemDef, connIdAttrInfo);
            mpItemDef.setMatchingRuleQName(
                    connIdAttributeInfoToMatchingRule(connIdAttrInfo));

            if (!Uid.NAME.equals(connIdAttrName)) {
                ocDefBuilder.add(mpItemDef);
                parsedItemDefinitions.put(mpItemDef.getItemName(), mpItemDef);
            } else {
                LOGGER.trace("-> UID will be added after parsing all attributes");
            }
        }

        private @NotNull ShadowReferenceParticipantRole determineParticipantRole(AttributeInfo connIdAttrInfo)
                throws SchemaException {
            var stringValue = connIdAttrInfo.getRoleInReference();
            if (AttributeInfo.RoleInReference.SUBJECT.toString().equals(stringValue)) {
                return ShadowReferenceParticipantRole.SUBJECT;
            } else if (AttributeInfo.RoleInReference.OBJECT.toString().equals(stringValue)) {
                return ShadowReferenceParticipantRole.OBJECT;
            } else if (stringValue == null) {
                throw new SchemaException("Missing role in reference in %s".formatted(connIdAttrInfo));
            } else {
                throw new SchemaException("Unsupported role in reference: '%s' in %s".formatted(stringValue, connIdAttrInfo));
            }
        }

        private void processCommonDefinitionParts(NativeShadowAttributeDefinitionImpl<?> mpItemDef, AttributeInfo connIdAttrInfo) {
            mpItemDef.setMinOccurs(connIdAttrInfo.isRequired() ? 1 : 0);
            mpItemDef.setMaxOccurs(connIdAttrInfo.isMultiValued() ? -1 : 1);

            mpItemDef.setCanRead(connIdAttrInfo.isReadable());
            mpItemDef.setCanAdd(connIdAttrInfo.isCreateable());
            mpItemDef.setCanModify(connIdAttrInfo.isUpdateable());

            mpItemDef.setNativeAttributeName(connIdAttrInfo.getNativeName());
            mpItemDef.setFrameworkAttributeName(connIdAttrInfo.getName());
            mpItemDef.setReturnedByDefault(connIdAttrInfo.isReturnedByDefault());
        }

        private QName connIdAttributeInfoToMatchingRule(AttributeInfo attributeInfo) {
            String connIdSubtype = attributeInfo.getSubtype();
            if (connIdSubtype == null) {
                return null;
            }
            if (AttributeInfo.Subtypes.STRING_CASE_IGNORE.toString().equals(connIdSubtype)) {
                return PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME;
            }
            if (AttributeInfo.Subtypes.STRING_LDAP_DN.toString().equals(connIdSubtype)) {
                return PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME;
            }
            if (AttributeInfo.Subtypes.STRING_XML.toString().equals(connIdSubtype)) {
                return PrismConstants.XML_MATCHING_RULE_NAME;
            }
            if (AttributeInfo.Subtypes.STRING_UUID.toString().equals(connIdSubtype)) {
                return PrismConstants.UUID_MATCHING_RULE_NAME;
            }
            LOGGER.debug("Unknown subtype {} defined for attribute {}, ignoring (no matching rule definition)",
                    connIdSubtype, attributeInfo.getName());
            return null;
        }
    }

    /**
     * Information about special attributes present in the schema. They are not listed among standard attributes.
     * We use them to determine specific capabilities instead.
     */
    static class SpecialAttributes {
        AttributeInfo passwordAttributeInfo;
        AttributeInfo enableAttributeInfo;
        AttributeInfo enableDateAttributeInfo;
        AttributeInfo disableDateAttributeInfo;
        AttributeInfo lockoutAttributeInfo;
        AttributeInfo auxiliaryObjectClassAttributeInfo;
        AttributeInfo lastLoginDateAttributeInfo;

        /**
         * Updates the current knowledge about special attributes with the currently parsed attribute.
         * Returns true if the match was found (i.e. current attribute is "special"), so the current attribute
         * should not be listed among normal ones.
         */
        boolean updateWithAttribute(@NotNull AttributeInfo attributeInfo) {
            String icfName = attributeInfo.getName();
            if (OperationalAttributes.PASSWORD_NAME.equals(icfName)) {
                passwordAttributeInfo = attributeInfo;
                return true;
            }

            if (OperationalAttributes.ENABLE_NAME.equals(icfName)) {
                enableAttributeInfo = attributeInfo;
                return true;
            }

            if (OperationalAttributes.ENABLE_DATE_NAME.equals(icfName)) {
                enableDateAttributeInfo = attributeInfo;
                return true;
            }

            if (OperationalAttributes.DISABLE_DATE_NAME.equals(icfName)) {
                disableDateAttributeInfo = attributeInfo;
                return true;
            }

            if (PredefinedAttributes.LAST_LOGIN_DATE_NAME.equals(icfName)) {
                lastLoginDateAttributeInfo = attributeInfo;
                return true;
            }

            if (OperationalAttributes.LOCK_OUT_NAME.equals(icfName)) {
                lockoutAttributeInfo = attributeInfo;
                return true;
            }

            if (PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME.equals(icfName)) {
                auxiliaryObjectClassAttributeInfo = attributeInfo;
                return true;
            }

            return false;
        }
    }

    /** Binds parsed schema with "special attributes" information, used to determine additional capabilities. Internal use. */
    record ParsedSchemaInfo(
            @NotNull NativeResourceSchema parsedSchema,
            @NotNull SpecialAttributes specialAttributes,
            boolean legacySchema) {
    }
}
