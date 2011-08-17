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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common;

import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.common.diff.MidPointDifferenceListener;
import com.evolveum.midpoint.common.diff.OidQualifier;
import com.evolveum.midpoint.schema.xpath.XPathSegment;
import com.evolveum.midpoint.schema.xpath.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
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
 * This is s remnant of DOMUtil that has to be moved to the utils. It contains
 * methods that have to remain in common.
 * 
 * This still needs a cleanup.
 * 
 * @author Radovan Semancik
 */
public class XmlUtil {
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
		Document doc = DOMUtil.getDocument(), newDoc = DOMUtil.getDocument();
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

	private static void removeChildNodes(Node parentNode, String namespace, String newNodeName) {
		NodeList childNodes = parentNode.getChildNodes();
		for (int j = 0; j < childNodes.getLength(); j++) {
			Node child = childNodes.item(j);
			// TODO: we should match not only parent name, but also namespace
			if (null != child.getLocalName()
					&& child.getLocalName().equals(newNodeName) && child.getNamespaceURI().equals(namespace)) {
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
		removeChildNodes(parentNode, newNode.getNamespaceURI(), newNode.getLocalName());
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

		removeChildNodes(parentNode, newNodes.get(0).getNamespaceURI(), newNodeName);
		addChildNodes(parentNode, newNodes);
	}


}
