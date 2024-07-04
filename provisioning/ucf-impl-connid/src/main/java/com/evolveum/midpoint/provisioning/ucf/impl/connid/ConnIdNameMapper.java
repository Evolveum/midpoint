/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;

import org.identityconnectors.framework.common.objects.ObjectClass;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.GROUP_OBJECT_CLASS_LOCAL_NAME;

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
     * @param frameworkNameResolver Provides "reasonable" attribute names for special `icfs:xyz` (framework) attribute names.
     */
    static ItemName connIdAttributeNameToUcf(
            @NotNull String connIdAttrName,
            @Nullable String nativeAttrName,
            @NotNull FrameworkNameResolver frameworkNameResolver) {

        if (SpecialItemNameMapper.isConnIdNameSpecial(connIdAttrName)) {
            if (nativeAttrName != null) {
                // native name is explicitly provided
                return itemNameInRi(nativeAttrName);
            }
            var resolved = frameworkNameResolver.resolveFrameworkName(connIdAttrName);
            if (resolved != null) {
                return resolved;
            } else {
                // not found -> use automatic translation (fallback, compatibility)
                return SpecialItemNameMapper.toUcfFormat(connIdAttrName);
            }
        } else {
            // This is not a special name, just use it as is.
            return itemNameInRi(connIdAttrName);
        }
    }

    static String ucfAttributeNameToConnId(PropertyDelta<?> attributeDelta, ResourceObjectDefinition ocDef) throws SchemaException {
        PrismPropertyDefinition<?> propDef = attributeDelta.getDefinition();
        ShadowSimpleAttributeDefinition<?> attrDef;
        if (propDef instanceof ShadowSimpleAttributeDefinition<?> rad) {
            attrDef = rad;
        } else {
            attrDef = ocDef.findSimpleAttributeDefinitionRequired(attributeDelta.getElementName());
        }
        return ucfAttributeNameToConnId(attrDef);
    }

    static String ucfAttributeNameToConnId(ReferenceDelta referenceDelta) throws SchemaException {
        return ucfAttributeNameToConnId(
                MiscUtil.castSafely(referenceDelta.getDefinition(), ShadowReferenceAttributeDefinition.class));
    }

    static String ucfAttributeNameToConnId(ShadowAttribute<?, ?, ?, ?> attribute, ResourceObjectDefinition ocDef)
            throws SchemaException {
        var attrDef = attribute.getDefinition();
        if (attrDef == null) {
            attrDef = ocDef.findAttributeDefinitionRequired(attribute.getElementName());
        }
        return ucfAttributeNameToConnId(attrDef);
    }

    public static String ucfAttributeNameToConnId(QName attributeName, ResourceObjectDefinition ocDef, String desc)
            throws SchemaException {
        return ucfAttributeNameToConnId(
                ocDef.findSimpleAttributeDefinitionRequired(attributeName, () -> " " + desc));
    }

    static String ucfAttributeNameToConnId(ShadowAttributeDefinition<?, ?, ?, ?> itemDef) throws SchemaException {
        if (itemDef.getFrameworkAttributeName() != null) {
            // This is the special name, as registered in the schema.
            return itemDef.getFrameworkAttributeName();
        }

        QName ucfAttrName = itemDef.getItemName();

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

    static QName connIdObjectClassNameToUcf(String connIdName, boolean legacySchema) {
        var local = connIdObjectClassNameToUcfLocal(connIdName, legacySchema);
        return local != null ? typeNameInRi(local) : null;
    }

    /** A convenience variant of the above. */
    static QName connIdObjectClassNameToUcf(@Nullable ObjectClass connIdObjectClass, boolean legacySchema) {
        return connIdObjectClass != null ?
                connIdObjectClassNameToUcf(connIdObjectClass.getObjectClassValue(), legacySchema) : null;
    }

    /**
     * Maps ICF native objectclass name to a midPoint QName objectclass name (local part of).
     *
     * The mapping is "stateless" - it does not keep any mapping database or any other state.
     * There is a bi-directional mapping algorithm.
     *
     * Note that ConnId class names are case-insensitive.
     */
    static String connIdObjectClassNameToUcfLocal(String connIdName, boolean legacySchema) {
        if (connIdName == null) {
            return null;
        }
        if (connIdName.equalsIgnoreCase(ObjectClass.ALL_NAME)) {
            return null;
        }
        if (legacySchema) {
            if (connIdName.equalsIgnoreCase(ObjectClass.ACCOUNT_NAME)) {
                return ACCOUNT_OBJECT_CLASS_LOCAL_NAME;
            } else if (connIdName.equalsIgnoreCase(ObjectClass.GROUP_NAME)) {
                return GROUP_OBJECT_CLASS_LOCAL_NAME;
            } else {
                return CUSTOM_OBJECTCLASS_PREFIX + connIdName + CUSTOM_OBJECTCLASS_SUFFIX;
            }
        } else {
            return connIdName;
        }
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
            if (ACCOUNT_OBJECT_CLASS_LOCAL_NAME.equals(localName)) {
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

    /** Name should NOT be in `+__NAME__+` format. */
    private static @NotNull ItemName itemNameInRi(@NotNull String name) {
        return new ItemName(MidPointConstants.NS_RI, QNameUtil.escapeElementName(name), MidPointConstants.PREFIX_NS_RI);
    }

    /** Used for type names. Most probably we don't need to escape special characters here. */
    public static @NotNull QName typeNameInRi(@NotNull String name) {
        return new QName(MidPointConstants.NS_RI, name, MidPointConstants.PREFIX_NS_RI);
    }
}
