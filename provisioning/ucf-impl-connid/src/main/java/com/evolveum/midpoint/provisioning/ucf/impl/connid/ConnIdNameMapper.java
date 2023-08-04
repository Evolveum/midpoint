/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.connid;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.QNameUtil;
import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.Uid;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConnIdNameMapper {

    private static final String CUSTOM_OBJECTCLASS_PREFIX = "Custom";
    private static final String CUSTOM_OBJECTCLASS_SUFFIX = "ObjectClass";

    private static final Map<String,QName> SPECIAL_ATTRIBUTE_MAP_ICF = new HashMap<>();
    private static final Map<QName,String> SPECIAL_ATTRIBUTE_MAP_MP = new HashMap<>();

    private ResourceSchema resourceSchema = null;

    public ConnIdNameMapper() {
        super();
    }

    public ResourceSchema getResourceSchema() {
        return resourceSchema;
    }

    public void setResourceSchema(ResourceSchema resourceSchema) {
        this.resourceSchema = resourceSchema;
    }

    private static void initialize() {
        addSpecialAttributeMapping(Name.NAME, SchemaConstants.ICFS_NAME);
        addSpecialAttributeMapping(Uid.NAME, SchemaConstants.ICFS_UID);

        addOperationalAttributeMapping(OperationalAttributeInfos.CURRENT_PASSWORD);
        addOperationalAttributeMapping(OperationalAttributeInfos.DISABLE_DATE);
        addOperationalAttributeMapping(OperationalAttributeInfos.ENABLE);
        addOperationalAttributeMapping(OperationalAttributeInfos.ENABLE_DATE);
        addOperationalAttributeMapping(OperationalAttributeInfos.LOCK_OUT);
        addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD);
        addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD_EXPIRATION_DATE);
        addOperationalAttributeMapping(OperationalAttributeInfos.PASSWORD_EXPIRED);

        addOperationalAttributeMapping(SecretIcfOperationalAttributes.DESCRIPTION);
        addOperationalAttributeMapping(SecretIcfOperationalAttributes.GROUPS);
        addOperationalAttributeMapping(SecretIcfOperationalAttributes.LAST_LOGIN_DATE);
    }

    private static void addSpecialAttributeMapping(String icfName, QName qname) {
        SPECIAL_ATTRIBUTE_MAP_ICF.put(icfName, qname);
        SPECIAL_ATTRIBUTE_MAP_MP.put(qname, icfName);
    }

    private static void addOperationalAttributeMapping(
            SecretIcfOperationalAttributes opAttr) {
        addOperationalAttributeMapping(opAttr.getName());
    }

    private static void addOperationalAttributeMapping(AttributeInfo attrInfo) {
        addOperationalAttributeMapping(attrInfo.getName());
    }

    private static void addOperationalAttributeMapping(String icfName) {
        QName qName = convertUnderscoreAttributeNameToQName(icfName);
        addSpecialAttributeMapping(icfName, qName);
    }


    public QName convertAttributeNameToQName(String icfAttrName, ResourceAttributeContainerDefinition attributesContainerDefinition) {
        return convertAttributeNameToQName(
                icfAttrName, attributesContainerDefinition.getComplexTypeDefinition());
    }

    public QName convertAttributeNameToQName(String icfAttrName, ResourceObjectDefinition ocDef) {
        if (SPECIAL_ATTRIBUTE_MAP_ICF.containsKey(icfAttrName)) {
            for (ResourceAttributeDefinition<?> attributeDefinition: ocDef.getAttributeDefinitions()) {
                if (icfAttrName.equals(attributeDefinition.getFrameworkAttributeName())) {
                    return attributeDefinition.getItemName();
                }
            }
            // fallback, compatibility
            return SPECIAL_ATTRIBUTE_MAP_ICF.get(icfAttrName);
        }
        return new QName(MidPointConstants.NS_RI, QNameUtil.escapeElementName(icfAttrName), MidPointConstants.PREFIX_NS_RI);
    }

    public String convertAttributeNameToConnId(PropertyDelta<?> attributeDelta, ResourceObjectDefinition ocDef)
            throws SchemaException {
        PrismPropertyDefinition<?> propDef = attributeDelta.getDefinition();
        ResourceAttributeDefinition<?> attrDef;
        if (propDef instanceof ResourceAttributeDefinition) {
            attrDef = (ResourceAttributeDefinition<?>) propDef;
        } else {
            attrDef = ocDef.findAttributeDefinition(attributeDelta.getElementName());
            if (attrDef == null) {
                throw new SchemaException("No attribute "+attributeDelta.getElementName()+" in object class "+ocDef.getTypeName());
            }
        }
        return convertAttributeNameToConnId(attrDef);
    }

    public String convertAttributeNameToConnId(ResourceAttribute<?> attribute, ResourceObjectDefinition ocDef)
                throws SchemaException {
            ResourceAttributeDefinition<?> attrDef = attribute.getDefinition();
            if (attrDef == null) {
                attrDef = ocDef.findAttributeDefinition(attribute.getElementName());
                if (attrDef == null) {
                    throw new SchemaException("No attribute "+attribute.getElementName()+" in object class "+ocDef.getTypeName());
                }
            }
            return convertAttributeNameToConnId(attrDef);
        }

    public String convertAttributeNameToConnId(QName attributeName, ResourceObjectDefinition ocDef, String desc)
            throws SchemaException {
        ResourceAttributeDefinition<?> attrDef = ocDef.findAttributeDefinition(attributeName);
        if (attrDef == null) {
            throw new SchemaException("No attribute " + attributeName + " in " + ocDef + " " + desc);
        }
        return convertAttributeNameToConnId(attrDef);
    }

    public String convertAttributeNameToConnId(ResourceAttributeDefinition<?> attrDef)
            throws SchemaException {
        if (attrDef.getFrameworkAttributeName() != null) {
            return attrDef.getFrameworkAttributeName();
        }

        QName attrQName = attrDef.getItemName();
        if (SPECIAL_ATTRIBUTE_MAP_MP.containsKey(attrQName)) {
            return SPECIAL_ATTRIBUTE_MAP_MP.get(attrQName);
        }

        if (!attrQName.getNamespaceURI().equals(MidPointConstants.NS_RI)) {
            throw new SchemaException("No mapping from QName " + attrQName + " to an ICF attribute in resource schema namespace: " + MidPointConstants.NS_RI);
        }

        return attrQName.getLocalPart();

    }

    private boolean isUnderscoreSyntax(String icfAttrName) {
        return icfAttrName.startsWith("__") && icfAttrName.endsWith("__");
    }

    private static QName convertUnderscoreAttributeNameToQName(String icfAttrName) {
        // Strip leading and trailing underscores
        String inside = icfAttrName.substring(2, icfAttrName.length()-2);

        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (true) {
            int nextIndex = inside.indexOf("_", lastIndex);
            if (nextIndex < 0) {
                String upcase = inside.substring(lastIndex);
                sb.append(toCamelCase(upcase, lastIndex == 0));
                break;
            }
            String upcase = inside.substring(lastIndex, nextIndex);
            sb.append(toCamelCase(upcase, lastIndex == 0));
            lastIndex = nextIndex + 1;
        }

        return new QName(SchemaConstants.NS_ICF_SCHEMA, sb.toString());
    }

    private static String toCamelCase(String upcase, boolean lowCase) {
        if (lowCase) {
            return StringUtils.lowerCase(upcase);
        } else {
            return StringUtils.capitalize(StringUtils.lowerCase(upcase));
        }
    }

    /**
     * Maps ICF native objectclass name to a midPoint QName objectclass name.
     * <p/>
     * The mapping is "stateless" - it does not keep any mapping database or any
     * other state. There is a bi-directional mapping algorithm.
     * <p/>
     * TODO: mind the special characters in the ICF objectclass names.
     */
    QName objectClassToQname(ObjectClass icfObjectClass, boolean legacySchema) {
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

    @Nullable
    ObjectClass objectClassToConnId(PrismObject<? extends ShadowType> shadow,
            ConnectorType connectorBean, boolean legacySchema) {

        ShadowType shadowBean = shadow.asObjectable();
        QName objectClassName = shadowBean.getObjectClass();
        if (objectClassName == null) {
            ResourceAttributeContainer attrContainer = ShadowUtil.getAttributesContainer(shadowBean);
            if (attrContainer == null) {
                return null;
            }
            ResourceAttributeContainerDefinition objectClassDefinition = attrContainer.getDefinition();
            objectClassName = objectClassDefinition.getTypeName();
        }

        return objectClassToConnId(objectClassName, connectorBean, legacySchema);
    }

    /**
     * Maps a midPoint object class QName to the ICF native objectclass name.
     *
     * The mapping is "stateless" - it does not keep any mapping database or any
     * other state. There is a bi-directional mapping algorithm.
     *
     * TODO: mind the special characters in the ICF objectclass names.
     */
    @NotNull
    ObjectClass objectClassToConnId(
            ResourceObjectDefinition objectDefinition, ConnectorType connectorBean, boolean legacySchema) {
        return objectClassToConnId(
                objectDefinition.getTypeName(),
                connectorBean,
                legacySchema);
    }

    @NotNull
    ObjectClass objectClassToConnId(QName qnameObjectClass, ConnectorType connectorType, boolean legacySchema) {

        if (!MidPointConstants.NS_RI.equals(qnameObjectClass.getNamespaceURI())) {
            throw new IllegalArgumentException("ObjectClass QName " + qnameObjectClass
                    + " is not in the appropriate namespace for "
                    + connectorType + ", expected: " + MidPointConstants.NS_RI);
        }

        String localName = qnameObjectClass.getLocalPart();
        if (legacySchema) {
            if (SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME.equals(localName)) {
                return ObjectClass.ACCOUNT;
            } else if (SchemaConstants.GROUP_OBJECT_CLASS_LOCAL_NAME.equals(localName)) {
                return ObjectClass.GROUP;
            } else if (localName.startsWith(CUSTOM_OBJECTCLASS_PREFIX) && localName.endsWith(CUSTOM_OBJECTCLASS_SUFFIX)) {
                String icfObjectClassName = localName.substring(CUSTOM_OBJECTCLASS_PREFIX.length(), localName.length()
                        - CUSTOM_OBJECTCLASS_SUFFIX.length());
                return new ObjectClass(icfObjectClassName);
            } else {
                throw new IllegalArgumentException("Cannot recognize objectclass QName " + qnameObjectClass
                        + " for " + ObjectTypeUtil.toShortString(connectorType) + ", expected: "
                        + MidPointConstants.NS_RI);
            }
        } else {
            return new ObjectClass(localName);
        }
    }

    static {
        initialize();
    }

}
