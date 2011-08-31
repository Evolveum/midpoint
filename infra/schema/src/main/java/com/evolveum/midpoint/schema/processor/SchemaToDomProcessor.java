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
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_ACCOUNT_TYPE;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_ATTRIBUTE_DISPLAY_NAME;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_ATTR_DEFAULT;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_DESCRIPTION_ATTRIBUTE;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_DISPLAY_NAME;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_HELP;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_IDENTIFIER;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_NAMING_ATTRIBUTE;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_NATIVE_ATTRIBUTE_NAME;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_NATIVE_OBJECT_CLASS;
import static com.evolveum.midpoint.schema.processor.ProcessorConstants.A_SECONDARY_IDENTIFIER;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.sun.xml.xsom.XSParticle;

/**
 * @author lazyman
 */
class SchemaToDomProcessor {

	private static final Trace TRACE = TraceManager.getTrace(SchemaToDomProcessor.class);
	public static final String RESOURCE_OBJECT_CLASS = "ResourceObjectClass";
	private static final String MAX_OCCURS_UNBOUNDED = "unbounded";
	private Map<String, String> prefixMap = null;
	private boolean attributeQualified = false;

	SchemaToDomProcessor() {
		this(null);
	}

	SchemaToDomProcessor(Map<String, String> prefixMap) {
		this.prefixMap = prefixMap;
	}

	void setAttributeQualified(boolean attributeQualified) {
		this.attributeQualified = attributeQualified;
	}

	Document parseSchema(Schema schema) throws SchemaProcessorException {
		if (schema == null) {
			throw new IllegalArgumentException("Schema can't be null.");
		}

		Document document = null;
		try {
			document = init(schema);
			Set<Definition> definitions = schema.getDefinitions();
			for (Definition definition : definitions) {
				if (definition instanceof PropertyContainerDefinition) {
					addPropertyContainerDefinition(schema, (PropertyContainerDefinition) definition,
							document.getDocumentElement());
				} else if (definition instanceof PropertyDefinition) {
					addPropertyDefinition(schema, (PropertyDefinition) definition,
							document.getDocumentElement());
				} else {
					throw new IllegalArgumentException("Encountered unsupported definition in schema: "
							+ definition);
				}
			}

			Set<Entry<String, String>> set = prefixMap.entrySet();
			for (Entry<String, String> entry : set) {
				document.getDocumentElement().setAttribute("xmlns:" + entry.getValue(), entry.getKey());
			}
		} catch (Exception ex) {
			throw new SchemaProcessorException("Couldn't parse schema, reason: " + ex.getMessage(), ex);
		}
		return document;
	}

	private void addPropertyContainerDefinition(Schema schema, PropertyContainerDefinition definition,
			Element parent) {
		Document document = parent.getOwnerDocument();
		Element container = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI, "complexType"));
		// "typeName" should be used instead of "name" when defining a XSD type
		setAttribute(container, "name", definition.getTypeName().getLocalPart());

		Element definitionHomeElement = container;
		if (definition instanceof ResourceObjectDefinition) {
			Element annotation = createResourceObjectAnnotations((ResourceObjectDefinition) definition,
					document);
			if (annotation != null) {
				container.appendChild(annotation);
			}
			Element complexContent = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI,
					"complexContent"));
			container.appendChild(complexContent);
			Element extension = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI, "extension"));
			setAttribute(extension, "base", createPrefixedValue(new QName(SchemaConstants.NS_RESOURCE,
					RESOURCE_OBJECT_CLASS)));
			complexContent.appendChild(extension);
			definitionHomeElement = extension;
		}

		definitionHomeElement.setAttribute("xmlns:" + prefixMap.get(SchemaConstants.NS_RESOURCE),
				SchemaConstants.NS_RESOURCE);
		Element sequence = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI, "sequence"));
		definitionHomeElement.appendChild(sequence);

		Set<PropertyDefinition> definitions = definition.getDefinitions();
		for (PropertyDefinition propertyDefinition : definitions) {
			addPropertyDefinition(schema, propertyDefinition, sequence);
		}

		parent.appendChild(container);
	}

	private void addPropertyDefinition(Schema schema, PropertyDefinition definition, Element parent) {
		Element property = createElementNS(parent.getOwnerDocument(), new QName(W3C_XML_SCHEMA_NS_URI,
				"element"));

		String attrNamespace = definition.getName().getNamespaceURI();
		if (attrNamespace != null && attrNamespace.equals(schema.getNamespace())) {
			setAttribute(property, "name", definition.getName().getLocalPart());
			setAttribute(property, "type", createPrefixedValue(definition.getTypeName()));
		} else {
			setAttribute(property, "ref", createPrefixedValue(definition.getName()));
		}

		if (definition.getMinOccurs() != 1) {
			setAttribute(property, "minOccurs", Integer.toString(definition.getMinOccurs()));
		}

		if (definition.getMaxOccurs() != 1) {
			String maxOccurs = definition.getMaxOccurs() == XSParticle.UNBOUNDED ? MAX_OCCURS_UNBOUNDED
					: Integer.toString(definition.getMaxOccurs());
			setAttribute(property, "maxOccurs", maxOccurs);
		}

		Element annotation = createPropertyAnnotation(definition, parent.getOwnerDocument());
		if (annotation != null) {
			property.appendChild(annotation);
		}

		parent.appendChild(property);
	}

	private Element createResourceObjectAnnotations(ResourceObjectDefinition definition, Document document) {
		Element annotation = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
		Element appinfo = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		annotation.appendChild(appinfo);

		// displayName, identifier, secondaryIdentifier
		for (ResourceObjectAttributeDefinition identifier : definition.getIdentifiers()) {
			appinfo.appendChild(createRefAnnotation(A_IDENTIFIER, createPrefixedValue(identifier.getName()),
					document));
		}
		for (ResourceObjectAttributeDefinition identifier : definition.getSecondaryIdentifiers()) {
			appinfo.appendChild(createRefAnnotation(A_SECONDARY_IDENTIFIER,
					createPrefixedValue(identifier.getName()), document));
		}
		if (definition.getDisplayNameAttribute() != null) {
			appinfo.appendChild(createRefAnnotation(A_DISPLAY_NAME, createPrefixedValue(definition
					.getDisplayNameAttribute().getName()), document));
		}
		if (definition.getDescriptionAttribute() != null) {
			appinfo.appendChild(createRefAnnotation(A_DESCRIPTION_ATTRIBUTE, createPrefixedValue(definition
					.getDescriptionAttribute().getName()), document));
		}
		if (definition.getNamingAttribute() != null) {
			appinfo.appendChild(createRefAnnotation(A_NAMING_ATTRIBUTE, createPrefixedValue(definition
					.getNamingAttribute().getName()), document));
		}
		// TODO: what to do with native object class, composite
		// // nativeObjectClass
		if (!StringUtils.isEmpty(definition.getNativeObjectClass())) {
			appinfo.appendChild(createAnnotation(A_NATIVE_OBJECT_CLASS, definition.getNativeObjectClass(),
					document));
		}

		// container
		// appinfo.appendChild(createAnnotation(A_CONTAINER, null, document));

		// accountType
		if (definition.isAccountType()) {
			Element accountTypeAnnotation = createAnnotation(A_ACCOUNT_TYPE, null, document);
			if (definition.isDefaultAccountType()) {
				setAttribute(accountTypeAnnotation, A_ATTR_DEFAULT, "true");
			}
			appinfo.appendChild(accountTypeAnnotation);
		}

		if (!appinfo.hasChildNodes()) {
			return null;
		}

		return annotation;
	}

	private Element createPropertyAnnotation(PropertyDefinition definition, Document document) {
		Element appinfo = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));

		// flagList annotation
		// StringBuilder builder = new StringBuilder();
		// List<AttributeFlag> flags = attribute.getAttributeFlag();
		// for (AttributeFlag flag : flags) {
		// builder.append(flag);
		// if (flags.indexOf(flag) + 1 != flags.size()) {
		// builder.append(" ");
		// }
		// }
		// if (builder.length() != 0) {
		// appinfoUsed = true;
		// appinfo.appendChild(createAnnotation(A_ATTRIBUTE_FLAG,
		// builder.toString()));
		// }

		// ResourceAttributeDefinition.ClassifiedAttributeInfo classifiedInfo =
		// attribute.getClassifiedAttributeInfo();
		// if (attribute.isClassifiedAttribute() && classifiedInfo != null) {
		// Element classifiedAttribute =
		// document.createElementNS(SchemaDOMElement.A_CLASSIFIED_ATTRIBUTE.getNamespaceURI(),
		// SchemaDOMElement.A_CLASSIFIED_ATTRIBUTE.getLocalPart());
		// appinfo.appendChild(classifiedAttribute);
		// //encryption
		// ResourceAttributeDefinition.Encryption encryption =
		// classifiedInfo.getEncryption();
		// if (encryption != null && encryption !=
		// ResourceAttributeDefinition.Encryption.NONE) {
		// classifiedAttribute.appendChild(createAnnotation(A_CA_ENCRYPTION,
		// encryption.toString()));
		// }
		// //classificationLevel
		// String classificationLevel = classifiedInfo.getClassificationLevel();
		// if (classificationLevel != null && !classificationLevel.isEmpty()) {
		// classifiedAttribute.appendChild(createAnnotation(A_CA_CLASSIFICATION_LEVEL,
		// classificationLevel));
		// }
		// }

		// attributeDisplayName
		if (!StringUtils.isEmpty(definition.getDisplayName())) {
			appinfo.appendChild(createAnnotation(A_ATTRIBUTE_DISPLAY_NAME, definition.getDisplayName(),
					document));
		}

		// help
		if (!StringUtils.isEmpty(definition.getHelp())) {
			appinfo.appendChild(createAnnotation(A_HELP, definition.getHelp(), document));
		}

		if (definition instanceof ResourceObjectAttributeDefinition) {
			ResourceObjectAttributeDefinition attrDefinition = (ResourceObjectAttributeDefinition) definition;
			// nativeAttributeName
			if (!StringUtils.isEmpty(attrDefinition.getNativeAttributeName())) {
				appinfo.appendChild(createAnnotation(A_NATIVE_ATTRIBUTE_NAME,
						attrDefinition.getNativeAttributeName(), document));
			}
		}

		Element annotation = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
		if (appinfo.hasChildNodes()) {
			annotation.appendChild(appinfo);
		} else {
			return null;
		}

		return annotation;
	}

	private Element createAnnotation(QName qname, String value, Document document) {
		Element annotation = createElementNS(document, qname);
		annotation.setTextContent(value);

		return annotation;
	}

	private Element createRefAnnotation(QName qname, String value, Document document) {
		Element access = createElementNS(document, qname);
		setAttribute(access, new QName(SchemaConstants.NS_RESOURCE, "ref"), value);

		return access;
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

	private Document init(Schema schema) throws ParserConfigurationException {
		if (prefixMap == null) {
			prefixMap = new HashMap<String, String>();
		}
		if (!prefixMap.containsKey(W3C_XML_SCHEMA_NS_URI)) {
			prefixMap.put(W3C_XML_SCHEMA_NS_URI, "xsd");
		}
		if (!prefixMap.containsKey(SchemaConstants.NS_C)) {
			prefixMap.put(SchemaConstants.NS_C, "c");
			// document.getDocumentElement().appendChild(createImport(document,
			// SchemaConstants.NS_C));
		}
		if (!prefixMap.containsKey(SchemaConstants.NS_RESOURCE)) {
			prefixMap.put(SchemaConstants.NS_RESOURCE, "r");
		}
		// TODO: This is wrong. The dependency should be inverted (MID-356)
		if (!prefixMap.containsKey(SchemaConstants.NS_ICF_SCHEMA)) {
			prefixMap.put(SchemaConstants.NS_ICF_SCHEMA, "icfs");
		}
		// TODO: This is wrong. The dependency should be inverted (MID-356)
		if (!prefixMap.containsKey(SchemaConstants.NS_ICF_CONFIGURATION)) {
			prefixMap.put(SchemaConstants.NS_ICF_CONFIGURATION, "icfc");
		}

		prefixMap.put(schema.getNamespace(), "tns");

		int index = 0;
		for (Definition definition : schema.getDefinitions()) {
			index += updatePrefixMapFromDefinition(definition, index);
		}

		Document document = createSchemaDocument(schema.getNamespace());
		for (Entry<String, String> entry : prefixMap.entrySet()) {
			if (W3C_XML_SCHEMA_NS_URI.equals(entry.getKey())) {
				continue;
			}

			Element root = document.getDocumentElement();
			root.insertBefore(createImport(document, entry.getKey()), root.getFirstChild());
		}

		return document;
	}

	private int updatePrefixMapFromDefinition(Definition definition, int index) {
		// Add appropriate namespace if a definition is in different namespace
		// e.g. <element ref="foo:bar">
		String namespace = definition.getName().getNamespaceURI();
		final String generatedPrefix = "vr";
		if (!prefixMap.containsKey(namespace)) {
			prefixMap.put(namespace, generatedPrefix + index);
			index++;
		}

		// Add appropriate namespace if the type of the definition is in a
		// different namespace e.g. <element type="foo:BarType">
		String typeNamespace = definition.getTypeName().getNamespaceURI();
		if (!prefixMap.containsKey(typeNamespace)) {
			prefixMap.put(typeNamespace, generatedPrefix + index);
			index++;
		}

		if (definition instanceof ResourceObjectDefinition) {
			// We need to add the "r" namespace. This is not in the definitions
			// but it in supertype definition
			// therefore it will not be discovered
			// addImportIfNotYetAdded(document, SchemaConstants.NS_RESOURCE,
			// alreadyImportedNamespaces);
		}

		if (definition instanceof PropertyContainerDefinition) {
			PropertyContainerDefinition container = (PropertyContainerDefinition) definition;
			Set<PropertyDefinition> definitions = container.getDefinitions();
			for (PropertyDefinition property : definitions) {
				index += updatePrefixMapFromDefinition(property, index);
			}
		}

		return index;
	}

	private Element createImport(Document document, String namespace) {
		Element element = createElementNS(document, new QName(W3C_XML_SCHEMA_NS_URI, "import"));
		setAttribute(element, "namespace", namespace);

		return element;
	}

	private Document createSchemaDocument(String targetNamespace) throws ParserConfigurationException {
		Document doc = createDocument(new QName(W3C_XML_SCHEMA_NS_URI, "schema"));
		Element root = doc.getDocumentElement();
		setAttribute(root, "targetNamespace", targetNamespace);
		setAttribute(root, "elementFormDefault", "qualified");
		if (attributeQualified) {
			setAttribute(root, "attributeFormDefault", "qualified");
		}

		return doc;
	}

	private Document createDocument(QName name) throws ParserConfigurationException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
		DocumentBuilder db = dbf.newDocumentBuilder();

		Document document = db.newDocument();
		Element root = createElementNS(document, name);
		document.appendChild(root);

		return document;
	}

	private Element createElementNS(Document document, QName qname) {
		Element element = document.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
		element.setPrefix(prefixMap.get(qname.getNamespaceURI()));

		return element;
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
}
