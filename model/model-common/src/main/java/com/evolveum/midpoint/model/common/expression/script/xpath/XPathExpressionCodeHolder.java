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
package com.evolveum.midpoint.model.common.expression.script.xpath;

import java.util.Map;

import com.evolveum.midpoint.prism.marshaller.TrivialItemPathParser;

/**
 * @author Radovan Semancik
 *
 */
public class XPathExpressionCodeHolder {
	
	private String expression;              // TODO think about this one! (there's a problem with namespaces now)
	
	public XPathExpressionCodeHolder(String expression) {
    	if (expression == null) {
    		throw new IllegalArgumentException("Attempt to create "+XPathExpressionCodeHolder.class.getSimpleName()+" with null DOM element");
    	}
        this.expression = expression;
    }
	
//	public NodeList getExpression() {
//        return dom.getChildNodes();
//    }

	public String getFullExpressionAsString() {
        return expression;
//        NodeList childNodes = dom.getChildNodes();
//        StringBuilder sb = new StringBuilder();
//        for (int i = 0; i < childNodes.getLength(); i++) {
//            Node childNode = childNodes.item(i);
//            if (childNode.getNodeType() == Node.TEXT_NODE || childNode.getNodeType() == Node.CDATA_SECTION_NODE) {
//                sb.append(childNode.getNodeValue());
//            } else if (childNode.getNodeType() == Node.COMMENT_NODE) {
//                // Silently ignore
//            } else {
//                // TODO: throw exception
//            }
//        }
//
//        return sb.toString();
    }

    public String getExpressionAsString() {

        String stringExpression = getFullExpressionAsString();

        // try to strip namespace declarations
        TrivialItemPathParser parser = TrivialItemPathParser.parse(stringExpression);
        stringExpression = parser.getPureItemPathString();

        return stringExpression;
    }

    public String lookupNamespaceUri(String prefix) {
        // not available any more [pm]
//        if (prefix == null || prefix.isEmpty()) {
//            return dom.lookupNamespaceURI(null);
//        } else {
//            return dom.lookupNamespaceURI(prefix);
//        }
        return null;
    }

    public Map<String, String> getNamespaceMap() {

        Map<String, String> namespaceMap = null;

        // Try to process XPath namespace declarations first

        String stringExpression = getFullExpressionAsString();

        // try to strip namespace declarations
        TrivialItemPathParser parser = TrivialItemPathParser.parse(stringExpression);
        namespaceMap = parser.getNamespaceMap();

        // this isn't available any more [pm]
//        Node node = dom;
//        while (node != null) {
//            NamedNodeMap attributes = node.getAttributes();
//            if (attributes != null) {
//                for (int i = 0; i < attributes.getLength(); i++) {
//                    Node attribute = attributes.item(i);
//                    if (attribute.getNamespaceURI() != null && attribute.getNamespaceURI().equals("http://www.w3.org/2000/xmlns/")) {
//                        String localName = attribute.getLocalName();
//                        if (attribute.getPrefix() == null && localName.equals("xmlns")) {
//                            if (namespaceMap.get("") == null) {
//                                namespaceMap.put("", attribute.getNodeValue());
//                            }
//                        } else {
//                            if (namespaceMap.get(localName) == null) {
//                                namespaceMap.put(localName, attribute.getNodeValue());
//                            }
//                        }
//                    }
//                }
//            }
//            node = node.getParentNode();
//        }
        return namespaceMap;
    }

}
