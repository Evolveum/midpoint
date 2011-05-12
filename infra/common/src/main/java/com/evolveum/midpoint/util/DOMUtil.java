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
import com.evolveum.midpoint.util.diff.OidQualifier;
import com.evolveum.midpoint.util.diff.MidPointDifferenceListener;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;
import java.io.IOException;
import java.io.StringWriter;
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

	private static void setupXmlUnitPatchTool() {
		// XmlUnit setup
		// Note: compareUnmatched has to be set to false to calculate diff
		// properly,
		// to avoid matching of nodes that are not comparable
		XMLUnit.setCompareUnmatched(false);
		XMLUnit.setIgnoreAttributeOrder(true);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setNormalize(true);
		XMLUnit.setNormalizeWhitespace(true);
	}

	private static boolean isEqualsNode(Node node, Node newNode) {
		if ((null == node) && (null == newNode)) {
			return true;
		}
		if ((null != node) && (null == newNode)) {
			return false;
		}
		if ((null == node) && (null != newNode)) {
			return false;
		}

		setupXmlUnitPatchTool();
		Document doc = getDocument(), newDoc = getDocument();
		doc.appendChild(doc.adoptNode(node));
		newDoc.appendChild(newDoc.adoptNode(newNode));
		Diff d = new Diff(doc, newDoc);
		DetailedDiff dd = new DetailedDiff(d);
		dd.overrideElementQualifier(new OidQualifier());
		dd.overrideDifferenceListener(new MidPointDifferenceListener());
		List<Difference> differences = dd.getAllDifferences();

		return differences.size() == 0;

	}

	private static boolean oidEquals(Node node, Node value) {
		String oid = Utils.getNodeOid(node);
		String newOid = Utils.getNodeOid(value);
		if ((null == oid) && (null == newOid)) {
			// if there is no oid in both nodes, then we do not know if they are
			// equal
			// FIXME: temporary changed to true
			return true;
		}
		if ((null != oid) && (null != newOid)) {
			return oid.equals(newOid);
		}
		return false;
	}

	public static void addChildNodes(Node parentNode, Node newNode) {
		// append new childs
		Document parentDoc = parentNode.getOwnerDocument();
		Node adoptedNode = parentDoc.importNode(newNode, true);
		parentNode.appendChild(adoptedNode);
	}

	public static void addChildNodes(Node parentNode, List<Element> newNodes) {
		for (Node newNode : newNodes) {
			addChildNodes(parentNode, newNode);
		}
	}

	// public static void addChildNodes(Node parentNode, String serializedNodes)
	// {
	// Document doc = parseDocument(serializedNodes);
	// NodeList nodes = doc.getChildNodes();
	// for (int i=0; i < nodes.getLength(); i++ ) {
	// Node newNode = nodes .item(i);
	// addChildNodes(parentNode, newNode);
	// }
	// }
	// public static void deleteChildNodes(Node parent, Node existingValue)
	// throws DOMException {
	// // XPath in midPoint we will get parent parentNode for the change,
	// therefor we have to check all children
	// NodeList children = parent.getChildNodes();
	// for (int j = 0; j < children.getLength(); j++) {
	// Node child = children.item(j);
	// //HACK: for unknown reason we have to filter out fake text nodes (' \n')
	// if (Node.TEXT_NODE == child.getNodeType()) {
	// //ingore fake text parent
	// continue;
	// }
	// if (oidEquals(child, existingValue) || isEqualsNode(child,
	// existingValue)) {
	// //compare whole subtree
	// parent.removeChild(child);
	// }
	// }
	// }
	// TODO: temporary solution
	public static void deleteChildNodes(Node parentNode, Node newNode) {
		NodeList childNodes = parentNode.getChildNodes();
		String newNodeName = newNode.getLocalName();
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node child = childNodes.item(j);
			// TODO: we should match not only parent name, but also namespace
			if (null != child.getLocalName()
					&& child.getLocalName().equals(newNodeName)) {
				if (oidEquals(child, newNode)) {
					parentNode.removeChild(child);
				}
			}
		}
	}

	private static void removeChildNodes(Node parentNode, String newNodeName) {
		NodeList childNodes = parentNode.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node child = childNodes.item(j);
			// TODO: we should match not only parent name, but also namespace
			if (null != child.getLocalName()
					&& child.getLocalName().equals(newNodeName)) {
				parentNode.removeChild(child);
			}
		}
	}

	// TODO: implementation cannot fully handle property/attribute with xml
	// nodes only nodes with text nodes are handled correctly
	// e.g. <foo><bar>value</bar></foo> is not supported
	// Problem is in not-functioning isEquals method for two different
	// implementation of Nodes that should be compared
	public static void replaceChildNodes(Node parentNode, Element newNode) {
		removeChildNodes(parentNode, newNode.getLocalName());
		addChildNodes(parentNode, newNode);
	}

	public static void replaceChildNodes(Node parentNode, List<Element> newNodes) {
		if (null == newNodes || newNodes.size() == 0) {
			return;
		}
		String newNodeName = newNodes.get(0).getLocalName();

		// Prerequisite: we are replacing multi-value node, e.g. all elements
		// have the same name
		for (Node newNode : newNodes) {
			if (!StringUtils.equals(newNodeName, newNode.getLocalName())) {
				throw new IllegalArgumentException(
						"Provided list of new nodes contains nodes with different names: "
								+ newNodes);
			}
		}

		removeChildNodes(parentNode, newNodeName);
		addChildNodes(parentNode, newNodes);
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

	public static Node getFirstChildElement(Node parent) {
		if (parent == null || parent.getChildNodes() == null) {
			return null;
		}

		NodeList nodes = parent.getChildNodes();
		for (int i = 0; i < nodes.getLength(); i++) {
			Node child = nodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				return child;
			}
		}

		return null;
	}

	public static void createNodesDefinedByXPath(Document doc,
			XPathType xpathType) {
		Validate.notNull(doc, "Provided parameter doc was null");
		Validate.notNull(xpathType, "Provided parameter xpathType was null");

		List<XPathSegment> segments = xpathType.toSegments();

		if (null != segments) {
			// first we will find first element defined by xpath that does not
			// exists
			Node docChildToWhichAppend = (Element) doc.getFirstChild();
			int i;
			for (i = 0; i < segments.size(); i++) {
				NodeList nodes;
				try {
					XPathType path = null;
					path = new XPathType(segments.subList(0, i + 1));
					nodes = (new XPathUtil()).matchedNodesByXPath(path, null,
							doc.getFirstChild());
				} catch (XPathExpressionException ex) {
					// TODO: exception translation
					throw new IllegalArgumentException(ex);
				}

				if ((null == nodes) || (nodes.getLength() == 0)) {
					break;
				}
				docChildToWhichAppend = docChildToWhichAppend.getFirstChild();
			}

			// if there is at least element that does not exists (defined by
			// xpath), then create it and all remaining elements from xpath
			if (i < segments.size()) {
				Element parentElement = null;
				Element element = null;
				Element child;

				for (int j = i; j < segments.size(); j++) {
					QName qname = segments.get(j).getQName();
					if (null == parentElement) {
						parentElement = doc.createElementNS(
								qname.getNamespaceURI(), qname.getLocalPart());
						element = parentElement;
					} else {
						child = doc.createElementNS(qname.getNamespaceURI(),
								qname.getLocalPart());
						element.appendChild(child);
						element = child;
					}
				}
				docChildToWhichAppend.appendChild(parentElement);
			}
		}

	}
}
