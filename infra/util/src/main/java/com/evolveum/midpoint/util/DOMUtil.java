/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.util;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.sun.org.apache.xml.internal.utils.XMLChar;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.util.exception.SystemException;

/**
 * @author Igor Farinic
 * @author Radovan Semancik
 * @since 0.1
 */
public class DOMUtil {

    public static final Trace LOGGER = TraceManager.getTrace(DOMUtil.class);

	public static final String W3C_XML_SCHEMA_XMLNS_URI = "http://www.w3.org/2000/xmlns/";
	public static final String W3C_XML_SCHEMA_XMLNS_PREFIX = "xmlns";

	public static final String W3C_XML_XML_URI = "http://www.w3.org/XML/1998/namespace";
	public static final String W3C_XML_XML_PREFIX = "xml";

	public static final String NS_W3C_XSI_PREFIX = "xsi";
	public static final QName XSI_TYPE = new QName(W3C_XML_SCHEMA_INSTANCE_NS_URI, "type", NS_W3C_XSI_PREFIX);
	public static final QName XSI_NIL = new QName(W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil", NS_W3C_XSI_PREFIX);
	public static final QName XML_ID_ATTRIBUTE = new QName(W3C_XML_XML_URI, "id", W3C_XML_XML_PREFIX);

    public static final String HACKED_XSI_TYPE = "xsiType";
	public static final String IS_LIST_ATTRIBUTE_NAME = "list";
	private static final List<String> AUXILIARY_ATTRIBUTE_NAMES = Arrays.asList(HACKED_XSI_TYPE, IS_LIST_ATTRIBUTE_NAME);
	private static final List<String> AUXILIARY_NAMESPACES = Arrays.asList(W3C_XML_SCHEMA_XMLNS_URI, W3C_XML_XML_URI, W3C_XML_SCHEMA_INSTANCE_NS_URI);

	public static final String NS_W3C_XML_SCHEMA_PREFIX = "xsd";
	public static final QName XSD_SCHEMA_ELEMENT = new QName(W3C_XML_SCHEMA_NS_URI, "schema",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_ANNOTATION_ELEMENT = new QName(W3C_XML_SCHEMA_NS_URI, "annotation",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_APPINFO_ELEMENT = new QName(W3C_XML_SCHEMA_NS_URI, "appinfo",
			NS_W3C_XML_SCHEMA_PREFIX);
    public static final QName XSD_DOCUMENTATION_ELEMENT = new QName(W3C_XML_SCHEMA_NS_URI, "documentation",
            NS_W3C_XML_SCHEMA_PREFIX);
    public static final QName XSD_IMPORT_ELEMENT = new QName(W3C_XML_SCHEMA_NS_URI, "import",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_INCLUDE_ELEMENT = new QName(W3C_XML_SCHEMA_NS_URI, "include",
			NS_W3C_XML_SCHEMA_PREFIX);

	public static final QName XSD_ATTR_TARGET_NAMESPACE = new QName(W3C_XML_SCHEMA_NS_URI, "targetNamespace",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_ATTR_NAMESPACE = new QName(W3C_XML_SCHEMA_NS_URI, "namespace",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_ATTR_SCHEMA_LOCATION = new QName(W3C_XML_SCHEMA_NS_URI, "schemaLocation",
			NS_W3C_XML_SCHEMA_PREFIX);

    public static final QName XSD_DECIMAL = new QName(W3C_XML_SCHEMA_NS_URI, "decimal",
            NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_STRING = new QName(W3C_XML_SCHEMA_NS_URI, "string",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_INTEGER = new QName(W3C_XML_SCHEMA_NS_URI, "integer",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_INT = new QName(W3C_XML_SCHEMA_NS_URI, "int",
			NS_W3C_XML_SCHEMA_PREFIX);
    public static final QName XSD_LONG = new QName(W3C_XML_SCHEMA_NS_URI, "long",
            NS_W3C_XML_SCHEMA_PREFIX);
    public static final QName XSD_SHORT = new QName(W3C_XML_SCHEMA_NS_URI, "short",
            NS_W3C_XML_SCHEMA_PREFIX);
    public static final QName XSD_FLOAT = new QName(W3C_XML_SCHEMA_NS_URI, "float",
            NS_W3C_XML_SCHEMA_PREFIX);
    public static final QName XSD_DOUBLE = new QName(W3C_XML_SCHEMA_NS_URI, "double",
            NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_BOOLEAN = new QName(W3C_XML_SCHEMA_NS_URI, "boolean",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_BASE64BINARY = new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_DATETIME = new QName(W3C_XML_SCHEMA_NS_URI, "dateTime",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_DURATION = new QName(W3C_XML_SCHEMA_NS_URI, "duration",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_BYTE = new QName(W3C_XML_SCHEMA_NS_URI, "byte",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_QNAME = new QName(W3C_XML_SCHEMA_NS_URI, "QName", NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_ANYURI = new QName(W3C_XML_SCHEMA_NS_URI, "anyURI", NS_W3C_XML_SCHEMA_PREFIX);

	public static final QName XSD_ANY = new QName(W3C_XML_SCHEMA_NS_URI, "any", NS_W3C_XML_SCHEMA_PREFIX);
    public static final QName XSD_ANYTYPE = new QName(W3C_XML_SCHEMA_NS_URI, "anyType", NS_W3C_XML_SCHEMA_PREFIX);

	public static final String NS_XML_ENC = "http://www.w3.org/2001/04/xmlenc#";
	public static final String NS_XML_DSIG = "http://www.w3.org/2000/09/xmldsig#";

	public static final String NS_WSDL = "http://schemas.xmlsoap.org/wsdl/";
	public static final String NS_WSDL_SCHEMA_PREFIX = "wsdl";
    public static final QName WSDL_IMPORT_ELEMENT = new QName(NS_WSDL, "import",
    		NS_WSDL_SCHEMA_PREFIX);
    public static final QName WSDL_TYPES_ELEMENT = new QName(NS_WSDL, "types",
    		NS_WSDL_SCHEMA_PREFIX);
	public static final QName WSDL_ATTR_NAMESPACE = new QName(NS_WSDL, "namespace",
			NS_WSDL_SCHEMA_PREFIX);
	public static final QName WSDL_ATTR_SCHEMA_LOCATION = new QName(NS_WSDL, "schemaLocation",
			NS_WSDL_SCHEMA_PREFIX);
	public static final QName WSDL_ATTR_LOCATION = new QName(NS_WSDL, "location",
			NS_WSDL_SCHEMA_PREFIX);

	private static final String RANDOM_ATTR_PREFIX_PREFIX = "qn";
	private static final int RANDOM_ATTR_PREFIX_RND = 1000;
	private static final int RANDOM_ATTR_PREFIX_MAX_ITERATIONS = 30;

    // To generate random namespace prefixes
	private static Random rnd = new Random();

	private static final DocumentBuilder loader;

	static {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			loader = factory.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Error creating XML document " + ex.getMessage());
		}
	}

    public static String serializeDOMToString(org.w3c.dom.Node node) {
		return printDom(node).toString();
	}

	public static void serializeDOMToFile(org.w3c.dom.Node node, File file) throws TransformerFactoryConfigurationError, TransformerException {

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Result output = new StreamResult(file);
		Source input = new DOMSource(node);

		transformer.transform(input, output);
	}

	public static Document getDocument(Node node) {
		if (node instanceof Document) {
			return (Document) node;
		}
		return node.getOwnerDocument();
	}

	public static Document getDocument() {
		return loader.newDocument();
	}

    public static Document getDocument(QName rootElementName) {
        Document document = loader.newDocument();
        document.appendChild(createElement(document, rootElementName));
        return document;
    }

    public static DocumentBuilder createDocumentBuilder() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Error creating document builder " + e.getMessage(), e);
        }
    }

    public static Document parseDocument(String doc) {
		try {
			DocumentBuilder loader = createDocumentBuilder();
			return loader.parse(IOUtils.toInputStream(doc, "utf-8"));
		} catch (SAXException | IOException ex) {
			throw new IllegalStateException("Error parsing XML document " + ex.getMessage(),ex);
		}
	}

	public static Document parseFile(String filePath) {
		return parseFile(new File(filePath));
	}

	public static Document parseFile(File file) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder loader = factory.newDocumentBuilder();
			return loader.parse(file);
		} catch (SAXException | IOException | ParserConfigurationException ex) {
			throw new IllegalStateException("Error parsing XML document " + ex.getMessage(),ex);
		}
	}

	public static Document parse(InputStream inputStream) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature("http://xml.org/sax/features/namespaces", true);
			// voodoo to turn off reading of DTDs during parsing. This is needed e.g. to pre-parse schemas
			factory.setValidating(false);
			factory.setFeature("http://xml.org/sax/features/validation", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			DocumentBuilder loader = factory.newDocumentBuilder();
			return loader.parse(inputStream);
		} catch (SAXException | ParserConfigurationException ex) {
			throw new IllegalStateException("Error parsing XML document " + ex.getMessage(),ex);
		}
	}

	public static String showDom(List<Element> elements) {
		StringBuilder sb = new StringBuilder();
		for (Element element : elements) {
			showDomNode(element, sb, 0);
			sb.append("\n");
		}
		return sb.toString();
	}

    public static StringBuffer printDom(Node node) {
        return printDom(node, true, true);
    }

	public static StringBuffer printDom(Node node, boolean indent, boolean omitXmlDeclaration) {
		StringWriter writer = new StringWriter();
		TransformerFactory transfac = TransformerFactory.newInstance();
		Transformer trans;
		try {
			trans = transfac.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new SystemException("Error in XML configuration: "+e.getMessage(),e);
		}
		trans.setOutputProperty(OutputKeys.INDENT, (indent ? "yes" : "no"));
        trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");      // XALAN-specific
		trans.setParameter(OutputKeys.ENCODING, "utf-8");
        // Note: serialized XML does not contain xml declaration
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, (omitXmlDeclaration ? "yes" : "no"));

		DOMSource source = new DOMSource(node);
		try {
			trans.transform(source, new StreamResult(writer));
		} catch (TransformerException e) {
			throw new SystemException("Error in XML transformation: "+e.getMessage(),e);
		}

		return writer.getBuffer();
	}

	private static void showDomNode(Node node, StringBuilder sb, int level) {
		if (sb == null) {
			// buffer not provided, return immediately
			return;
		}

		// indent
		for (int i = 0; i < level; i++) {
			sb.append("  ");
		}
		if (node == null) {
			sb.append("null\n");
		} else {
			sb.append(node.getNodeName());
			sb.append(" (");
			NamedNodeMap attributes = node.getAttributes();
			boolean broken = false;
			if (attributes != null) {
				for (int ii = 0; ii < attributes.getLength(); ii++) {
					Node attribute = attributes.item(ii);
					sb.append(attribute.getPrefix());
					sb.append(":");
					sb.append(attribute.getLocalName());
					sb.append("='");
					sb.append(attribute.getNodeValue());
					sb.append("',");
					if (attribute.getPrefix() == null && attribute.getLocalName().equals("xmlns")
							&& (attribute.getNodeValue() == null || attribute.getNodeValue().isEmpty())) {
						broken = true;
					}
				}
			}
			sb.append(")");
			if (broken) {
				sb.append(" *** WARNING: empty default namespace");
			}
			sb.append("\n");
			NodeList childNodes = node.getChildNodes();
			for (int ii = 0; ii < childNodes.getLength(); ii++) {
				Node subnode = childNodes.item(ii);
				showDomNode(subnode, sb, level + 1);
			}
		}

	}

	public static Node getNextSiblingElement(Node node) {
		if (node == null || node.getParentNode() == null) {
			return null;
		}
		Node parent = node.getParentNode();
		NodeList nodes = parent.getChildNodes();
		if (nodes == null) {
			return null;
		}
		boolean found = false;
		for (int i = 0; i < nodes.getLength(); i++) {
			Node child = nodes.item(i);
			if (child.equals(node)) {
				found = true;
				continue;
			}
			if (found && child.getNodeType() == Node.ELEMENT_NODE) {
				return child;
			}
		}

		return null;
	}

	public static Element getFirstChildElement(Node parent) {
		if (parent == null || parent.getChildNodes() == null) {
			return null;
		}

		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node child = nodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) child;
			}
		}

		return null;
	}

	public static Element getLastChildElement(Node parent) {
		if (parent == null || parent.getChildNodes() == null) {
			return null;
		}

		NodeList nodes = parent.getChildNodes();
		for (int i = nodes.getLength() - 1; i >= 0; i--) {
			Node child = nodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) child;
			}
		}

		return null;
	}

	public static List<Element> getChildElements(Element element, QName elementName) {
		Validate.notNull(elementName, "Element name to get must not be null");
		List<Element> elements = new ArrayList<>();
		NodeList childNodes = element.getChildNodes();
		for (int i= 0; i< childNodes.getLength(); i++){
			Node childNode = childNodes.item(i);
			if (QNameUtil.compareQName(elementName, childNode)){
				elements.add((Element)childNode);
			}
		}
		return elements;
	}

	@NotNull
	public static List<Element> listChildElements(Node node) {
		List<Element> subelements = new ArrayList<>();
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				subelements.add((Element) childNode);
			}
		}
		return subelements;
	}

	public static boolean hasChildElements(Node node) {
		List<Element> childElements = listChildElements(node);
		return (!childElements.isEmpty());
	}

	public static QName resolveQName(Element element) {
		return resolveQName(element, element.getTextContent());
	}

    /**
     * Resolves a QName.
     *
     * @param domNode Provides a context in which we will resolve namespace prefixes (may be null)
     * @param qnameStringRepresentation String representation of a QName (e.g. c:RoleType) (may be null)
     *
     * @return parsed QName (or null if string representation is blank)
     *
     * Contrary to traditional XML handling, a QName without prefix is parsed to a QName without namespace,
     * even if default namespace declaration is present.
     */

	public static QName resolveQName(Node domNode, String qnameStringRepresentation) {
		if (StringUtils.isBlank(qnameStringRepresentation)) {
			// No QName
			return null;
		}
		String[] qnameArray = qnameStringRepresentation.split(":");
		if (qnameArray.length > 2) {
			throw new IllegalArgumentException("Unsupported format: more than one colon in Qname: "
					+ qnameStringRepresentation);
		}
		QName qname;
		if (qnameArray.length == 1 || qnameArray[1] == null || qnameArray[1].isEmpty()) {
			// no prefix => no namespace
			qname = new QName(null, qnameArray[0]);
		} else {
            String namespacePrefix = qnameArray[0];
			String namespace = findNamespace(domNode, namespacePrefix);
            if (namespace == null) {
                QNameUtil.reportUndeclaredNamespacePrefix(namespacePrefix, qnameStringRepresentation);
                namespacePrefix = QNameUtil.markPrefixAsUndeclared(namespacePrefix);
            }
			qname = new QName(namespace, qnameArray[1], namespacePrefix);
		}
		return qname;
	}


    public static String findNamespace(Node domNode, String prefix) {
		String ns = null;
		if (domNode != null) {
			if (prefix == null || prefix.isEmpty()) {
				ns = domNode.lookupNamespaceURI(null);
			} else {
				ns = domNode.lookupNamespaceURI(prefix);
			}
			if (ns != null) {
				return ns;
			}
		}
		return ns;
	}

	public static QName resolveXsiType(Element element) {
		String xsiType = element.getAttributeNS(XSI_TYPE.getNamespaceURI(), XSI_TYPE.getLocalPart());
		if (xsiType == null || xsiType.isEmpty()) {
			xsiType = element.getAttribute(HACKED_XSI_TYPE);
        }
        if (xsiType == null || xsiType.isEmpty()) {
            return null;
        }
		return resolveQName(element, xsiType);
	}

	public static boolean hasXsiType(Element element) {
		String xsiType = element.getAttributeNS(XSI_TYPE.getNamespaceURI(), XSI_TYPE.getLocalPart());
        if (xsiType == null || xsiType.isEmpty()) {
            xsiType = element.getAttribute(HACKED_XSI_TYPE);
        }
		if (xsiType == null || xsiType.isEmpty()) {
			return false;
		}
		return true;
	}

	public static void removeXsiType(Element element) {
		element.removeAttributeNS(XSI_TYPE.getNamespaceURI(), XSI_TYPE.getLocalPart());
		element.removeAttribute(HACKED_XSI_TYPE);
	}

	public static void setXsiType(Element element, QName type) {
		if (hasXsiType(element)) {
			throw new IllegalArgumentException("Element already has a type");
		}
		setQNameAttribute(element, XSI_TYPE, type);
	}

	public static void setQNameAttribute(Element element, QName attributeName, QName attributeValue) {
		Document doc = element.getOwnerDocument();
		Attr attr = doc.createAttributeNS(attributeName.getNamespaceURI(), attributeName.getLocalPart());
		String namePrefix = lookupOrCreateNamespaceDeclaration(element, attributeName.getNamespaceURI(),
				attributeName.getPrefix(), element, true);
		attr.setPrefix(namePrefix);
		setQNameAttribute(element, attr, attributeValue, element);
	}

	public static void setQNameAttribute(Element element, String attributeName, QName attributeValue) {
		Document doc = element.getOwnerDocument();
		Attr attr = doc.createAttribute(attributeName);
		setQNameAttribute(element, attr, attributeValue, element);
	}

	public static void setQNameAttribute(Element element, QName attributeName, QName attributeValue,
			Element definitionElement) {
		Document doc = element.getOwnerDocument();
		Attr attr = doc.createAttributeNS(attributeName.getNamespaceURI(), attributeName.getLocalPart());
		String namePrefix = lookupOrCreateNamespaceDeclaration(element, attributeName.getNamespaceURI(),
				attributeName.getPrefix(), element, true);
		attr.setPrefix(namePrefix);
		setQNameAttribute(element, attr, attributeValue, definitionElement);
	}

	public static void setQNameAttribute(Element element, String attributeName, QName attributeValue,
			Element definitionElement) {
		Document doc = element.getOwnerDocument();
		Attr attr = doc.createAttribute(attributeName);
		setQNameAttribute(element, attr, attributeValue, definitionElement);
	}

    /*
     * Actually, it is not possible to create *and use* xmlns declaration pointing to an empty URI. From Section 6.1 in http://www.w3.org/TR/xml-names11/
     *
     * <?xml version="1.1"?>
     *   <x xmlns:n1="http://www.w3.org">
     *     <n1:a/>               <!-- legal; the prefix n1 is bound to http://www.w3.org -->
     *     <x xmlns:n1="">
     *       <n1:a/>           <!-- illegal; the prefix n1 is not bound here -->
	 *         <x xmlns:n1="http://www.w3.org">
     *           <n1:a/>       <!-- legal; the prefix n1 is bound again -->
     *         </x>
     *     </x>
     *   </x>
     *
     * We strictly use localname-only representation of QNames with null NS. When writing, we write it in such a way.
     * And when reading, we ignore default namespace when parsing unqualified QNames.
     */
	private static void setQNameAttribute(Element element, Attr attr, QName attributeQnameValue, Element definitionElement) {
        String attributeStringValue;

        if (attributeQnameValue == null) {
            attributeStringValue = "";
        } else if (XMLConstants.NULL_NS_URI.equals(attributeQnameValue.getNamespaceURI())) {
            if (QNameUtil.isPrefixUndeclared(attributeQnameValue.getPrefix())) {
                attributeStringValue = attributeQnameValue.getPrefix() + ":" + attributeQnameValue.getLocalPart();      // to give user a chance to see and fix this
            } else {
                attributeStringValue = attributeQnameValue.getLocalPart();
            }
        } else {
            String valuePrefix = lookupOrCreateNamespaceDeclaration(element, attributeQnameValue.getNamespaceURI(),
                    attributeQnameValue.getPrefix(), definitionElement, false);
            assert StringUtils.isNotBlank(valuePrefix);
            attributeStringValue = valuePrefix + ":" + attributeQnameValue.getLocalPart();
        }

		NamedNodeMap attributes = element.getAttributes();
        checkValidXmlChars(attributeStringValue);
		attr.setValue(attributeStringValue);
		attributes.setNamedItem(attr);
	}

    /**
     * Sets QName value for a given element.
     *
     * Contrary to standard XML semantics, namespace-less QNames are specified as simple names without prefix
     * (regardless of default prefix used in the XML document).
     *
     * @param element Element whose text content should be set to represent QName value
     * @param elementValue QName value to be stored into the element
     */
	public static void setQNameValue(Element element, QName elementValue) {
        if (elementValue == null) {
            setElementTextContent(element, "");
        } else if (XMLConstants.NULL_NS_URI.equals(elementValue.getNamespaceURI())) {
            if (QNameUtil.isPrefixUndeclared(elementValue.getPrefix())) {
                setElementTextContent(element, elementValue.getPrefix() + ":" + elementValue.getLocalPart());
            } else {
                setElementTextContent(element, elementValue.getLocalPart());
            }
        } else {
            String prefix = lookupOrCreateNamespaceDeclaration(element, elementValue.getNamespaceURI(),
                    elementValue.getPrefix(), element, false);
            assert StringUtils.isNotBlank(prefix);
            String stringValue = prefix + ":" + elementValue.getLocalPart();
            setElementTextContent(element, stringValue);
        }
	}

    /**
     * @param element Element, on which the namespace declaration is evaluated
     * @param namespaceUri Namespace URI to be assigned to a prefix
     * @param preferredPrefix Preferred prefix
     * @param definitionElement Element, on which namespace declaration will be created (there should not be any redefinitions between definitionElement and element in order for this to work...)
     * @param allowUseOfDefaultNamespace If we are allowed to use default namespace (i.e. return empty prefix). This is important for QNames, see setQNameValue
     * @return prefix that is really used
     *
     * Returned prefix is never null nor "" if allowUseOfDefaultNamespace is false.
     */

	public static String lookupOrCreateNamespaceDeclaration(Element element, String namespaceUri,
			String preferredPrefix, Element definitionElement, boolean allowUseOfDefaultNamespace) {
		// We need to figure out correct prefix. We have namespace URI, but we
		// need a prefix to specify in the xsi:type or element name
		if (!StringUtils.isBlank(preferredPrefix)) {
			String namespaceForPreferredPrefix = element.lookupNamespaceURI(preferredPrefix);
			if (namespaceForPreferredPrefix == null) {
                // preferred prefix is not yet bound
				setNamespaceDeclaration(definitionElement, preferredPrefix, namespaceUri);
				return preferredPrefix;
			} else {
				if (namespaceForPreferredPrefix.equals(namespaceUri)) {
					return preferredPrefix;
				} else {
					// Prefix conflict, we need to create different prefix
					// Just going on will do that
				}
			}
		}
        if (allowUseOfDefaultNamespace && element.isDefaultNamespace(namespaceUri)) {
            // Namespace URI is a default namespace. Return empty prefix;
            return "";
        }
        // We DO NOT WANT to use default namespace for QNames. QNames without prefix are NOT considered by midPoint to belong to the default namespace.
		String prefix = element.lookupPrefix(namespaceUri);
		if (prefix == null) {
            // generate random prefix
            boolean gotIt = false;
            for (int i=0; i < RANDOM_ATTR_PREFIX_MAX_ITERATIONS; i++) {
                prefix = generatePrefix();
                if (element.lookupNamespaceURI(prefix) == null) {
                    // the prefix is free
                    gotIt = true;
                    break;
                }
            }
            if (!gotIt) {
                throw new IllegalStateException("Unable to generate unique prefix for namespace "+namespaceUri+" even after "+RANDOM_ATTR_PREFIX_MAX_ITERATIONS+" attempts");
            }
            setNamespaceDeclaration(definitionElement, prefix, namespaceUri);
		}
		return prefix;
	}

	private static String generatePrefix() {
		return RANDOM_ATTR_PREFIX_PREFIX + rnd.nextInt(RANDOM_ATTR_PREFIX_RND);
	}

	public static boolean isNamespaceDefinition(Attr attr) {
		if (W3C_XML_SCHEMA_XMLNS_URI.equals(attr.getNamespaceURI())) {
			return true;
		}
		return attr.getName().startsWith("xmlns:") || "xmlns".equals(attr.getName());
	}

	public static void setNamespaceDeclaration(Element element, String prefix, String namespaceUri) {
		Document doc = element.getOwnerDocument();
		NamedNodeMap attributes = element.getAttributes();
		Attr attr;
		if (prefix == null || prefix.isEmpty()) {
			// default namespace
			attr = doc.createAttributeNS(W3C_XML_SCHEMA_XMLNS_URI, W3C_XML_SCHEMA_XMLNS_PREFIX);
		} else {
			attr = doc
					.createAttributeNS(W3C_XML_SCHEMA_XMLNS_URI, W3C_XML_SCHEMA_XMLNS_PREFIX + ":" + prefix);
		}
        checkValidXmlChars(namespaceUri);
		attr.setValue(namespaceUri);
		attributes.setNamedItem(attr);
	}

	/**
	 * Returns map of all namespace declarations from specified element (prefix -&gt; namespace).
	 */
	public static Map<String,String> getNamespaceDeclarations(Element element) {
		Map<String,String> nsDeclMap = new HashMap<>();
		NamedNodeMap attributes = element.getAttributes();
		for(int i=0; i<attributes.getLength(); i++) {
			Attr attr = (Attr)attributes.item(i);
			if (isNamespaceDefinition(attr)) {
				String prefix = getNamespaceDeclarationPrefix(attr);
				String namespace = getNamespaceDeclarationNamespace(attr);
				nsDeclMap.put(prefix, namespace);
			}
		}
		return nsDeclMap;
	}

	public static void setNamespaceDeclarations(Element element, Map<String, String> rootNamespaceDeclarations) {
        if (rootNamespaceDeclarations == null) {
            return;
        }
		for (Entry<String, String> entry : rootNamespaceDeclarations.entrySet()) {
			setNamespaceDeclaration(element, entry.getKey(), entry.getValue());
		}
	}

    /**
     * Returns all namespace declarations visible from the given node.
     * Uses recursion for simplicity.
     */
    public static Map<String,String> getAllVisibleNamespaceDeclarations(Node node) {
        Map<String,String> retval;
        Node parent = getParentNode(node);
        if (parent != null) {
            retval = getAllVisibleNamespaceDeclarations(parent);
        } else {
            retval = new HashMap<>();
        }
        if (node instanceof Element) {
            retval.putAll(getNamespaceDeclarations((Element) node));
        }
        return retval;
    }

    // returns owner node - works also for attributes
    private static Node getParentNode(Node node) {
        if (node instanceof Attr) {
            Attr attr = (Attr) node;
            return attr.getOwnerElement();
        } else {
            return node.getParentNode();
        }
    }

    /**
	 * Take all the namespace declaration of parent elements and put them to this element.
	 */
	public static void fixNamespaceDeclarations(Element element) {
		fixNamespaceDeclarations(element, element);
	}

	private static void fixNamespaceDeclarations(Element targetElement, Element currentElement) {
		NamedNodeMap attributes = currentElement.getAttributes();
		for(int i=0; i<attributes.getLength(); i++) {
			Attr attr = (Attr)attributes.item(i);
			if (isNamespaceDefinition(attr)) {
				String prefix = getNamespaceDeclarationPrefix(attr);
				//String namespace = getNamespaceDeclarationNamespace(attr);
				if (hasNamespaceDeclarationForPrefix(targetElement, prefix)) {
					if (targetElement != currentElement) {
						// We are processing parent element, while the original element already
						// has prefix declaration. That means it must have been processed before
						// we can skip the usage check
						continue;
					}
				} else {
					setNamespaceDeclaration(targetElement, prefix, getNamespaceDeclarationNamespace(attr));
				}
			}
		}
		Node parentNode = currentElement.getParentNode();
		if (parentNode instanceof Element) {
			fixNamespaceDeclarations(targetElement, (Element)parentNode);
		}
	}

	public static boolean isPrefixUsed(Element targetElement, String prefix) {
		if (comparePrefix(prefix, targetElement.getPrefix())) {
			return true;
		}
		NamedNodeMap attributes = targetElement.getAttributes();
		for(int i=0; i<attributes.getLength(); i++) {
			Attr attr = (Attr)attributes.item(i);
			if (comparePrefix(prefix, attr.getPrefix())) {
				return true;
			}
		}
		NodeList childNodes = targetElement.getChildNodes();
		for (int i=0; i<childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node instanceof Element) {
				Element element = (Element)node;
				if (isPrefixUsed(element, prefix)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean hasNamespaceDeclarationForPrefix(Element targetElement, String prefix) {
		return getNamespaceDeclarationForPrefix(targetElement, prefix) != null;
	}

	public static String getNamespaceDeclarationForPrefix(Element targetElement, String prefix) {
		NamedNodeMap attributes = targetElement.getAttributes();
		for(int i=0; i<attributes.getLength(); i++) {
			Attr attr = (Attr)attributes.item(i);
			if (isNamespaceDefinition(attr)) {
				String thisPrefix = getNamespaceDeclarationPrefix(attr);
				if (comparePrefix(prefix, thisPrefix)) {
					return getNamespaceDeclarationNamespace(attr);
				}
			}
		}
		return null;
	}

	public static String getNamespaceDeclarationPrefix(Attr attr) {
		if(!W3C_XML_SCHEMA_XMLNS_URI.equals(attr.getNamespaceURI())) {
			throw new IllegalStateException("Attempt to get prefix from a attribute that is not a namespace declaration, it has namespace "
					+ attr.getNamespaceURI());
		}
		String attrName = attr.getName();
		if(attrName.startsWith("xmlns:")) {
			return attrName.substring(6);
		}
		if ("xmlns".equals(attrName)) {
			return null;
		}
		throw new IllegalStateException("Attempt to get prefix from a attribute that is not a namespace declaration, it is "+attrName);
	}

	public static String getNamespaceDeclarationNamespace(Attr attr) {
		if(!W3C_XML_SCHEMA_XMLNS_URI.equals(attr.getNamespaceURI())) {
			throw new IllegalStateException("Attempt to get namespace from a attribute that is not a namespace declaration, it has namespace "
					+ attr.getNamespaceURI());
		}
		String attrName = attr.getName();
		if(!attrName.startsWith("xmlns:") && !"xmlns".equals(attr.getName())) {
			throw new IllegalStateException("Attempt to get namespace from a attribute that is not a namespace declaration, it is "+attrName);
		}
		return attr.getValue();
	}

	public static Collection<Attr> listApplicationAttributes(Element element) {
		Collection<Attr> attrs = new ArrayList<>();
		NamedNodeMap attributes = element.getAttributes();
		for(int i=0; i<attributes.getLength(); i++) {
			Attr attr = (Attr)attributes.item(i);
			if (isApplicationAttribute(attr)) {
				attrs.add(attr);
			}
		}
		return attrs;
	}


	public static boolean hasApplicationAttributes(Element element) {
		NamedNodeMap attributes = element.getAttributes();
		for(int i=0; i<attributes.getLength(); i++) {
			Attr attr = (Attr)attributes.item(i);
			if (isApplicationAttribute(attr)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isApplicationAttribute(Attr attr) {
		String namespaceURI = attr.getNamespaceURI();
        if (StringUtils.isEmpty(namespaceURI)) {
			return !AUXILIARY_ATTRIBUTE_NAMES.contains(attr.getName());
		} else {
        	return !AUXILIARY_NAMESPACES.contains(namespaceURI);
		}
	}

	private static boolean comparePrefix(String prefixA, String prefixB) {
		if (StringUtils.isBlank(prefixA) && StringUtils.isBlank(prefixB)) {
			return true;
		}
		if (StringUtils.isBlank(prefixA) || StringUtils.isBlank(prefixB)) {
			return false;
		}
		return prefixA.equals(prefixB);
	}

	public static Element getChildElement(Element element, QName qname) {
		for (Element subelement: listChildElements(element)) {
			if (qname.equals(getQName(subelement))) {
				return subelement;
			}
		}
		return null;
	}

	public static Element getChildElement(Element element, String localPart) {
		for (Element subelement: listChildElements(element)) {
			if (subelement.getLocalName().equals(localPart)) {
				return subelement;
			}
		}
		return null;
	}

	public static Element getChildElement(Element element, int index) {
		return listChildElements(element).get(index);
	}

	public static Element getOrCreateAsFirstElement(Element parentElement, QName elementQName) {
		Element element = getChildElement(parentElement, elementQName);
		if (element != null) {
			return element;
		}
		Document doc = parentElement.getOwnerDocument();
		element = doc.createElementNS(elementQName.getNamespaceURI(), elementQName.getLocalPart());
		parentElement.insertBefore(element, getFirstChildElement(parentElement));
		return element;
	}

	@NotNull
	public static QName getQName(Element element) {
		QName name = getQName((Node) element);
		if (name == null) {
			throw new IllegalStateException("Element with no name: " + element);
		} else {
			return name;
		}
	}

	public static QName getQName(Node node) {
		if (node.getLocalName() == null) {
			if (node.getNodeName() == null) {
				return null;
			} else {
				return new QName(null, node.getNodeName());
			}
		}
		if (node.getPrefix() == null) {
			return new QName(node.getNamespaceURI(), node.getLocalName());
		}
		return new QName(node.getNamespaceURI(), node.getLocalName(), node.getPrefix());
	}

	public static QName getQNameValue(Element element) {
		return resolveQName(element, element.getTextContent());
	}

	public static QName getQNameAttribute(Element element, String attributeName) {
		String attrContent = element.getAttribute(attributeName);
		if (StringUtils.isBlank(attrContent)) {
			return null;
		}
		return resolveQName(element, attrContent);
	}

	public static QName getQNameAttribute(Element element, QName attributeName) {
		String attrContent = element.getAttributeNS(attributeName.getNamespaceURI(), attributeName.getLocalPart());
		if (StringUtils.isBlank(attrContent)) {
			return null;
		}
		return resolveQName(element, attrContent);
	}

	public static QName getQNameValue(Attr attr) {
		return resolveQName(attr, attr.getTextContent());
	}

	public static Integer getIntegerValue(Element element) {
		if (element == null) {
			return null;
		}
		String textContent = element.getTextContent();
		if (StringUtils.isBlank(textContent)) {
			return null;
		}
		return Integer.valueOf(textContent);
	}

	public static void copyContent(Element source, Element destination) {
		NamedNodeMap attributes = source.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr attr = (Attr) attributes.item(i);
			Attr clone = (Attr) attr.cloneNode(true);
			destination.setAttributeNode(clone);
		}
		NodeList childNodes = source.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node item = childNodes.item(i);
			destination.appendChild(item);
		}
	}

    public static Element createElement(QName qname) {
        return createElement(DOMUtil.getDocument(), qname);
    }

	public static Element createElement(Document document, QName qname) {
		Element element;
//		String namespaceURI = qname.getNamespaceURI();
//		if (StringUtils.isBlank(namespaceURI)) {
//			element = document.createElement(qname.getLocalPart());
//		} else {
			element = document.createElementNS(qname.getNamespaceURI(), qname.getLocalPart());
//		}
		if (StringUtils.isNotEmpty(qname.getPrefix()) && StringUtils.isNotEmpty(qname.getNamespaceURI())) {     // second part of the condition is because of wrong data in tests (undeclared prefixes in XPath expressions)
			element.setPrefix(qname.getPrefix());
		}
		return element;
	}

	public static Element createElement(Document document, QName qname, Element parentElement,
			Element definitionElement) {
		lookupOrCreateNamespaceDeclaration(parentElement, qname.getNamespaceURI(), qname.getPrefix(),
				definitionElement, true);
		return createElement(document, qname);
	}

	public static Element createSubElement(Element parent, QName subElementQName) {
		Document doc = parent.getOwnerDocument();
		Element subElement = createElement(doc, subElementQName);
		parent.appendChild(subElement);
		return subElement;
	}

	public static boolean compareElement(Element a, Element b, boolean considerNamespacePrefixes) {
		return compareElement(a, b, considerNamespacePrefixes, true);
	}

	public static boolean compareElement(Element a, Element b, boolean considerNamespacePrefixes, boolean considerWhitespaces) {
		if (a==b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (!getQName(a).equals(getQName(b))) {
			return false;
		}
		if (!compareAttributes(a.getAttributes(),b.getAttributes(), considerNamespacePrefixes)) {
			return false;
		}
		if (!compareNodeList(a.getChildNodes(),b.getChildNodes(), considerNamespacePrefixes, considerWhitespaces)) {
			return false;
		}
		return true;
	}

	public static boolean compareDocument(Document a, Document b, boolean considerNamespacePrefixes, boolean considerWhitespaces) {
		if (a==b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		if (!compareNodeList(a.getChildNodes(),b.getChildNodes(), considerNamespacePrefixes, considerWhitespaces)) {
			return false;
		}
		return true;
	}

	public static boolean compareElementList(List<Element> aList, List<Element> bList, boolean considerNamespacePrefixes) {
		return compareElementList(aList, bList, considerNamespacePrefixes, true);
	}

	public static boolean compareElementList(List<Element> aList, List<Element> bList, boolean considerNamespacePrefixes, boolean considerWhitespaces) {
		if (aList.size() != bList.size()) {
			return false;
		}
		Iterator<Element> bIterator = bList.iterator();
		for (Element a: aList) {
			Element b = bIterator.next();
			if (!compareElement(a, b, considerNamespacePrefixes, considerWhitespaces)) {
				return false;
			}
		}
		return true;
	}

	private static boolean compareAttributes(NamedNodeMap a, NamedNodeMap b, boolean considerNamespacePrefixes) {
		if (a==b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}

		return (compareAttributesIsSubset(a,b,considerNamespacePrefixes)
				&& compareAttributesIsSubset(b,a,considerNamespacePrefixes));
	}

	private static boolean compareAttributesIsSubset(NamedNodeMap subset, NamedNodeMap superset, boolean considerNamespacePrefixes) {
		for (int i = 0; i < subset.getLength(); i++) {
			Node aItem = subset.item(i);
			Attr aAttr = (Attr) aItem;
			if (!considerNamespacePrefixes && isNamespaceDefinition(aAttr)) {
				continue;
			}
			if (StringUtils.isBlank(aAttr.getLocalName())) {
				// this is strange, but it can obviously happen
				continue;
			}
			QName aQname = new QName(aAttr.getNamespaceURI(),aAttr.getLocalName());
			Attr bAttr = findAttributeByQName(superset,aQname);
			if (bAttr == null) {
				return false;
			}
			if (!StringUtils.equals(aAttr.getTextContent(),bAttr.getTextContent())) {
				return false;
			}
		}
		return true;
	}

	private static Attr findAttributeByQName(NamedNodeMap attrs, QName qname) {
		for (int i = 0; i < attrs.getLength(); i++) {
			Node aItem = attrs.item(i);
			Attr aAttr = (Attr) aItem;
			if (aAttr.getLocalName() == null) {
				continue;
			}
			QName aQname = new QName(aAttr.getNamespaceURI(), aAttr.getLocalName());
			if (aQname.equals(qname)) {
				return aAttr;
			}
		}
		return null;
	}

	private static boolean compareNodeList(NodeList a, NodeList b, boolean considerNamespacePrefixes, boolean considerWhitespaces) {
		if (a==b) {
			return true;
		}
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}

		List<Node> aList = canonizeNodeList(a);
		List<Node> bList = canonizeNodeList(b);

		if (aList.size() != bList.size()) {
			return false;
		}

		Iterator<Node> aIterator = aList.iterator();
		Iterator<Node> bIterator = bList.iterator();
		while (aIterator.hasNext()) {
			Node aItem = aIterator.next();
			Node bItem = bIterator.next();
			if (aItem.getNodeType() != bItem.getNodeType()) {
				return false;
			}
			if (aItem.getNodeType() == Node.ELEMENT_NODE) {
				if (!compareElement((Element)aItem, (Element)bItem, considerNamespacePrefixes, considerWhitespaces)) {
					return false;
				}
			} else if (aItem.getNodeType() == Node.TEXT_NODE) {
				if (!compareTextNodeValues(aItem.getTextContent(), bItem.getTextContent(), considerWhitespaces)) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean compareTextNodeValues(String a, String b) {
		return compareTextNodeValues(a, b, true);
	}

	public static boolean compareTextNodeValues(String a, String b, boolean considerWhitespaces) {
		if (StringUtils.equals(a,b)) {
			return true;
		}
		if (!considerWhitespaces && StringUtils.trimToEmpty(a).equals(StringUtils.trimToEmpty(b))) {
			return true;
		}
		if (StringUtils.isBlank(a) && StringUtils.isBlank(b)) {
			return true;
		}
		return false;
	}

	private static final String SPACE_REGEX = "\\s*";
	private static final Pattern SPACE_PATTERN = Pattern.compile(SPACE_REGEX);

	private static List<Node> canonizeNodeList(NodeList nodelist) {
		List<Node> list = new ArrayList<Node>(nodelist.getLength());
		for (int i = 0; i < nodelist.getLength(); i++) {
			Node aItem = nodelist.item(i);
			if (aItem.getNodeType() == Node.ELEMENT_NODE || aItem.getNodeType() == Node.ATTRIBUTE_NODE) {
				list.add(aItem);
			} else if (aItem.getNodeType() == Node.TEXT_NODE || aItem.getNodeType() == Node.CDATA_SECTION_NODE) {
				if (!SPACE_PATTERN.matcher(aItem.getTextContent()).matches()) {
					list.add(aItem);
				}
			}
		}
		return list;
	}

	public static void normalize(Node node, boolean keepWhitespaces) {
		NodeList childNodes = node.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node aItem = childNodes.item(i);
			if (aItem.getNodeType() == Node.COMMENT_NODE) {
				node.removeChild(aItem);
				i--;
			} else if (aItem.getNodeType() == Node.TEXT_NODE) {
				if (SPACE_PATTERN.matcher(aItem.getTextContent()).matches()) {
					node.removeChild(aItem);
					i--;
				} else {
					if (!keepWhitespaces) {
						aItem.setTextContent(aItem.getTextContent().trim());
					}
				}
			} else if (aItem.getNodeType() == Node.ELEMENT_NODE) {
				normalize(aItem, keepWhitespaces);
			}
		}
	}

	private static final String WS_ONLY_REGEX = "^\\s*$";
	private static final Pattern WS_ONLY_PATTERN = Pattern.compile(WS_ONLY_REGEX);

	public static boolean isJunk(Node node) {
		if (node.getNodeType() == Node.COMMENT_NODE) {
			return true;
		}
		if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
			return true;
		}
		if (node.getNodeType() == Node.TEXT_NODE) {
			Text text = (Text)node;
			if (WS_ONLY_PATTERN.matcher(text.getTextContent()).matches()) {
				return true;
			}
			return false;
		}
		return false;
	}

	public static void validateNonEmptyQName(QName qname, String shortDescription, boolean allowEmptyNamespace) {
		if (qname == null) {
			throw new IllegalArgumentException("null" + shortDescription);
		}
        // This is hard to enforce. There are situations where unmarshalling
        // object reference types as ObjectReferenceType, not as prism references.
        // (E.g. when dealing with reference variables in mappigns/expressions.)
        // In these cases we need to qualify types after the unmarshalling is complete.
		if (!allowEmptyNamespace && StringUtils.isEmpty(qname.getNamespaceURI())) {
			throw new IllegalArgumentException("Missing namespace"+shortDescription);
		}
		if (StringUtils.isEmpty(qname.getLocalPart())) {
			throw new IllegalArgumentException("Missing local part"+shortDescription);
		}
	}

	public static Element findElementRecursive(Element element, QName elementQName) {
		if (elementQName.equals(getQName(element))) {
			return element;
		}
		for (Element subElement: listChildElements(element)) {
			Element foundElement = findElementRecursive(subElement, elementQName);
			if (foundElement != null) {
				return foundElement;
			}
		}
		return null;
	}

    public static QName getQNameWithoutPrefix(Node node) {
        QName qname = getQName(node);
        return new QName(qname.getNamespaceURI(), qname.getLocalPart());
    }

    public static boolean isElementName(Element element, QName name) {
        return name.equals(getQNameWithoutPrefix(element));
    }

	public static boolean isNil(Element element) {
		String nilString = element.getAttributeNS(XSI_NIL.getNamespaceURI(), XSI_NIL.getLocalPart());
		if (nilString == null) {
			return false;
		}
		return Boolean.parseBoolean(nilString);
	}

	public static void setNill(Element element) {
		element.setAttributeNS(XSI_NIL.getNamespaceURI(), XSI_NIL.getLocalPart(), "true");
	}

    /**
     * Serializes the content of the element to a string (without the eclosing element tags).
     */
    public static String serializeElementContent(Element element) {
        String completeXml = serializeDOMToString(element);
        String restXml = completeXml.replaceFirst("^\\s*<[^>]>", "");
        return restXml.replaceFirst("</[^>]>\\s*$", "");
    }

	public static boolean isEmpty(Element element) {
		if (element == null) {
			return true;
		}
		if (hasChildElements(element)) {
			return false;
		}
		if (isNil(element)) {
			return true;
		}
		return StringUtils.isBlank(element.getTextContent());
	}

	public static boolean isEmpty(Attr attr) {
		if (attr == null) {
			return true;
		}
		return StringUtils.isEmpty(attr.getValue());
	}

    public static void setAttributeValue(Element element, String name, String value) {
        checkValidXmlChars(value);
        element.setAttribute(name, value);
    }

    public static void setElementTextContent(Element element, String value) {
        checkValidXmlChars(value);
        element.setTextContent(value);
    }

    public static void checkValidXmlChars(String stringValue) {
        if (stringValue == null) {
            return;
        }
        for (int i = 0; i < stringValue.length(); i++) {
            if (!XMLChar.isValid(stringValue.charAt(i))) {
                throw new IllegalStateException("Invalid character with regards to XML (code " + ((int) stringValue.charAt(i)) + ") in '" + makeSafelyPrintable(stringValue, 200) + "'");
            }
        }
    }

    // todo move to some Util class
    private static String makeSafelyPrintable(String text, int maxSize) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!XMLChar.isValid(c)) {
                sb.append('.');
            } else if (Character.isWhitespace(c)) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
            if (i == maxSize) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }

    public static void createComment(Element element, String text) {
        if (text != null) {
            Comment commentNode = element.getOwnerDocument().createComment(replaceInvalidXmlChars(text));
            element.appendChild(commentNode);
        }
    }

    private static String replaceInvalidXmlChars(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!XMLChar.isValid(c)) {
                sb.append('.');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String getAttribute(Element element, QName attrQname) {
    	String attr = element.getAttributeNS(attrQname.getNamespaceURI(), attrQname.getLocalPart());
		if (StringUtils.isEmpty(attr)) {
			// also try without the namespace
			attr = element.getAttribute(attrQname.getLocalPart());
		}
		return attr;
    }

	public static boolean hasNoPrefix(Element top) {
		return Objects.equals(top.getLocalName(), top.getNodeName());
	}

	@NotNull
	public static List<Element> getElementsWithoutNamespacePrefix(Element element) {
    	List<Element> rv = new ArrayList<>();
    	getElementsWithoutNamespacePrefix(element, rv);
    	return rv;
	}

	private static void getElementsWithoutNamespacePrefix(Element element, List<Element> result) {
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			if (childNode instanceof Element) {
				Element childElement = (Element) childNode;
				if (hasNoPrefix(childElement)) {
					result.add(childElement);
				} else {
					getElementsWithoutNamespacePrefix(childElement, result);
				}
			}
		}
	}
}
