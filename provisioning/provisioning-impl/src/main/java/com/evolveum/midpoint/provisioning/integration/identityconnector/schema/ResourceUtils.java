/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.integration.identityconnector.schema;

import com.evolveum.midpoint.api.exceptions.MidPointException;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.integration.identityconnector.converter.ICFConverterFactory;
import com.evolveum.midpoint.provisioning.objects.ResourceAttribute;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.AttributeFlag;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassUtil;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.PredefinedAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import static javax.xml.XMLConstants.*;

/**
 * Utility class to make transformation between ConnectorObject and ResrourceObject.
 * 
 * @author Vilo Repan
 */
public class ResourceUtils {

    public static final String code_id = "$Id$";

    private static final Trace logger = TraceManager.getTrace(ResourceUtils.class);

    public static final String NS_ICF_RESOURCE = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd";

    public static final String NS_ICF_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/configuration-1.xsd";

    public static final QName ATTRIBUTE_NAME = new QName(NS_ICF_RESOURCE, Name.NAME);

    public static final QName ATTRIBUTE_UID = new QName(NS_ICF_RESOURCE, Uid.NAME);

    public static final QName ATTRIBUTE_PASSWORD = new QName(NS_ICF_RESOURCE, OperationalAttributes.PASSWORD_NAME);

    public static final String OBJECT_CLASS_ACCOUNT = "AccountObjectClass";

    public static final String OBJECT_CLASS_GROUP = "GroupObjectClass";

    public static final String OBJECT_CLASS_CUSTOM = "CustomObjectClass";

    private static final Map<Class, QName> types = new HashMap<Class, QName>();

    private Map<String, QName> attributeTypeConversion = new HashMap();

    private static ResourceUtils instance = new ResourceUtils();

    static {

        //TODO: move this to Resourte utils
        types.put(String.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        types.put(Long.class, new QName(W3C_XML_SCHEMA_NS_URI, "long"));
        types.put(long.class, new QName(W3C_XML_SCHEMA_NS_URI, "long"));
        types.put(char.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        types.put(Character.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        types.put(double.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        types.put(Double.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        types.put(float.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        types.put(Float.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        types.put(int.class, new QName(W3C_XML_SCHEMA_NS_URI, "int"));
        types.put(Integer.class, new QName(W3C_XML_SCHEMA_NS_URI, "int"));
        types.put(boolean.class, new QName(W3C_XML_SCHEMA_NS_URI, "boolean"));
        types.put(Boolean.class, new QName(W3C_XML_SCHEMA_NS_URI, "boolean"));
        types.put(byte[].class, new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary"));
        types.put(BigDecimal.class, new QName(W3C_XML_SCHEMA_NS_URI, "long"));
        types.put(BigInteger.class, new QName(W3C_XML_SCHEMA_NS_URI, "long"));
        types.put(GuardedByteArray.class, new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary"));
        types.put(GuardedString.class, new QName(SchemaConstants.NS_C, "PasswordType"));


    }

    private ResourceUtils() {
        attributeTypeConversion = new HashMap();
        attributeTypeConversion.put(Name.NAME, ATTRIBUTE_NAME);
        attributeTypeConversion.put(Uid.NAME, ATTRIBUTE_UID);
        for (String name : OperationalAttributes.getOperationalAttributeNames()) {
            attributeTypeConversion.put(name, new QName(NS_ICF_RESOURCE, name));
        }
        for (String name : new String[]{PredefinedAttributes.SHORT_NAME, PredefinedAttributes.DESCRIPTION, PredefinedAttributes.GROUPS_NAME, PredefinedAttributes.LAST_LOGIN_DATE_NAME, PredefinedAttributes.LAST_PASSWORD_CHANGE_DATE_NAME, PredefinedAttributes.PASSWORD_CHANGE_INTERVAL_NAME}) {
            attributeTypeConversion.put(name, new QName(NS_ICF_RESOURCE, name));
        }

    }

    public static ResourceUtils getInstance() {
        return instance;
    }

    public ObjectClass mapObjectClass(ResourceObjectDefinition def) {
        if (ObjectClassUtil.namesEqual(ObjectClass.ACCOUNT_NAME, def.getNativeObjectClass())) {
            return ObjectClass.ACCOUNT;
        } else if (ObjectClassUtil.namesEqual(ObjectClass.GROUP_NAME, def.getNativeObjectClass())) {
            return ObjectClass.GROUP;
        } else {
            return new ObjectClass(def.getNativeObjectClass());
        }
    }

    /**
     * Convert ResourceObject values to ConnectorObject attributes.
     */
    public Set<Attribute> convertAttributes(ResourceObject resourceObject, Set<Attribute> attributes) {
        for (ResourceAttribute attribute : resourceObject.getValues()) {
            addConnectorAttribute(attributes, attribute);
        }
        return attributes;
    }

    /**
     * Convert ResourceObject values to ConnectorObject attributes.
     *
     * @param resourceObject
     * @return
     * @todo 
     */
    @Deprecated
    public Set<Attribute> buildConnectorObject(ResourceObject resourceObject) {
        Set<Attribute> result = new HashSet<Attribute>(2);
        for (ResourceAttribute attribute : resourceObject.getValues()) {
            addConnectorAttribute(result, attribute);
        }
        return result;
    }

    protected void addConnectorAttribute(Set<Attribute> result, ResourceAttribute attr) {
        Attribute a = AttributeUtil.find(attr.getDefinition().getQName().getLocalPart(), result);
        if (null != a) {
            result.remove(a);
        }
        result.add(resourceAttributeToIcfAttribute(attr, a));
    }

    // TODO: This is ICF-specific. It should not be in the utils. Util should be kept generic.
    public Attribute resourceAttributeToIcfAttribute(ResourceAttribute attr, Attribute a) {
        if (ATTRIBUTE_UID.equals(attr.getDefinition().getQName())) {
            return new Uid(attr.getSingleJavaValue(String.class));
        } else if (ATTRIBUTE_NAME.equals(attr.getDefinition().getQName())) {
            return new Name(attr.getSingleJavaValue(String.class));
        } else if (ATTRIBUTE_PASSWORD.equals(attr.getDefinition().getQName())) {
            //todo create converter for GuardedString
            String password = attr.getSingleJavaValue(String.class);
            return AttributeBuilder.buildPassword(ICFConverterFactory.getInstance().getConverter(GuardedString.class, password).convert(password));
        } else {
            if (attr.getDefinition().isMultivalue()) {
                List<Object> value = new ArrayList<Object>();
                if (a != null) {
                    value.addAll(a.getValue());
                }
                value.addAll(attr.getJavaValues());
                return AttributeBuilder.build(attr.getDefinition().getQName().getLocalPart(), value);
            } else {
                //TODO QNAME-> attribute name mapping? NS validation??
                return AttributeBuilder.build(attr.getDefinition().getQName().getLocalPart(), attr.getJavaValues());
            }
        }
    }

    /**
     * Create ResourceObject from connectorObject.
     *
     * This method works with real resource objects, not object shadows.
     * Therefore objects are identified by the native identifiers, not OID.
     *
     * This method has no dependence on repository, but it depends on Identity
     * Connectors framework.
     *
     * This is NOT generated from WSDL.
     * It should be later moved to the adaptation layer.
     *
     * It returns only ResourceObjectShadowType now and no subclasses,
     * e.g. AccountShadowType. It should also return subclasses, but for that
     * it has to be aware of resource schema. It is not now.
     *
     * @param connector Resource XML (JAXB object) as defined by the identity shcema.
     * @param identification set of attributes that natively identify requested resource object on resource
     * @return detached Resource Object Shadow object. It has no OID and does not correspond to the repository.
     * @throws FaultMessage most likely error communicating with the resource (TODO: better definition)
     */
    public ResourceObject buildResourceObject(ConnectorObject connectorObject, ResourceObjectDefinition resourceObjectDefinition) throws MidPointException {
        ResourceObject result = new ResourceObject(resourceObjectDefinition);
        Set<String> specialAttribute = attributeTypeConversion.keySet();
        for (Attribute connectorAttribute : connectorObject.getAttributes()) {
            String attributeName = connectorAttribute.getName();
            if (specialAttribute.contains(attributeName)) {
                QName qname = attributeTypeConversion.get(attributeName);
                ResourceAttributeDefinition def = resourceObjectDefinition.getAttributeDefinition(qname);
                if (def != null) {
                    ResourceAttribute resourceAttr = new ResourceAttribute(def);
                    for (Object value : connectorAttribute.getValue()) {
                        resourceAttr.addJavaValue(value);
                    }
                    result.addValue(resourceAttr);
                }
            } else {
                QName qname = new QName(resourceObjectDefinition.getQName().getNamespaceURI(), attributeName);
                ResourceAttributeDefinition def = resourceObjectDefinition.getAttributeDefinition(qname);
                if (def != null) {
                    ResourceAttribute resourceAttr = new ResourceAttribute(def);
                    for (Object value : connectorAttribute.getValue()) {
                        resourceAttr.addJavaValue(value);
                    }
                    result.addValue(resourceAttr);
                }
            }
        }

        return result;
    }

    /**
     * Create ResourceSchema from ICF specific schema.
     * 
     * @param schema
     * @param resourceNamespace
     * @return
     */
    public ResourceSchema parse(Schema schema, String resourceNamespace) {
        ResourceSchema resSchema = new ResourceSchema(resourceNamespace);

        Set<ObjectClassInfo> ocInfo = schema.getObjectClassInfo();
        if (ocInfo == null) {
            return resSchema;
        }

        ResourceObjectDefinition objClass;
        QName qname;
        for (ObjectClassInfo info : ocInfo) {
            if (ObjectClass.ACCOUNT_NAME.equals(info.getType())) {
                qname = new QName(resourceNamespace, OBJECT_CLASS_ACCOUNT);
            } else if (ObjectClass.GROUP_NAME.equals(info.getType())) {
                qname = new QName(resourceNamespace, OBJECT_CLASS_GROUP);
            } else {
                qname = new QName(resourceNamespace, OBJECT_CLASS_CUSTOM);
            }
            objClass = new ResourceObjectDefinition(qname, info.getType());

            ResourceAttributeDefinition uid = new ResourceAttributeDefinition(new QName(NS_ICF_RESOURCE, "uid"));
            uid.setType(new QName(NS_ICF_RESOURCE, "uid"));
            uid.setIdentifier(true);
            objClass.addAttribute(uid);

            Set<AttributeInfo> attributeInfoSet = info.getAttributeInfo();
            if (attributeInfoSet != null) {
                ResourceAttributeDefinition attribute;
                for (AttributeInfo attributeInfo : attributeInfoSet) {
                    if (attributeInfo.getName() == null) {
                        continue;
                    }
                    attribute = createResourceAttribute(attributeInfo, resourceNamespace);
                    objClass.addAttribute(attribute);
                }
            }
            resSchema.addObjectClass(objClass);
        }

        return resSchema;
    }

    private ResourceAttributeDefinition createResourceAttribute(AttributeInfo info, String resourceNamespace) {
    	QName attributeQName = new QName(resourceNamespace, info.getName());
        ResourceAttributeDefinition attribute = new ResourceAttributeDefinition(attributeQName);

        if (ATTRIBUTE_PASSWORD.equals(attributeQName)) {
            attribute.setType(new QName(NS_ICF_RESOURCE, "password"));
        } else {
            attribute.setType(translateClassName(info.getType()));
        }

        attribute.setRestriction(null);
        attribute.setMinOccurs(0);

        Set<AttributeInfo.Flags> flagsSet = info.getFlags();
        List<AttributeFlag> flagList = new ArrayList<AttributeFlag>();
        for (AttributeInfo.Flags flags : flagsSet) {
            switch (flags) {
                case MULTIVALUED:
                    attribute.setMaxOccurs(ResourceAttributeDefinition.MAX_OCCURS_UNBOUNDED);
                    break;
                case NOT_CREATABLE:
                    flagList.add(AttributeFlag.NOT_CREATABLE);
                    break;
                case NOT_READABLE:
                    flagList.add(AttributeFlag.NOT_READABLE);
                    break;
                case NOT_RETURNED_BY_DEFAULT:
                    flagList.add(AttributeFlag.NOT_RETURNED_BY_DEFAULT);
                    break;
                case NOT_UPDATEABLE:
                    flagList.add(AttributeFlag.NOT_UPDATEABLE);
                    break;
                case REQUIRED:
                    attribute.setMinOccurs(1);
                    break;
            }
        }
        attribute.setAttributeFlag(flagList);

        return attribute;
    }

    public QName translateClassName(Class clazz) {
        return types.get(clazz);
    }
}
