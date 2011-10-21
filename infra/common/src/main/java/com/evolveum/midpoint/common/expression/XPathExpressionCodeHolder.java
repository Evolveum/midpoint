/**
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
package com.evolveum.midpoint.common.expression;

import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.schema.holder.TrivialXPathParser;

/**
 * @author Radovan Semancik
 *
 */
public class XPathExpressionCodeHolder {
	
	private Element dom;
	
	public XPathExpressionCodeHolder(Element domElement) {
    	if (domElement==null) {
    		throw new IllegalArgumentException("Attempt to create "+XPathExpressionCodeHolder.class.getSimpleName()+" with null DOM element");
    	}
        dom = domElement;
    }
	
	public NodeList getExpression() {
        return dom.getChildNodes();
    }

	public String getFullExpressionAsString() {
        NodeList childNodes = dom.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.TEXT_NODE || childNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(childNode.getNodeValue());
            } else if (childNode.getNodeType() == Node.COMMENT_NODE) {
                // Silently ignore
            } else {
                // TODO: throw exception
            }
        }
        
        return sb.toString();
    }

    public String getExpressionAsString() {

        String stringExpression = getFullExpressionAsString();

        // try to strip namespace declarations
        TrivialXPathParser parser = TrivialXPathParser.parse(stringExpression);
        stringExpression = parser.getPureXPathString();

        return stringExpression;
    }

    public String lookupNamespaceUri(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return dom.lookupNamespaceURI(null);
        } else {
            return dom.lookupNamespaceURI(prefix);
        }
    }

    public Map<String, String> getNamespaceMap() {

        Map<String, String> namespaceMap = null;

        // Try to process XPath namespace declarations first

        String stringExpression = getFullExpressionAsString();

        // try to strip namespace declarations
        TrivialXPathParser parser = TrivialXPathParser.parse(stringExpression);
        namespaceMap = parser.getNamespaceMap();

        Node node = dom;
        while (node != null) {
            NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attribute = attributes.item(i);
                    if (attribute.getNamespaceURI() != null && attribute.getNamespaceURI().equals("http://www.w3.org/2000/xmlns/")) {
                        String localName = attribute.getLocalName();
                        if (attribute.getPrefix() == null && localName.equals("xmlns")) {
                            if (namespaceMap.get("") == null) {
                                namespaceMap.put("", attribute.getNodeValue());
                            }
                        } else {
                            if (namespaceMap.get(localName) == null) {
                                namespaceMap.put(localName, attribute.getNodeValue());
                            }
                        }
                    }
                }
            }
            node = node.getParentNode();
        }
        return namespaceMap;
    }

}
