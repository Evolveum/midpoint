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

package com.evolveum.midpoint.provisioning.schema.util;

import com.evolveum.midpoint.provisioning.exceptions.ConversionException;
import com.evolveum.midpoint.provisioning.schema.AccountObjectClassDefinition;
import com.evolveum.midpoint.provisioning.schema.AttributeFlag;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import static com.evolveum.midpoint.provisioning.schema.util.SchemaDOMElement.*;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import static javax.xml.XMLConstants.*;

/**
 *
 * @author Vilo Repan
 */
public class SchemaToDOMParser {

    public static final String RESOURCE_OBJECT_CLASS = "ResourceObjectClass";
    private static final String MAX_OCCURS_UNBOUNDED = "unbounded";
    private Document document;
    private Map<String, String> defaultPrefixMap = new HashMap<String, String>();
    private Map<String, String> prefixMap = new HashMap<String, String>();
    private boolean attributeQualified = false;

    public SchemaToDOMParser() {
        this(null);
    }

    public SchemaToDOMParser(Map<String, String> defaultPrefixMap) {
        this.defaultPrefixMap = defaultPrefixMap;
    }

    public boolean isAttributeQualified() {
        return attributeQualified;
    }

    public void setAttributeQualified(boolean attributeQualified) {
        this.attributeQualified = attributeQualified;
    }

    private void init(ResourceSchema resSchema) {
        document = null;

        prefixMap.clear();
        if (defaultPrefixMap == null) {
            //some default namespaces, for better reading
            prefixMap.put(W3C_XML_SCHEMA_NS_URI, "xsd");
            prefixMap.put(SchemaConstants.NS_C, "c");
            prefixMap.put(SchemaConstants.NS_RESOURCE, "r");
        } else {
            prefixMap.putAll(defaultPrefixMap);
        }

        Set<String> imports = resSchema.getImportList();
        int i = 0;
        final String generatedPrefix = "xns";
        prefixMap.put(resSchema.getResourceNamespace(), generatedPrefix + i);

        for (String importNamespace : imports) {
            if (prefixMap.containsKey(importNamespace)) {
                continue;
            }
            i++;
            prefixMap.put(importNamespace, generatedPrefix + i);
        }
    }

    /*
     * I don't know if this mehtod will be needed...
     */
    public Document getDomSchemaHandling(ResourceSchema resSchema) {
        return null;
    }

    public Document getDomSchema(ResourceSchema resSchema) {
        init(resSchema);

        try {
            document = createSchemaDocument(resSchema.getResourceNamespace());
        } catch (ParserConfigurationException ex) {
            throw new ConversionException("Can't create document for XSD schema: " + ex.getMessage(), ex);
        }

        Element schema = document.getDocumentElement();
        Set<String> imports = resSchema.getImportList();
        for (String importNamespace : imports) {
            schema.appendChild(createImport(importNamespace));
        }

        if (!imports.contains(SchemaConstants.NS_C)) {
            schema.appendChild(createImport(SchemaConstants.NS_C));
        }
        if (!imports.contains(SchemaConstants.NS_RESOURCE)) {
            schema.appendChild(createImport(SchemaConstants.NS_RESOURCE));
        }


        for (Iterator<ResourceObjectDefinition> it = resSchema.getObjectClassesIterator(); it.hasNext();) {
            ResourceObjectDefinition objClass = it.next();
            schema.appendChild(createComplexType(objClass, resSchema.getResourceNamespace()));
        }

        Set<String> usedNamespaces = updatePrefixes(schema);
        addNamespaces(schema, usedNamespaces);

        return document;
    }

    private void addNamespaces(Element schema, Set<String> usedNamespaces) {
        Set<Entry<String, String>> set = prefixMap.entrySet();
        for (Entry<String, String> entry : set) {
            if (schema.hasAttribute("xmlns:" + entry.getValue()) || usedNamespaces.contains(entry.getKey())) {
                continue;
            }
            schema.setAttribute("xmlns:" + entry.getValue(), entry.getKey());
        }
    }

    private void setAttribute(Element element, String attrName, String attrValue) {
        setAttribute(element, new QName(W3C_XML_SCHEMA_NS_URI, attrName), attrValue);
    }

    private void setAttribute(Element element, QName attr, String attrValue) {
        if (attributeQualified) {
            element.setAttributeNS(attr.getNamespaceURI(), attr.getLocalPart(), attrValue);
        } else {
            element.setAttribute(attr.getLocalPart(), attrValue);
        }
    }

    private Element createComplexType(ResourceObjectDefinition objClass, String resourceNamespace) {
        Element complexType = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "complexType");
        setAttribute(complexType, "name", objClass.getQName().getLocalPart());
//        complexType.setAttribute("name", objClass.getQName().getLocalPart());

        Element annotation = createObjectClassAnnotation(objClass);
        if (annotation != null) {
            complexType.appendChild(annotation);
        }

        Element complexContent = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "complexContent");
        complexType.appendChild(complexContent);
        Element extension = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "extension");
//        extension.setAttribute("base", prefixMap.get(SchemaConstants.NS_RESOURCE) + ":" + RESOURCE_OBJECT_CLASS);
        setAttribute(extension, "base", prefixMap.get(SchemaConstants.NS_RESOURCE) + ":" + RESOURCE_OBJECT_CLASS);
        extension.setAttribute("xmlns:" + prefixMap.get(SchemaConstants.NS_RESOURCE), SchemaConstants.NS_RESOURCE);
        complexContent.appendChild(extension);
        Element sequence = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "sequence");
        extension.appendChild(sequence);

        Collection<ResourceAttributeDefinition> resAttributeList = objClass.getAttributesCopy();
        for (ResourceAttributeDefinition attribute : resAttributeList) {
            Element element = createElement(attribute, resourceNamespace);
            sequence.appendChild(element);
        }

        return complexType;
    }

    private String createPrefixedValue(QName name) {
        StringBuilder builder = new StringBuilder();
        String prefix = prefixMap.get(name.getNamespaceURI());
        if (prefix != null) {
            builder.append(prefix);
            builder.append(":");
        }
        builder.append(name.getLocalPart());

        return builder.toString();
    }

    private Element createObjectClassAnnotation(ResourceObjectDefinition objClass) {
        Element annotation = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "annotation");
        Element appinfo = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "appinfo");
        annotation.appendChild(appinfo);

        //displayName, identifier, secondaryIdentifier
        ResourceAttributeDefinition identifier = null;
        ResourceAttributeDefinition displayName = null;
        ResourceAttributeDefinition secondaryIdentifier = null;
        ResourceAttributeDefinition compositeIdentifier = null;
        ResourceAttributeDefinition descriptionAttribute = null;
        Collection<ResourceAttributeDefinition> resAttributeList = objClass.getAttributesCopy();
        for (ResourceAttributeDefinition attr : resAttributeList) {
            if (attr.isIdentifier()) {
                identifier = attr;
            }

            if (attr.isDisplayName()) {
                displayName = attr;
            }

            if (attr.isSecondaryIdentifier()) {
                secondaryIdentifier = attr;
            }

            if (attr.isCompositeIdentifier()) {
                compositeIdentifier = attr;
            }

            if (attr.isDescriptionAttribute()) {
                descriptionAttribute = attr;
            }

            if (identifier != null && displayName != null &&
                    secondaryIdentifier != null && compositeIdentifier != null &&
                    descriptionAttribute != null) {
                break;
            }
        }

        if (identifier != null) {
            appinfo.appendChild(createRefAnnotation(A_IDENTIFIER, createPrefixedValue(identifier.getQName())));
        }
        if (secondaryIdentifier != null) {
            appinfo.appendChild(createRefAnnotation(A_SECONDARY_IDENTIFIER, createPrefixedValue(secondaryIdentifier.getQName())));
        }
        if (compositeIdentifier != null) {
            appinfo.appendChild(createRefAnnotation(A_COMPOSITE_IDENTIFIER, createPrefixedValue(compositeIdentifier.getQName())));
        }
        if (displayName != null) {
            appinfo.appendChild(createRefAnnotation(A_DISPLAY_NAME, createPrefixedValue(displayName.getQName())));
        }
        if (descriptionAttribute != null) {
            appinfo.appendChild(createRefAnnotation(A_DESCRIPTION_ATTRIBUTE, createPrefixedValue(descriptionAttribute.getQName())));
        }
        //nativeObjectClass
        appinfo.appendChild(createAnnotation(A_NATIVE_OBJECT_CLASS, objClass.getNativeObjectClass()));

        //container
        if (objClass.isContainer()) {
            appinfo.appendChild(createAnnotation(A_CONTAINER, null));
        }

        //accountType
        if (objClass instanceof AccountObjectClassDefinition) {
            AccountObjectClassDefinition accObjClass = (AccountObjectClassDefinition) objClass;
            //TODO: how to save: this is account object class, and it's default maybe...
            Element accountTypeAnnotation = createAnnotation(A_ACCOUNT_TYPE, null);
            if (accObjClass.isDefault()) {
                setAttribute(accountTypeAnnotation, A_ATTR_DEFAULT, "true");
//                accountTypeAnnotation.setAttribute(A_ATTR_DEFAULT.getLocalPart(), "true");
            }
            appinfo.appendChild(accountTypeAnnotation);
        }

        return annotation;
    }

    private Element createRefAnnotation(QName qname, String value) {
        Element access = document.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
//        access.setAttribute("ref", value);
        setAttribute(access, new QName(SchemaConstants.NS_RESOURCE, "ref"), value);

        return access;
    }

    private Element createElement(ResourceAttributeDefinition attribute, String resourceNamespace) {
        Element element = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "element");

        String attrNamespace = attribute.getQName().getNamespaceURI();
        if (attrNamespace != null && attrNamespace.equals(resourceNamespace)) {
//            element.setAttribute("name", attribute.getQName().getLocalPart());
//            element.setAttribute("type", createPrefixedValue(attribute.getType()));
            setAttribute(element, "name", attribute.getQName().getLocalPart());
            setAttribute(element, "type", createPrefixedValue(attribute.getType()));
        } else {
//            element.setAttribute("ref", createPrefixedValue(attribute.getQName()));
            setAttribute(element, "ref", createPrefixedValue(attribute.getQName()));
        }

        if (attribute.getMinOccurs() != 1) {
//            element.setAttribute("minOccurs", Integer.toString(attribute.getMinOccurs()));
            setAttribute(element, "minOccurs", Integer.toString(attribute.getMinOccurs()));
        }

        if (attribute.getMaxOccurs() != 1) {
            String maxOccurs = attribute.getMaxOccurs() == ResourceAttributeDefinition.MAX_OCCURS_UNBOUNDED ? MAX_OCCURS_UNBOUNDED : Integer.toString(attribute.getMaxOccurs());
//            element.setAttribute("maxOccurs", maxOccurs);
            setAttribute(element, "maxOccurs", maxOccurs);
        }

        Element annotation = createAttributeAnnotation(attribute);
        if (annotation != null) {
            element.appendChild(annotation);
        }

        return element;
    }

    private Element createAttributeAnnotation(ResourceAttributeDefinition attribute) {
        boolean appinfoUsed = false;

        Element appinfo = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "appinfo");

        //flagList annotation
        StringBuilder builder = new StringBuilder();
        List<AttributeFlag> flags = attribute.getAttributeFlag();
        for (AttributeFlag flag : flags) {
            builder.append(flag);
            if (flags.indexOf(flag) + 1 != flags.size()) {
                builder.append(" ");
            }
        }
        if (builder.length() != 0) {
            appinfoUsed = true;
            appinfo.appendChild(createAnnotation(A_ATTRIBUTE_FLAG, builder.toString()));
        }

        ResourceAttributeDefinition.ClassifiedAttributeInfo classifiedInfo = attribute.getClassifiedAttributeInfo();
        if (attribute.isClassifiedAttribute() && classifiedInfo != null) {
            Element classifiedAttribute = document.createElementNS(SchemaDOMElement.A_CLASSIFIED_ATTRIBUTE.getNamespaceURI(),
                    SchemaDOMElement.A_CLASSIFIED_ATTRIBUTE.getLocalPart());
            appinfo.appendChild(classifiedAttribute);
            //encryption
            ResourceAttributeDefinition.Encryption encryption = classifiedInfo.getEncryption();
            if (encryption != null && encryption != ResourceAttributeDefinition.Encryption.NONE) {
                classifiedAttribute.appendChild(createAnnotation(A_CA_ENCRYPTION, encryption.toString()));
            }
            //classificationLevel
            String classificationLevel = classifiedInfo.getClassificationLevel();
            if (classificationLevel != null && !classificationLevel.isEmpty()) {
                classifiedAttribute.appendChild(createAnnotation(A_CA_CLASSIFICATION_LEVEL, classificationLevel));
            }
        }

        //attributeDisplayName
        if (attribute.getAttributeDisplayName() != null) {
            appinfo.appendChild(createAnnotation(A_ATTRIBUTE_DISPLAY_NAME, attribute.getAttributeDisplayName()));
            appinfoUsed = true;
        }

        //help
        if (attribute.getHelp() != null) {
            appinfo.appendChild(createAnnotation(A_HELP, attribute.getHelp()));
            appinfoUsed = true;
        }

        //nativeAttributeName
        if (attribute.getNativeAttributeName() != null) {
            appinfo.appendChild(createAnnotation(A_NATIVE_ATTRIBUTE_NAME, attribute.getNativeAttributeName()));
            appinfoUsed = true;
        }

        if (!appinfoUsed) {
            return null;
        }

        Element annotation = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "annotation");
        if (appinfoUsed) {
            annotation.appendChild(appinfo);
        }

        return annotation;
    }

    private Element createAnnotation(QName qname, String value) {
        Element annotation = document.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
        annotation.setTextContent(value);

        return annotation;
    }

    private Element createImport(String namespace) {
        Element element = document.createElementNS(W3C_XML_SCHEMA_NS_URI, "import");
//        element.setAttribute("namespace", namespace);
        setAttribute(element, "namespace", namespace);

        return element;
    }

    private Document createSchemaDocument(String targetNamespace) throws ParserConfigurationException {
        QName name = new QName(W3C_XML_SCHEMA_NS_URI, "schema");
        Document doc = createDocument(name);
        Element root = doc.getDocumentElement();
//        root.setAttribute("targetNamespace", targetNamespace);
//        root.setAttribute("elementFormDefault", "qualified");
        setAttribute(root, "targetNamespace", targetNamespace);
        setAttribute(root, "elementFormDefault", "qualified");
        if (attributeQualified) {
            setAttribute(root, "attributeFormDefault", "qualified");
        }

        return doc;
    }

    private Document createSchemaHandlingDocument() throws ParserConfigurationException {
        Document doc = createDocument(new QName(SchemaConstants.NS_C, "schemaHandling"));

        return doc;
    }

    private Document createDocument(QName name) throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.newDocument();
        Element root = doc.createElementNS(name.getNamespaceURI(), name.getLocalPart());
        doc.appendChild(root);

        return doc;
    }

    private Set<String> updatePrefixes(Node parent) {
        Set<String> usedNamespaces = new HashSet<String>();
        if (parent.getNamespaceURI() != null) {
            usedNamespaces.add(parent.getNamespaceURI());
            parent.setPrefix(prefixMap.get(parent.getNamespaceURI()));
        }

        if (parent.hasChildNodes()) {
            NodeList children = parent.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    usedNamespaces.addAll(updatePrefixes(child));
                }
            }
        }

        return usedNamespaces;
    }
}
