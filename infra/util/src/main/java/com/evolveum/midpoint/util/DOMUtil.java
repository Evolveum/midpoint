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
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * 
 * 
 * @author Igor Farinic
 * @version $Revision$ $Date$
 * @since 0.1
 */
public class DOMUtil {

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

}
