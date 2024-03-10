/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;

import org.identityconnectors.framework.common.objects.ObjectClass;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts (maps) ConnId names to midPoint names and vice versa - for object classes, attributes, and so on.
 * Tightly bound to {@link ConnectorInstanceConnIdImpl}.
 *
 * When converting, it prefers using native names. So, for example, `+__NAME__+` is converted to `ri:dn` for LDAP,
 * instead of `icfs:name`.
 */
public class ConnIdNameMapper {

    private static final String CUSTOM_OBJECTCLASS_PREFIX = "Custom";
    private static final String CUSTOM_OBJECTCLASS_SUFFIX = "ObjectClass";

    /**
     * Converts ConnId attribute name (either native or ConnId-style e.g.  __NAME__) to midPoint/UCF {@link ItemName}.
     * For UID, NAME and other special ConnId names it prefers native names, provided either explicitly by caller of found
     * in the definition.
     *
     * @param connIdAttrName ConnId attribute name (either native or ConnId-style e.g.  __NAME__)
     * @param nativeAttrName Native attribute name (if known), e.g. "dn" for __NAME__ in LDAP
     * @param ocDef Relevant object definition. It may contain the definition of the special attribute we are asking about.
     */
    static ItemName connIdAttributeNameToUcf(
            @NotNull String connIdAttrName, @Nullable String nativeAttrName, @NotNull ResourceObjectDefinition ocDef) {

        if (SpecialItemNameMapper.isConnIdNameSpecial(connIdAttrName)) {
            if (nativeAttrName != null) {
                // native name is explicitly provided
                return nameInRiNamespace(nativeAttrName);
            }
            for (ResourceAttributeDefinition<?> attributeDefinition : ocDef.getAttributeDefinitions()) {
                if (connIdAttrName.equals(attributeDefinition.getFrameworkAttributeName())) {
                    // the definition is found in the object class -> use the official name
                    return attributeDefinition.getItemName();
                }
            }
            // not found -> use automatic translation (fallback, compatibility)
            return SpecialItemNameMapper.toUcfFormat(connIdAttrName);
        } else {
            // This is not a special name, just use it as is.
            return nameInRiNamespace(connIdAttrName);
        }
    }

    /** Name should NOT be in `+__NAME__+` format. */
    private static ItemName nameInRiNamespace(String name) {
        return new ItemName(MidPointConstants.NS_RI, QNameUtil.escapeElementName(name), MidPointConstants.PREFIX_NS_RI);
    }

    static String ucfAttributeNameToConnId(PropertyDelta<?> attributeDelta, ResourceObjectDefinition ocDef) throws SchemaException {
        PrismPropertyDefinition<?> propDef = attributeDelta.getDefinition();
        ResourceAttributeDefinition<?> attrDef;
        if (propDef instanceof ResourceAttributeDefinition<?> rad) {
            attrDef = rad;
        } else {
            attrDef = ocDef.findAttributeDefinitionRequired(attributeDelta.getElementName());
        }
        return ucfAttributeNameToConnId(attrDef);
    }

    static String ucfAttributeNameToConnId(ResourceAttribute<?> attribute, ResourceObjectDefinition ocDef)
            throws SchemaException {
        ResourceAttributeDefinition<?> attrDef = attribute.getDefinition();
        if (attrDef == null) {
            attrDef = ocDef.findAttributeDefinitionRequired(attribute.getElementName());
        }
        return ucfAttributeNameToConnId(attrDef);
    }

    public static String ucfAttributeNameToConnId(QName attributeName, ResourceObjectDefinition ocDef, String desc)
            throws SchemaException {
        return ucfAttributeNameToConnId(
                ocDef.findAttributeDefinitionRequired(attributeName, () -> " " + desc));
    }

    static String ucfAttributeNameToConnId(ResourceAttributeDefinition<?> attrDef) throws SchemaException {
        if (attrDef.getFrameworkAttributeName() != null) {
            // This is the special name, as registered in the schema.
            return attrDef.getFrameworkAttributeName();
        }

        QName ucfAttrName = attrDef.getItemName();

        if (SpecialItemNameMapper.isUcfNameSpecial(ucfAttrName)) {
            // This is the special name, as determined by static mappings.
            return SpecialItemNameMapper.toConnIdFormat(ucfAttrName);
        }

        if (ucfAttrName.getNamespaceURI().equals(MidPointConstants.NS_RI)) {
            // We are in the correct namespace, so we assume the name can be used "as is".
            return ucfAttrName.getLocalPart();
        }

        // The namespace is different (e.g., icfs, or something else).
        throw new SchemaException("No mapping from QName " + ucfAttrName + " to ConnId attribute name");
    }

    /**
     * Maps ICF native objectclass name to a midPoint QName objectclass name.
     *
     * The mapping is "stateless" - it does not keep any mapping database or any
     * other state. There is a bi-directional mapping algorithm.
     *
     * TODO: mind the special characters in the ICF objectclass names.
     */
    static QName connIdObjectClassNameToUcf(ObjectClass icfObjectClass, boolean legacySchema) {
        if (icfObjectClass == null) {
            return null;
        }
        if (icfObjectClass.is(ObjectClass.ALL_NAME)) {
            return null;
        }
        if (legacySchema) {
            if (icfObjectClass.is(ObjectClass.ACCOUNT_NAME)) {
                return SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
            } else if (icfObjectClass.is(ObjectClass.GROUP_NAME)) {
                return SchemaConstants.RI_GROUP_OBJECT_CLASS;
            } else {
                return new QName(
                        MidPointConstants.NS_RI,
                        CUSTOM_OBJECTCLASS_PREFIX + icfObjectClass.getObjectClassValue() + CUSTOM_OBJECTCLASS_SUFFIX,
                        MidPointConstants.PREFIX_NS_RI);
            }
        } else {
            return new QName(
                    MidPointConstants.NS_RI,
                    icfObjectClass.getObjectClassValue(),
                    MidPointConstants.PREFIX_NS_RI);
        }
    }

    /** A convenience variant. */
    static QName connIdObjectClassNameToUcf(String connIdObjectClassName, boolean legacySchema) {
        return connIdObjectClassNameToUcf(new ObjectClass(connIdObjectClassName), legacySchema);
    }

    /**
     * Maps a midPoint object class QName to the ICF native objectclass name.
     */
    static @NotNull ObjectClass ucfObjectClassNameToConnId(
            ResourceObjectDefinition objectDefinition, boolean legacySchema) {
        return ucfObjectClassNameToConnId(
                objectDefinition.getTypeName(),
                legacySchema);
    }

    static @NotNull ObjectClass ucfObjectClassNameToConnId(QName ucfClassName, boolean legacySchema) {

        if (!MidPointConstants.NS_RI.equals(ucfClassName.getNamespaceURI())) {
            throw new IllegalArgumentException(
                    "ObjectClass QName %s is not in the appropriate namespace, expected: %s".formatted(
                            ucfClassName, MidPointConstants.NS_RI));
        }

        String localName = ucfClassName.getLocalPart();
        if (legacySchema) {
            if (SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME.equals(localName)) {
                return ObjectClass.ACCOUNT;
            } else if (SchemaConstants.GROUP_OBJECT_CLASS_LOCAL_NAME.equals(localName)) {
                return ObjectClass.GROUP;
            } else if (localName.startsWith(CUSTOM_OBJECTCLASS_PREFIX) && localName.endsWith(CUSTOM_OBJECTCLASS_SUFFIX)) {
                return new ObjectClass(
                        localName.substring(
                                CUSTOM_OBJECTCLASS_PREFIX.length(),
                                localName.length() - CUSTOM_OBJECTCLASS_SUFFIX.length()));
            } else {
                throw new IllegalArgumentException("Cannot recognize object class name " + ucfClassName);
            }
        } else {
            return new ObjectClass(localName);
        }
    }
}
