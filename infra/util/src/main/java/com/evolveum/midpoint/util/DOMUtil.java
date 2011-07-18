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

package com.evolveum.midpoint.util;

//TODO: fix imports - to remove warning during build
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * 
 * @author Igor Farinic
 * @author Radovan Semancik
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class DOMUtil {
	
	public static final String W3C_XML_SCHEMA_XMLNS_URI = "http://www.w3.org/2000/xmlns/";
	public static final String W3C_XML_SCHEMA_XMLNS_PREFIX = "xmlns";
	
	public static final String NS_W3C_XSI_URI = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String NS_W3C_XSI_PREFIX = "xsi";
	public static final QName XSI_TYPE = new QName(NS_W3C_XSI_URI, "type",
			NS_W3C_XSI_PREFIX);
	private static final String RANDOM_ATTR_PREFIX_PREFIX = "qn";
	private static final int RANDOM_ATTR_PREFIX_RND = 1000;
	// To generate random namespace prefixes
	private static Random rnd = new Random();

	public static String serializeDOMToString(org.w3c.dom.Node node) {
		return printDom(node).toString();
	}

	// public static String serializeDOMToString(org.w3c.dom.NodeList nodelist)
	// {
	// StringBuffer buffer = new StringBuffer();
	// for (int i = 0; i < nodelist.getLength(); i++) {
	// buffer.append(printDom(nodelist.item(i)));
	// }
	// return buffer.toString();
	// }
	//
	public static Document getDocument() {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder loader = factory.newDocumentBuilder();
			return loader.newDocument();
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Error creating XML document "
					+ ex.getMessage());
		}
	}

	public static Document parseDocument(String doc) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder loader = factory.newDocumentBuilder();
			return loader.parse(IOUtils.toInputStream(doc, "utf-8"));
		} catch (SAXException ex) {
			throw new IllegalStateException("Error parsing XML document "
					+ ex.getMessage());
		} catch (IOException ex) {
			throw new IllegalStateException("Error parsing XML document "
					+ ex.getMessage());
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Error parsing XML document "
					+ ex.getMessage());
		}
	}

	public static Document parseFile(String filePath) {
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder loader = factory.newDocumentBuilder();
			return loader.parse(new File(filePath));
		} catch (SAXException ex) {
			throw new IllegalStateException("Error parsing XML document "
					+ ex.getMessage());
		} catch (IOException ex) {
			throw new IllegalStateException("Error parsing XML document "
					+ ex.getMessage());
		} catch (ParserConfigurationException ex) {
			throw new IllegalStateException("Error parsing XML document "
					+ ex.getMessage());
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
		StringWriter writer = new StringWriter();
		try {
			TransformerFactory transfac = TransformerFactory.newInstance();
			Transformer trans = transfac.newTransformer();
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setParameter(OutputKeys.ENCODING, "utf-8");
			//Note: serialized XML does not contain xml declaration
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			
			DOMSource source = new DOMSource(node);
			trans.transform(source, new StreamResult(writer));
		} catch (Exception ex) {
			ex.printStackTrace();
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
					if (attribute.getPrefix() == null
							&& attribute.getLocalName().equals("xmlns")
							&& (attribute.getNodeValue() == null || attribute
									.getNodeValue().isEmpty())) {
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
	
	public static List<Element> getSubelementList(Node node) {
		List<Element> subelements = new ArrayList<Element>();
		NodeList childNodes = node.getChildNodes();
		for(int i=0;i<childNodes.getLength();i++) {
			Node childNode = childNodes.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				subelements.add((Element)childNode);
			}
		}
		return subelements;
	}
	
	public static QName resolveQName(Node domNode, String prefixNotation, String defaultNamespacePrefix) {
		if (prefixNotation==null) {
			// No QName
			return null;
		}
        String[] qnameArray = prefixNotation.split(":");
        if (qnameArray.length > 2) {
            throw new IllegalArgumentException("Unsupported format: more than one colon in Qname: " + prefixNotation);
        }
        QName qname;
        if (qnameArray.length == 1 || qnameArray[1] == null || qnameArray[1].isEmpty()) {
            // default namespace <= empty prefix
            String namespace = findNamespace(domNode, null);
            if (defaultNamespacePrefix!=null) {
            	qname = new QName(namespace, qnameArray[0], defaultNamespacePrefix);
            } else {
            	qname = new QName(namespace, qnameArray[0]);
            }
        } else {
            String namespace = findNamespace(domNode, qnameArray[0]);
            qname = new QName(namespace, qnameArray[1], qnameArray[0]);
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

	public static QName resolveXsiType(Element element, String defaultNamespacePrefix) {
		String xsiType = element.getAttributeNS(XSI_TYPE.getNamespaceURI(), XSI_TYPE.getLocalPart());
		if (xsiType == null || xsiType.isEmpty()) {
			return null;
		}
		return resolveQName(element, xsiType, defaultNamespacePrefix);
	}

	public static boolean hasXsiType(Element element) {
		String xsiType = element.getAttributeNS(XSI_TYPE.getNamespaceURI(), XSI_TYPE.getLocalPart());
		if (xsiType == null || xsiType.isEmpty()) {
			return false;
		}
		return true;
	}
	
	public static void setXsiType(Element element, QName type) {
		if (hasXsiType(element)) {
			throw new IllegalArgumentException("Element already has a type");
		}
		setQNameAttribute(element, XSI_TYPE, type);
	}
		
	public static void setQNameAttribute(Element element, QName attributeName, QName attributeValue) {
		if (hasXsiType(element)) {
			throw new IllegalArgumentException("Element already has a type");
		}
		// We need to figure out correct prefix. We have namespace URI, but we need a prefix to specify in the xsi:type
		String valuePrefix = lookupOrCreateNamespaceDeclaration(element,attributeValue.getNamespaceURI(),attributeValue.getPrefix());
		String attrValue = valuePrefix + ":" + attributeValue.getLocalPart();
		
		Document doc = element.getOwnerDocument();
		NamedNodeMap attributes = element.getAttributes();
        Attr attr = doc.createAttributeNS(attributeName.getNamespaceURI(), attributeName.getLocalPart());
        String namePrefix = lookupOrCreateNamespaceDeclaration(element,attributeName.getNamespaceURI(),attributeName.getPrefix());
        attr.setPrefix(namePrefix);
        attr.setValue(attrValue);
        attributes.setNamedItem(attr);
	}
	
	public static String lookupOrCreateNamespaceDeclaration(Element element, String namespaceUri, String preferredPrefix) {
		// We need to figure out correct prefix. We have namespace URI, but we need a prefix to specify in the xsi:type
		String prefix = element.lookupPrefix(namespaceUri);
		if (prefix == null) {
			// try to use preferred prefix from QName
			prefix = preferredPrefix;
			if (prefix != null) {
				// check if a declaration for it exists
				String namespaceDefinedForPreferredPrefix = element.lookupNamespaceURI(prefix);
				if (namespaceDefinedForPreferredPrefix==null || namespaceDefinedForPreferredPrefix.isEmpty()) {
					// No namespace definition for preferred prefix. So let's use it
					setNamespaceDeclaration(element,prefix,namespaceUri);
				} else if (namespaceUri.equals(namespaceDefinedForPreferredPrefix)) {
					// Nothing to do, prefix already defined and the definition matches.
					// The question is how this could happen. Why has element.lookupPrefix() haven't found it?
				} else {
					// prefix already defined, but the URI is different. Fallback to a random prefix.
					prefix = null;
				} 
			}
			if (prefix == null) {
				// generate random prefix
				prefix = RANDOM_ATTR_PREFIX_PREFIX + rnd.nextInt(RANDOM_ATTR_PREFIX_RND);
				setNamespaceDeclaration(element,prefix,namespaceUri);
			}
		}
		return prefix;
	}
	
	public static void setNamespaceDeclaration(Element element,String prefix, String namespaceUri) {
		Document doc = element.getOwnerDocument();
		NamedNodeMap attributes = element.getAttributes();
        Attr attr;
        if (prefix == null || prefix.isEmpty()) {
        	// default namespace
            attr = doc.createAttributeNS(W3C_XML_SCHEMA_XMLNS_URI, W3C_XML_SCHEMA_XMLNS_PREFIX);
       } else {
            attr = doc.createAttributeNS(W3C_XML_SCHEMA_XMLNS_URI, W3C_XML_SCHEMA_XMLNS_PREFIX + ":" + prefix);
       }
       attr.setValue(namespaceUri);
       attributes.setNamedItem(attr);
	}

	public static QName getQName(Element element) {
		return new QName(element.getNamespaceURI(),element.getLocalName(),element.getPrefix());
	}

}
