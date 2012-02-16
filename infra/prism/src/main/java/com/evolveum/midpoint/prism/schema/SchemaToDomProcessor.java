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

package com.evolveum.midpoint.prism.schema;

import static com.evolveum.midpoint.prism.PrismConstants.*;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import com.sun.xml.xsom.XSParticle;

/**
 * Takes a midPoint Schema definition and produces a XSD schema (in a DOM form).
 * 
 * Great pains were taken to make sure that the output XML is "nice" and human readable.
 * E.g. the namespace prefixes are unified using the definitions in SchemaRegistry.
 * Please do not ruin this if you would update this class.
 * 
 * Single use class. Not thread safe. Create new instance for each run.
 * 
 * @author lazyman
 * @author Radovan Semancik
 */
class SchemaToDomProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaToDomProcessor.class);
	public static final String RESOURCE_OBJECT_CLASS = "ResourceObjectClass";
	private static final String MAX_OCCURS_UNBOUNDED = "unbounded";
	private boolean attributeQualified = false;
	private DynamicNamespacePrefixMapper namespacePrefixMapper;
	private PrismSchema schema;
	private Element rootXsdElement;
	private Set<String> importNamespaces;
	private Document document;

	SchemaToDomProcessor() {
		importNamespaces = new HashSet<String>();
	}

	void setAttributeQualified(boolean attributeQualified) {
		this.attributeQualified = attributeQualified;
	}

	public DynamicNamespacePrefixMapper getNamespacePrefixMapper() {
		return namespacePrefixMapper;
	}

	public void setNamespacePrefixMapper(DynamicNamespacePrefixMapper namespacePrefixMapper) {
		this.namespacePrefixMapper = namespacePrefixMapper;
	}

	/**
	 * Main entry point.
	 * 
	 * @param schema midPoint schema
	 * @return XSD schema in DOM form
	 * @throws SchemaException error parsing the midPoint schema or converting values
	 */
	Document parseSchema(PrismSchema schema) throws SchemaException {
		if (schema == null) {
			throw new IllegalArgumentException("Schema can't be null.");
		}
		this.schema = schema;

		try {
			
			init();
			
			Collection<Definition> definitions = schema.getDefinitions();
			for (Definition definition : definitions) {
				
				if (definition instanceof PrismContainerDefinition) {
					// Add property container definition. This will add <complexType> and <element> definitions to XSD
					addPropertyContainerDefinition((PrismContainerDefinition) definition,
							document.getDocumentElement());
					
				} else if (definition instanceof PrismPropertyDefinition) {
					// Add top-level property definition. It will create <element> XSD definition
					addPropertyDefinition((PrismPropertyDefinition) definition,
							document.getDocumentElement());
					
				} else if (definition instanceof ComplexTypeDefinition){
					// Ignore for now. Some the these will be processed inside
					// processing of PropertyContainerDefinition
					
				} else {
					throw new IllegalArgumentException("Encountered unsupported definition in schema: "
							+ definition);
				}
				
				// TODO: process unprocessed ComplexTypeDefinitions
			}

			// Add import definition. These were accumulated during previous processing.
			addImports();

		} catch (Exception ex) {
			throw new SchemaException("Couldn't parse schema, reason: " + ex.getMessage(), ex);
		}
		return document;
	}


	/**
	 * Adds XSD definitions from PropertyContainerDefinition. This is complexType and element.
	 * If the property container is an ResourceObjectDefinition, it will add only annotated
	 * complexType definition.
	 * 
	 * @param definition PropertyContainerDefinition to process
	 * @param parent element under which the XSD definition will be added
	 */
	private void addPropertyContainerDefinition(PrismContainerDefinition definition,
			Element parent) {
		
		ComplexTypeDefinition complexTypeDefinition = definition.getComplexTypeDefinition();
		Element complexType = addComplexTypeDefinition(complexTypeDefinition,parent);
		
		addElementDefinition(definition.getName(),definition.getTypeName(),parent);
	}

	/**
	 * Adds XSD element definition created from the midPoint PropertyDefinition.
	 * @param definition midPoint PropertyDefinition
	 * @param parent element under which the definition will be added
	 */
	private void addPropertyDefinition(PrismPropertyDefinition definition, Element parent) {
		Element property = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "element"));
		// Add to document first, so following methods will be able to resolve namespaces
		parent.appendChild(property);

		String attrNamespace = definition.getName().getNamespaceURI();
		if (attrNamespace != null && attrNamespace.equals(schema.getNamespace())) {
			setAttribute(property, "name", definition.getName().getLocalPart());
			setQNameAttribute(property, "type", definition.getTypeName());
		} else {
			setQNameAttribute(property, "ref", definition.getName());
		}

		if (definition.getMinOccurs() != 1) {
			setAttribute(property, "minOccurs", Integer.toString(definition.getMinOccurs()));
		}

		if (definition.getMaxOccurs() != 1) {
			String maxOccurs = definition.getMaxOccurs() == XSParticle.UNBOUNDED ? MAX_OCCURS_UNBOUNDED
					: Integer.toString(definition.getMaxOccurs());
			setAttribute(property, "maxOccurs", maxOccurs);
		}

		addPropertyAnnotation(definition, property);

		parent.appendChild(property);
	}
	
	/**
	 * Adds XSD element definition.
	 * @param name element QName
	 * @param typeName element type QName
	 * @param parent element under which the definition will be added
	 */
	private void addElementDefinition(QName name, QName typeName, Element parent) {
		// TODO Auto-generated method stub
		Document document = parent.getOwnerDocument();
		Element elementDef = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "element"));
		parent.appendChild(elementDef);
		// "typeName" should be used instead of "name" when defining a XSD type
		setAttribute(elementDef, "name", name.getLocalPart());
		setQNameAttribute(elementDef, "type", typeName);
	}

	/**
	 * Adds XSD complexType definition from the midPoint Schema ComplexTypeDefinion object
	 * @param definition midPoint Schema ComplexTypeDefinion object
	 * @param parent element under which the definition will be added
	 * @return created (and added) XSD complexType definition
	 */
	private Element addComplexTypeDefinition(ComplexTypeDefinition definition,
			Element parent) {
		Document document = parent.getOwnerDocument();
		Element complexType = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "complexType"));
		parent.appendChild(complexType);
		// "typeName" should be used instead of "name" when defining a XSD type
		setAttribute(complexType, "name", definition.getTypeName().getLocalPart());

		Element sequence = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "sequence"));
		complexType.appendChild(sequence);

		Set<ItemDefinition> definitions = definition.getDefinitions();
		for (ItemDefinition def : definitions) {
			if (def instanceof PrismPropertyDefinition) {
				addPropertyDefinition((PrismPropertyDefinition) def, sequence);
			} else if (def instanceof PrismContainerDefinition) {
				// TODO
				throw new UnsupportedOperationException("Inner propertyContainers are not supported yet");
			} else {
				throw new IllegalArgumentException("Uknown definition "+def+"("+def.getClass().getName()+") in complex type definition "+def);
			}
		}
		
		addComplexTypeAnnotation(definition,complexType);
		
		return complexType;
	}
	
	/**
	 * Adds XSD annotations to the XSD element definition. The annotations will be based on the provided PropertyDefinition.
	 * @param definition
	 * @param parent element under which the definition will be added (inserted as the first sub-element)
	 */
	private void addComplexTypeAnnotation(ComplexTypeDefinition definition, Element parent) {
		Element annotation = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
		parent.insertBefore(annotation, parent.getFirstChild());
		Element appinfo = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		annotation.appendChild(appinfo);

		// A top-level complex type is implicitly a property container now. It may change in the future
		
		// annotation: propertyContainer
		addAnnotation(A_PROPERTY_CONTAINER, definition.getDisplayName(), appinfo);
		
		if (!appinfo.hasChildNodes()) {
			// remove unneeded <annotation> element
			parent.removeChild(annotation);
		}

	}


//	/**
//	 * Adds XSD annotations to the XSD complexType defintion. The annotations will be based on the provided ResourceObjectDefinition.
//	 * @param parent element under which the definition will be added (inserted as the first sub-element)
//	 * @return created annotation XSD element
//	 */
//	private Element addResourceObjectAnnotations(ResourceObjectDefinition definition, Element parent) {
//		// The annotation element can already exist. If it does, use the existing one instead of creating it.
//		Element annotation = null;
//		Element appinfo = null;
//		NodeList annotations = parent.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "annotation");
//		if (annotations.getLength()>0) {
//			// TODO: more checks
//			annotation = (Element) annotations.item(0);
//			appinfo = (Element) annotation.getElementsByTagNameNS(W3C_XML_SCHEMA_NS_URI, "appinfo").item(0);
//		} else {
//			annotation = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
//			parent.insertBefore(annotation, parent.getFirstChild());
//			appinfo = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
//			annotation.appendChild(appinfo);
//		}
//
//		addAnnotation(A_RESOURCE_OBJECT, null, appinfo);
//		
//		// displayName, identifier, secondaryIdentifier
//		for (ResourceObjectAttributeDefinition identifier : definition.getIdentifiers()) {
//			addRefAnnotation(A_IDENTIFIER, identifier.getName(), appinfo);
//		}
//		for (ResourceObjectAttributeDefinition identifier : definition.getSecondaryIdentifiers()) {
//			addRefAnnotation(A_SECONDARY_IDENTIFIER,identifier.getName(),appinfo);
//		}
//		if (definition.getDisplayNameAttribute() != null) {
//			addRefAnnotation(A_DISPLAY_NAME, definition.getDisplayNameAttribute().getName(), appinfo);
//		}
//		if (definition.getDescriptionAttribute() != null) {
//			addRefAnnotation(A_DESCRIPTION_ATTRIBUTE, definition.getDescriptionAttribute().getName(), appinfo);
//		}
//		if (definition.getNamingAttribute() != null) {
//			addRefAnnotation(A_NAMING_ATTRIBUTE, definition.getNamingAttribute().getName(), appinfo);
//		}
//		// TODO: what to do with native object class, composite
//		// // nativeObjectClass
//		if (!StringUtils.isEmpty(definition.getNativeObjectClass())) {
//			addAnnotation(A_NATIVE_OBJECT_CLASS, definition.getNativeObjectClass(), appinfo);
//		}
//		
//		if (definition.isIgnored()) {
//			addAnnotation(A_IGNORE, "true", appinfo);
//		}
//
//		// container
//		// appinfo.appendChild(createAnnotation(A_CONTAINER, null, document));
//
//		// accountType
//		if (definition.isAccountType()) {
//			Element accountTypeAnnotation = addAnnotation(A_ACCOUNT_TYPE, null,appinfo);
//			if (definition.isDefaultAccountType()) {
//				setAttribute(accountTypeAnnotation, A_ATTR_DEFAULT, "true");
//			}
//		}
//
//		if (!appinfo.hasChildNodes()) {
//			// Remove unneeded empty annotation
//			parent.removeChild(annotation);
//		}
//
//		return annotation;
//	}

	/**
	 * Adds XSD annotations to the XSD element definition. The annotations will be based on the provided PropertyDefinition.
	 * @param definition
	 * @param parent element under which the definition will be added (inserted as the first sub-element)
	 */
	private void addPropertyAnnotation(PrismPropertyDefinition definition, Element parent) {
		Element annotation = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
		parent.insertBefore(annotation, parent.getFirstChild());
		Element appinfo = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		annotation.appendChild(appinfo);

		// attributeDisplayName
		if (!StringUtils.isEmpty(definition.getDisplayName())) {
			addAnnotation(A_DISPLAY_NAME, definition.getDisplayName(), appinfo);
		}

		// help
		if (!StringUtils.isEmpty(definition.getHelp())) {
			addAnnotation(A_HELP, definition.getHelp(),appinfo);
		}

//		if (definition instanceof ResourceObjectAttributeDefinition) {
//			ResourceObjectAttributeDefinition attrDefinition = (ResourceObjectAttributeDefinition) definition;
//			// nativeAttributeName
//			if (!StringUtils.isEmpty(attrDefinition.getNativeAttributeName())) {
//				addAnnotation(A_NATIVE_ATTRIBUTE_NAME, attrDefinition.getNativeAttributeName(), appinfo);
//			}
//		}
		
		if (definition.isIgnored()) {
			addAnnotation(A_IGNORE, "true", appinfo);
		}
		
		if (!definition.canCreate() || !definition.canRead() || !definition.canUpdate()) {
			// read-write-create attribute is the default. If any of this flags is missing, we must
			// add appropriate annotations.
			if (definition.canCreate()) {
				addAnnotation(A_ACCESS, A_ACCESS_CREATE, appinfo);
			}
			if (definition.canRead()) {
				addAnnotation(A_ACCESS, A_ACCESS_READ, appinfo);
			}
			if (definition.canUpdate()) {
				addAnnotation(A_ACCESS, A_ACCESS_UPDATE, appinfo);
			}
		}
		
		if (!appinfo.hasChildNodes()) {
			// remove unneeded <annotation> element
			parent.removeChild(annotation);
		}

	}

	/**
	 * Add generic annotation element.
	 * @param qname QName of the element
	 * @param value string value of the element
	 * @param parent element under which the definition will be added
	 * @return created XSD element
	 */
	private Element addAnnotation(QName qname, String value, Element parent) {
		Element annotation = createElement(qname);
		parent.appendChild(annotation);
		if (value != null) {
			annotation.setTextContent(value);
		}
		return annotation;
	}

	/**
	 * Adds annotation that points to another element (ususaly a property).
	 * @param qname QName of the element
	 * @param value Qname of the target element (property QName)
	 * @param parent parent element under which the definition will be added
	 * @return created XSD element
	 */
	private Element addRefAnnotation(QName qname, QName value, Element parent) {
		Element access = createElement(qname);
		parent.appendChild(access);
		setQNameAttribute(access, "ref", value);
		return access;
	}

	/**
	 * Create schema XSD DOM document.
	 */
	private void init() throws ParserConfigurationException {
		namespacePrefixMapper.registerPrefix(schema.getNamespace(), "tns");

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
		DocumentBuilder db = dbf.newDocumentBuilder();

		document = db.newDocument();
		Element root = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "schema"));
		document.appendChild(root);
		
		rootXsdElement = document.getDocumentElement();
		setAttribute(rootXsdElement, "targetNamespace", schema.getNamespace());
		setAttribute(rootXsdElement, "elementFormDefault", "qualified");
		if (attributeQualified) {
			setAttribute(rootXsdElement, "attributeFormDefault", "qualified");
		}
	}
	
	/**
	 * Create DOM document with a root element.
	 */
	private Document createDocument(QName name) throws ParserConfigurationException {

		return document;
	}

	/**
	 * Create XML element with the correct namespace prefix and namespace definition.
	 * @param qname element QName
	 * @return created DOM element
	 */
	private Element createElement(QName qname) {
		QName qnameWithPrefix = namespacePrefixMapper.setQNamePrefix(qname);
		addToImport(qname.getNamespaceURI());
		if (rootXsdElement!=null) {
			return DOMUtil.createElement(document, qnameWithPrefix, rootXsdElement, rootXsdElement);
		} else {
			// This is needed otherwise the root element itself could not be created
			return DOMUtil.createElement(document, qnameWithPrefix);
		}
	}

	/**
	 * Set attribute in the DOM element to a string value.
	 * @param element element where to set attribute
	 * @param attrName attribute name (String)
	 * @param attrValue attribute value (String)
	 */
	private void setAttribute(Element element, String attrName, String attrValue) {
		setAttribute(element, new QName(W3C_XML_SCHEMA_NS_URI, attrName), attrValue);
	}

	/**
	 * Set attribute in the DOM element to a string value.
	 * @param element element element where to set attribute
	 * @param attr attribute name (QName)
	 * @param attrValue attribute value (String)
	 */
	private void setAttribute(Element element, QName attr, String attrValue) {
		if (attributeQualified) {
			element.setAttributeNS(attr.getNamespaceURI(), attr.getLocalPart(), attrValue);
			addToImport(attr.getNamespaceURI());
		} else {
			element.setAttribute(attr.getLocalPart(), attrValue);
		}
	}
	
	/**
	 * Set attribute in the DOM element to a QName value. This will make sure that the
	 * appropriate namespace definition for the QName exists.
	 * 
	 * @param element element element element where to set attribute
	 * @param attrName attribute name (String)
	 * @param value attribute value (Qname)
	 */
	private void setQNameAttribute(Element element, String attrName, QName value) {
		QName valueWithPrefix = namespacePrefixMapper.setQNamePrefix(value);
		DOMUtil.setQNameAttribute(element, attrName, valueWithPrefix, rootXsdElement);
		addToImport(value.getNamespaceURI());
	}
	
	/**
	 * Make sure that the namespace will be added to import definitions.
	 * @param namespace namespace to import
	 */
	private void addToImport(String namespace) {
		if (!importNamespaces.contains(namespace)) {
			importNamespaces.add(namespace);
		}
	}

	/**
	 * Adds import definition to XSD.
	 * It adds imports of namespaces that accumulated during schema processing in the importNamespaces list.
	 * @param schema
	 */
	private void addImports() {
		for (String namespace : importNamespaces) {
			if (W3C_XML_SCHEMA_NS_URI.equals(namespace)) {
				continue;
			}

			if (schema.getNamespace().equals(namespace)) {
				//we don't want to import target namespace
				continue;
			}
			
			rootXsdElement.insertBefore(createImport(namespace), rootXsdElement.getFirstChild());
		}
	}
	
	/**
	 * Create single import XSD element.
	 */
	private Element createImport(String namespace) {
		Element element = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "import"));
		setAttribute(element, "namespace", namespace);
		return element;
	}


}
