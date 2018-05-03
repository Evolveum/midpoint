/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.schema;

import static com.evolveum.midpoint.prism.PrismConstants.*;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.evolveum.midpoint.prism.*;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
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
public class SchemaToDomProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(SchemaToDomProcessor.class);
	private static final String MAX_OCCURS_UNBOUNDED = "unbounded";
	private boolean attributeQualified = false;
	private PrismContext prismContext;
	private DynamicNamespacePrefixMapper namespacePrefixMapper;
	private PrismSchema schema;
	private Element rootXsdElement;
	private Set<String> importNamespaces;
	private Document document;

	SchemaToDomProcessor() {
		importNamespaces = new HashSet<>();
	}

	public PrismContext getPrismContext() {
		return prismContext;
	}

	public void setPrismContext(PrismContext prismContext) {
		this.prismContext = prismContext;
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

	private SchemaDefinitionFactory getDefinitionFactory() {
		return ((PrismContextImpl) prismContext).getDefinitionFactory();
	}

	private String getNamespace() {
		return schema.getNamespace();
	}

	private boolean isMyNamespace(QName qname) {
		return getNamespace().equals(qname.getNamespaceURI());
	}

	/**
	 * Main entry point.
	 *
	 * @param schema midPoint schema
	 * @return XSD schema in DOM form
	 * @throws SchemaException error parsing the midPoint schema or converting values
	 */
	@NotNull
	Document parseSchema(PrismSchema schema) throws SchemaException {
		if (schema == null) {
			throw new IllegalArgumentException("Schema can't be null.");
		}
		this.schema = schema;

		try {

			init();			// here the document is initialized

			// Process complex types first.
			Collection<ComplexTypeDefinition> complexTypes = schema.getDefinitions(ComplexTypeDefinition.class);
			for (ComplexTypeDefinition complexTypeDefinition: complexTypes) {
				addComplexTypeDefinition(complexTypeDefinition, document.getDocumentElement());
			}

			Collection<Definition> definitions = schema.getDefinitions();
			for (Definition definition : definitions) {

				if (definition instanceof PrismContainerDefinition) {
					// Add property container definition. This will add <complexType> and <element> definitions to XSD
					addContainerDefinition((PrismContainerDefinition) definition,
							document.getDocumentElement(), document.getDocumentElement());

				} else if (definition instanceof PrismPropertyDefinition) {
					// Add top-level property definition. It will create <element> XSD definition
					addPropertyDefinition((PrismPropertyDefinition) definition,
							document.getDocumentElement());

				} else if (definition instanceof ComplexTypeDefinition){
					// Skip this. Already processed above.

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
	private void addContainerDefinition(PrismContainerDefinition definition,
			Element elementParent, Element complexTypeParent) {

		ComplexTypeDefinition complexTypeDefinition = definition.getComplexTypeDefinition();

		if (complexTypeDefinition != null &&
				// Check if the complex type is a top-level complex type. If it is then it was already processed and we can skip it
				schema.findComplexTypeDefinition(complexTypeDefinition.getTypeName()) == null &&
				// If the definition is not in this schema namespace then skip it. It is only a "ref"
				getNamespace().equals(complexTypeDefinition.getTypeName().getNamespaceURI())
				) {
			addComplexTypeDefinition(complexTypeDefinition,complexTypeParent);
		}

		Element elementElement = addElementDefinition(definition.getName(), definition.getTypeName(), definition.getMinOccurs(), definition.getMaxOccurs(),
				elementParent);

		if (complexTypeDefinition == null || !complexTypeDefinition.isContainerMarker()) {
			// Need to add a:container annotation to the element as the complex type does not have it
			addAnnotationToDefinition(elementElement, A_PROPERTY_CONTAINER);
		}
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
		if (attrNamespace != null && attrNamespace.equals(getNamespace())) {
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

		Element annotation = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
		property.appendChild(annotation);
		Element appinfo = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		annotation.appendChild(appinfo);

		addCommonDefinitionAnnotations(definition, appinfo);

		if (!definition.canAdd() || !definition.canRead() || !definition.canModify()) {
			// read-write-create attribute is the default. If any of this flags is missing, we must
			// add appropriate annotations.
			if (definition.canAdd()) {
				addAnnotation(A_ACCESS, A_ACCESS_CREATE, appinfo);
			}
			if (definition.canRead()) {
				addAnnotation(A_ACCESS, A_ACCESS_READ, appinfo);
			}
			if (definition.canModify()) {
				addAnnotation(A_ACCESS, A_ACCESS_UPDATE, appinfo);
			}
		}

		if (definition.isIndexed() != null) {
			addAnnotation(A_INDEXED, XmlTypeConverter.toXmlTextContent(definition.isIndexed(), A_INDEXED), appinfo);
		}

		if (definition.getMatchingRuleQName() != null) {
			addAnnotation(A_MATCHING_RULE, definition.getMatchingRuleQName(), appinfo);
		}

		if (definition.getValueEnumerationRef() != null) {
			addAnnotation(A_VALUE_ENUMERATION_REF, definition.getValueEnumerationRef(), appinfo);
		}

		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
		definitionFactory.addExtraPropertyAnnotations(definition, appinfo, this);

		if (!appinfo.hasChildNodes()) {
			// remove unneeded <annotation> element
			property.removeChild(annotation);
		}
	}

	/**
	 * Adds XSD element definition created from the PrismReferenceDefinition.
	 * TODO: need to finish
	 */
	private void addReferenceDefinition(PrismReferenceDefinition definition, Element parent) {
		Element property = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "element"));
		// Add to document first, so following methods will be able to resolve namespaces
		parent.appendChild(property);

		String attrNamespace = definition.getName().getNamespaceURI();
		if (attrNamespace != null && attrNamespace.equals(getNamespace())) {
			setAttribute(property, "name", definition.getName().getLocalPart());
			setQNameAttribute(property, "type", definition.getTypeName());
		} else {
			setQNameAttribute(property, "ref", definition.getName());
		}

		if (definition.getCompositeObjectElementName() == null) {
			setMultiplicityAttribute(property, "minOccurs", 0);
		}
		setMultiplicityAttribute(property, "maxOccurs", definition.getMaxOccurs());

		// Add annotations
		Element annotation = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
		property.appendChild(annotation);
		Element appinfo = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		annotation.appendChild(appinfo);

		addAnnotation(A_OBJECT_REFERENCE, appinfo);

		if (definition.getTargetTypeName() != null) {
			addAnnotation(A_OBJECT_REFERENCE_TARGET_TYPE, definition.getTargetTypeName(), appinfo);
		}

		if (definition.getCompositeObjectElementName() == null) {
			return;
		}

		property = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "element"));
		// Add to document first, so following methods will be able to resolve namespaces
		parent.appendChild(property);

		QName elementName = definition.getCompositeObjectElementName();
		attrNamespace = elementName.getNamespaceURI();
		if (attrNamespace != null && attrNamespace.equals(getNamespace())) {
			setAttribute(property, "name", elementName.getLocalPart());
			setQNameAttribute(property, "type", definition.getTargetTypeName());
		} else {
			setQNameAttribute(property, "ref", elementName);
		}

		setMultiplicityAttribute(property, "minOccurs", 0);
		setMultiplicityAttribute(property, "maxOccurs", definition.getMaxOccurs());

		// Add annotations
		annotation = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
		property.appendChild(annotation);
		appinfo = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		annotation.appendChild(appinfo);

		addAnnotation(A_OBJECT_REFERENCE, definition.getName(), appinfo);
		if (definition.isComposite()) {
			addAnnotation(A_COMPOSITE, definition.isComposite(), appinfo);
		}

		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
		definitionFactory.addExtraReferenceAnnotations(definition, appinfo, this);

	}

	/**
	 * Adds XSD element definition.
	 * @param name element QName
	 * @param typeName element type QName
	 * @param parent element under which the definition will be added
	 */
	private Element addElementDefinition(QName name, QName typeName, int minOccurs, int maxOccurs, Element parent) {
		Element elementDef = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "element"));
		parent.appendChild(elementDef);

		if (isMyNamespace(name)) {
			setAttribute(elementDef, "name", name.getLocalPart());

			if (typeName.equals(DOMUtil.XSD_ANY)) {
				addSequenceXsdAnyDefinition(elementDef);
			} else {
				setQNameAttribute(elementDef, "type", typeName);
			}
		} else {
			// Need to create "ref" instead of "name"
			setAttribute(elementDef, "ref", name);
			if (typeName != null) {
				// Type cannot be stored directly, XSD does not allow it with "ref"s.
				addAnnotationToDefinition(elementDef, A_TYPE, typeName);
			}
		}

		setMultiplicityAttribute(elementDef, "minOccurs", minOccurs);
		setMultiplicityAttribute(elementDef, "maxOccurs", maxOccurs);

		return elementDef;
	}

	private void addSequenceXsdAnyDefinition(Element elementDef) {
		Element complexContextElement = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "complexType"));
		elementDef.appendChild(complexContextElement);
		Element sequenceElement = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "sequence"));
		complexContextElement.appendChild(sequenceElement);
		addXsdAnyDefinition(sequenceElement);
	}

	private void addXsdAnyDefinition(Element elementDef) {
		Element anyElement = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "any"));
		elementDef.appendChild(anyElement);
		setAttribute(anyElement, "namespace", "##other");
		setAttribute(anyElement, "minOccurs", "0");
		setAttribute(anyElement, "maxOccurs", "unbounded");
		setAttribute(anyElement, "processContents", "lax");
	}

	/**
	 * Adds XSD complexType definition from the midPoint Schema ComplexTypeDefinion object
	 * @param definition midPoint Schema ComplexTypeDefinion object
	 * @param parent element under which the definition will be added
	 * @return created (and added) XSD complexType definition
	 */
	private Element addComplexTypeDefinition(ComplexTypeDefinition definition,
			Element parent) {

		if (definition == null) {
			// Nothing to do
			return null;
		}
		if (definition.getTypeName() == null) {
			throw new UnsupportedOperationException("Anonymous complex types as containers are not supported yet");
		}

		Element complexType = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "complexType"));
		parent.appendChild(complexType);
		// "typeName" should be used instead of "name" when defining a XSD type
		setAttribute(complexType, "name", definition.getTypeName().getLocalPart());
		Element annotation = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "annotation"));
		complexType.appendChild(annotation);

		Element containingElement = complexType;
		if (definition.getSuperType() != null) {
			Element complexContent = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "complexContent"));
			complexType.appendChild(complexContent);
			Element extension = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "extension"));
			complexContent.appendChild(extension);
			setQNameAttribute(extension, "base", definition.getSuperType());
			containingElement = extension;
		}

		Element sequence = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "sequence"));
		containingElement.appendChild(sequence);

		Collection<? extends ItemDefinition> definitions = definition.getDefinitions();
		for (ItemDefinition def : definitions) {
			if (def instanceof PrismPropertyDefinition) {
				addPropertyDefinition((PrismPropertyDefinition) def, sequence);
			} else if (def instanceof PrismContainerDefinition) {
				PrismContainerDefinition contDef = (PrismContainerDefinition)def;
				addContainerDefinition(contDef, sequence, parent);
			} else if (def instanceof PrismReferenceDefinition) {
				addReferenceDefinition((PrismReferenceDefinition) def, sequence);
			} else {
				throw new IllegalArgumentException("Uknown definition "+def+"("+def.getClass().getName()+") in complex type definition "+def);
			}
		}

		if (definition.isXsdAnyMarker()) {
			addXsdAnyDefinition(sequence);
		}

		Element appinfo = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"));
		annotation.appendChild(appinfo);

		if (definition.isObjectMarker()) {
			// annotation: propertyContainer
			addAnnotation(A_OBJECT, definition.getDisplayName(), appinfo);
		} else if (definition.isContainerMarker()) {
			// annotation: propertyContainer
			addAnnotation(A_PROPERTY_CONTAINER, definition.getDisplayName(), appinfo);
		}

		addCommonDefinitionAnnotations(definition, appinfo);

		SchemaDefinitionFactory definitionFactory = getDefinitionFactory();
		definitionFactory.addExtraComplexTypeAnnotations(definition, appinfo, this);

		if (!appinfo.hasChildNodes()) {
			// remove unneeded <annotation> element
			complexType.removeChild(annotation);
		}

		return complexType;
	}

	private void addCommonDefinitionAnnotations(Definition definition, Element appinfoElement) {
		if (definition.getProcessing() != null) {
			addAnnotation(A_PROCESSING, definition.getProcessing().getValue(), appinfoElement);
		}

		if ((definition instanceof ItemDefinition) && ((ItemDefinition)definition).isOperational()) {
			addAnnotation(A_OPERATIONAL, "true", appinfoElement);
		}

		if (definition.getDisplayName() != null) {
			addAnnotation(A_DISPLAY_NAME, definition.getDisplayName(), appinfoElement);
		}

		if (definition.getDisplayOrder() != null) {
			addAnnotation(A_DISPLAY_ORDER, definition.getDisplayOrder().toString(), appinfoElement);
		}

		if (definition.getHelp() != null) {
			addAnnotation(A_HELP, definition.getHelp(), appinfoElement);
		}

		if (definition.isEmphasized()) {
			addAnnotation(A_EMPHASIZED, "true", appinfoElement);
		}
	}

	/**
	 * Add generic annotation element.
	 * @param qname QName of the element
	 * @param value string value of the element
	 * @param parent element under which the definition will be added
	 * @return created XSD element
	 */
	public Element addAnnotation(QName qname, String value, Element parent) {
		Element annotation = createElement(qname);
		parent.appendChild(annotation);
		if (value != null) {
			annotation.setTextContent(value);
		}
		return annotation;
	}

	public Element addAnnotation(QName qname, boolean value, Element parent) {
		Element annotation = createElement(qname);
		parent.appendChild(annotation);
		annotation.setTextContent(Boolean.toString(value));
		return annotation;
	}

	public Element addAnnotation(QName qname, Element parent) {
		Element annotation = createElement(qname);
		parent.appendChild(annotation);
		return annotation;
	}

	public Element addAnnotation(QName qname, QName value, Element parent) {
		Element annotation = createElement(qname);
		parent.appendChild(annotation);
		if (value != null) {
			DOMUtil.setQNameValue(annotation, value);
		}
		return annotation;
	}

	public Element addAnnotation(QName qname, PrismReferenceValue value, Element parent) {
		Element annotation = createElement(qname);
		parent.appendChild(annotation);
		if (value != null) {
			annotation.setAttribute(ATTRIBUTE_OID_LOCAL_NAME, value.getOid());
			DOMUtil.setQNameAttribute(annotation, ATTRIBUTE_REF_TYPE_LOCAL_NAME, value.getTargetType());
		}
		return annotation;
	}

	private void addAnnotationToDefinition(Element definitionElement, QName qname) {
		addAnnotationToDefinition(definitionElement, qname, null);
	}

	private void addAnnotationToDefinition(Element definitionElement, QName qname, QName value) {
		Element annotationElement = getOrCreateElement(new QName(W3C_XML_SCHEMA_NS_URI, "annotation"), definitionElement);
		Element appinfoElement = getOrCreateElement(new QName(W3C_XML_SCHEMA_NS_URI, "appinfo"), annotationElement);
		if (value == null) {
			addAnnotation(qname, appinfoElement);
		} else {
			addAnnotation(qname, value, appinfoElement);
		}
	}

	private Element getOrCreateElement(QName qName, Element parentElement) {
		NodeList elements = parentElement.getElementsByTagNameNS(qName.getNamespaceURI(), qName.getLocalPart());
		if (elements.getLength() == 0) {
			Element element = createElement(qName);
			Element refChild = DOMUtil.getFirstChildElement(parentElement);
			parentElement.insertBefore(element, refChild);
			return element;
		}
		return (Element)elements.item(0);
	}


	/**
	 * Adds annotation that points to another element (ususaly a property).
	 * @param qname QName of the element
	 * @param value Qname of the target element (property QName)
	 * @param parent parent element under which the definition will be added
	 * @return created XSD element
	 */
	public Element addRefAnnotation(QName qname, QName value, Element parent) {
		Element element = createElement(qname);
		parent.appendChild(element);
		//old way: setQNameAttribute(access, "ref", value);
		DOMUtil.setQNameValue(element, value);
		return element;
	}

	/**
	 * Create schema XSD DOM document.
	 */
	private void init() throws ParserConfigurationException {

		if (namespacePrefixMapper == null) {
			// TODO: clone?
			namespacePrefixMapper = ((SchemaRegistryImpl) prismContext.getSchemaRegistry()).getNamespacePrefixMapper();
		}

		// We don't want the "tns" prefix to be kept in the mapper
		namespacePrefixMapper = namespacePrefixMapper.clone();
		namespacePrefixMapper.registerPrefixLocal(getNamespace(), "tns");

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Using namespace prefix mapper to serialize schema:\n{}",DebugUtil.dump(namespacePrefixMapper));
		}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
		DocumentBuilder db = dbf.newDocumentBuilder();

		document = db.newDocument();
		Element root = createElement(new QName(W3C_XML_SCHEMA_NS_URI, "schema"));
		document.appendChild(root);

		rootXsdElement = document.getDocumentElement();
		setAttribute(rootXsdElement, "targetNamespace", getNamespace());
		setAttribute(rootXsdElement, "elementFormDefault", "qualified");

		DOMUtil.setNamespaceDeclaration(rootXsdElement, "tns", getNamespace());

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
	public Element createElement(QName qname) {
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

	private void setAttribute(Element element, String attrName, QName attrValue) {
		setAttribute(element, new QName(W3C_XML_SCHEMA_NS_URI, attrName), attrValue);
	}

	private void setMultiplicityAttribute(Element element, String attrName, int attrValue) {
		if (attrValue == 1) {
			return;
		}
		setAttribute(element, attrName, XsdTypeMapper.multiplicityToString(attrValue));
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

	private void setAttribute(Element element, QName attr, QName attrValue) {
		if (attributeQualified) {
			DOMUtil.setQNameAttribute(element, attr, attrValue, rootXsdElement);
			addToImport(attr.getNamespaceURI());
		} else {
			DOMUtil.setQNameAttribute(element, attr.getLocalPart(), attrValue, rootXsdElement);
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

			if (getNamespace().equals(namespace)) {
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
