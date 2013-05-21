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
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertSame;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.springframework.util.xml.DomUtils;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestDom {
	
	// Disabled: this is not a finished functionality. It now fails on PolyString
	@Test(enabled=false)
	public void testAsDom() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testAsDom ]===");
		
		// GIVEN
		Document document = DOMUtil.parseFile(USER_JACK_FILE);
		Element userElement = DOMUtil.getFirstChildElement(document);
				
		PrismContext prismContext = constructInitializedPrismContext();
		
		PrismObject<UserType> user = prismContext.parseObject(userElement);
		
		System.out.println("User:");
		System.out.println(user.dump());
		assertNotNull(user);
		
		// WHEN
		Element userDom = user.asDomElement();
		
		// THEN
		assertNotNull(userDom);
		
		assertUser(userDom);
	}

	@Test
	public void testSerializeToDom() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testSerializeToDom ]===");
		
		// GIVEN
		Document document = DOMUtil.parseFile(USER_JACK_FILE);
		Element userElement = DOMUtil.getFirstChildElement(document);
				
		PrismContext prismContext = constructInitializedPrismContext();
		PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
		
		PrismObject<UserType> user = prismContext.parseObject(userElement);
		
		System.out.println("User:");
		System.out.println(user.dump());
		assertNotNull(user);
		
		// WHEN
		Element userDom = domProcessor.serializeToDom(user);
		
		// THEN
		assertNotNull(userDom);
		
		assertUser(userDom);
	}	
	
	private void assertUser(Element userDom) {
		assertElementName(USER_QNAME, userDom);
//		String oid = userDom.getAttribute("oid");
//		assertEquals("Wrong oid (getAttribute)", USER_JACK_OID, oid);
		assertElementChildNodes(userDom, 15);
		
		NodeList fullNameDoms = userDom.getElementsByTagNameNS(USER_FULLNAME_QNAME.getNamespaceURI(), USER_FULLNAME_QNAME.getLocalPart());
		assertSingleStringElement(USER_FULLNAME_QNAME, "cpt. Jack Sparrow", fullNameDoms);
		
		NodeList assignmentDoms = userDom.getElementsByTagNameNS(USER_ASSIGNMENT_QNAME.getNamespaceURI(), USER_ASSIGNMENT_QNAME.getLocalPart());
		assertEquals("Wrong number of assignments", assignmentDoms.getLength(), 2);
		assertAssignment(assignmentDoms.item(0));
		assertAssignment(assignmentDoms.item(1));
		
		String userXml = DOMUtil.serializeDOMToString(userDom);
		System.out.println("User XML:");
		System.out.println(userXml);
		
		assertNull("<user> element does not have default namespace prefix", userDom.getPrefix());
	}

	private void assertAssignment(Node node) {
		assertElement(node);
		Element element = (Element)node;
		String id = element.getAttribute("id");
		assertFalse("No id in assignment", id == null || id.isEmpty());
		if (id.equals(USER_ASSIGNMENT_2_ID)) {
			NodeList constructionDoms = element.getElementsByTagNameNS(USER_ACCOUNT_CONSTRUCTION_QNAME.getNamespaceURI(), USER_ACCOUNT_CONSTRUCTION_QNAME.getLocalPart());
			assertEquals("Wrong number of constructions", constructionDoms.getLength(), 1);
			assertConstruction(constructionDoms.item(0));
		}
	}

	private void assertConstruction(Node node) {
		assertElement(node);
		Element element = (Element)node;
		assertChildNodes(element, 2);
	}

	private void assertSingleStringElement(QName qname, String value, NodeList nodelist) {
		assertEquals("Expected just one node with name "+qname, 1, nodelist.getLength());
		Node node = nodelist.item(0);
		assertStringElement(qname, value, node);
	}
	
	private void assertStringElement(QName qname, String value, Node node) {
		assertElement(node);
		Element element = (Element)node;
		assertElementName(qname, element);
		assertEquals("Wrong string element "+qname+" TextContent", value, element.getTextContent());
		assertChildNodes(element, 1);
		Node textNode = element.getFirstChild();
		assertTextNode(qname, value, textNode);
	}
	
	private void assertTextNode(QName qname, String value, Node node) {
		assertEquals("Expect text node type in "+qname, Node.TEXT_NODE, node.getNodeType());
		assertTrue("Expect text node class in "+qname+" but got "+node.getClass(), node instanceof Text);
		assertEquals("Wrong TextContext in text node of "+qname, value, node.getNodeValue());
		assertEquals("Wrong TextContext in text node of "+qname, value, ((Text)node).getTextContent());
	}

	private void assertElement(Node node) {
		assertEquals("Expect element type for "+DOMUtil.getQName(node), Node.ELEMENT_NODE, node.getNodeType());
		assertTrue("Expect element class for "+DOMUtil.getQName(node)+" but got "+node.getClass(), node instanceof Element);
	}

	private void assertElementChildNodes(Node parentNode, int expectedNumber) {
		NodeList childNodes = parentNode.getChildNodes();
		QName parentQName = DOMUtil.getQName(parentNode);
		assertEquals("Wrong number of "+parentQName+" child nodes", expectedNumber, childNodes.getLength());
		assertChildNodes(parentNode, expectedNumber);
		for(int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			assertElement(childNode);
		}
	}

	private void assertChildNodes(Node parentNode, int expectedNumber) {
		NodeList childNodes = parentNode.getChildNodes();
		QName parentQName = DOMUtil.getQName(parentNode);
		assertNotNull("No child nodes in "+parentQName, childNodes);
		assertEquals("Wrong number of "+parentQName+" child nodes", expectedNumber, childNodes.getLength());
		for(int i = 0; i < childNodes.getLength(); i++) {
			Node childNode = childNodes.item(i);
			// We are not really strict about parent nodes now
//			assertSame("Wrong parent node in "+childNode, parentNode, childNode.getParentNode());
			if (i == 0) {
				assertSame("Wrong first child for "+parentNode, childNode, parentNode.getFirstChild());
			} else {
				assertSame("Wrong previous sibling (index "+i+") for "+childNode, childNodes.item(i - 1), childNode.getPreviousSibling());
			}
			if( i == childNodes.getLength() - 1) {
				assertSame("Wrong last child for "+parentNode, childNode, parentNode.getLastChild());
			} else {
				assertSame("Wrong next sibling (index "+i+") for "+childNode, childNodes.item(i + 1), childNode.getNextSibling());
			}
		}
	}

	
	private void assertElementName(QName qname, Element element) {
		assertEquals("Wrong element name", qname, DOMUtil.getQName(element));
	}
	

}
