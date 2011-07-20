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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.util;

import static org.junit.Assert.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import org.junit.Test;

/**
 * @author Radovan Semancik
 *
 */
public class DOMUtilTest {
	
	private static final String QNAME_IN_NS = "http://foo.com/bar";
	private static final String QNAME_IN_LOCAL = "baz";
	private static final String ELEMENT_NS = "http://foo.com/barbar";
	private static final String ELEMENT_LOCAL = "el";
	private static final String DEFAULT_NS = "http://foo.com/default";
	private static final String ELEMENT_TOP_LOCAL = "top";
	
	public DOMUtilTest() {
	}

	@Test
	public void testQNameRoundTrip() {
		// GIVEN
		Document doc = DOMUtil.getDocument();
		
		QName in = new QName(QNAME_IN_NS,QNAME_IN_LOCAL);
		Element e = doc.createElementNS(ELEMENT_NS, ELEMENT_LOCAL);
		
		// WHEN
		
		DOMUtil.setQNameValue(e, in);
		
		// THEN
		
		System.out.println(DOMUtil.serializeDOMToString(e));
		
		String content = e.getTextContent();
		String[] split = content.split(":");
		// Default namespace should not be used unless explicitly matches existing declaration
		// therefore there should be a prefix
		assertEquals(2,split.length);
		String prefix = split[0];
		String localPart = split[1];
		assertFalse(prefix.isEmpty());
		String namespaceURI = e.lookupNamespaceURI(prefix);
		assertEquals(QNAME_IN_NS, namespaceURI);
		assertEquals(QNAME_IN_LOCAL, localPart);
		
		// WHEN
		
		QName out = DOMUtil.getQNameValue(e);
		
		// THEN
		
		assertEquals(in, out);
	}
	
	@Test
	public void testQNameDefaultNamespace1() {
		// GIVEN
		Document doc = DOMUtil.getDocument();
		
		QName in = new QName(DEFAULT_NS,QNAME_IN_LOCAL);
		Element topElement = doc.createElementNS(DEFAULT_NS, ELEMENT_TOP_LOCAL);
		// Make sure there is a default ns declaration
		DOMUtil.setNamespaceDeclaration(topElement,"",DEFAULT_NS);
		DOMUtil.setNamespaceDeclaration(topElement,"e",ELEMENT_NS);
		doc.appendChild(topElement);
		Element e = doc.createElementNS(ELEMENT_NS, ELEMENT_LOCAL);
		e.setPrefix("e");
		e.setTextContent("foofoo");
		topElement.appendChild(e);
		
		System.out.println(DOMUtil.serializeDOMToString(topElement));
		
		// WHEN
		
		DOMUtil.setQNameValue(e, in);
		
		// THEN
		
		System.out.println(DOMUtil.serializeDOMToString(topElement));
		
		String content = e.getTextContent();
		// Default namespace should be reused
		assertFalse(content.contains(":"));
		assertEquals(QNAME_IN_LOCAL, content);		
	}

}
