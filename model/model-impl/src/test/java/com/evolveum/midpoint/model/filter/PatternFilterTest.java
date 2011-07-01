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
 */package com.evolveum.midpoint.model.filter;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.model.controller.Filter;
import com.evolveum.midpoint.util.DOMUtil;

import static com.evolveum.midpoint.model.filter.PatternFilter.*;

/**
 * 
 * @author lazyman
 * 
 */
public class PatternFilterTest {

	private static final String input = "midPoint";
	private static final String expected = "mxdPxxnt";
	private Filter filter;

	@Before
	public void before() {
		filter = new PatternFilter();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullNode() {
		filter.apply(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testEmptyParameters() {
		Node testNode = DOMUtil.getDocument().createElement("tag");
		testNode.setTextContent(input);
		Node node = filter.apply(testNode);

		assertEquals(expected, node.getTextContent());
	}

	@Test
	public void testNullValue() {
		Node testNode = DOMUtil.getDocument().createElement("testTag");
		testNode.setTextContent(null);
		Node node = filter.apply(testNode);
		assertEquals(node, testNode);
	}

	@Test
	public void testEmptyValue() {
		Node testNode = DOMUtil.getDocument().createElement("testTag");
		testNode.setTextContent("");
		Node node = filter.apply(testNode);
		assertEquals(node, testNode);
	}

	@Test
	public void testValueInElement() {
		List<Object> parameters = createGoodParameters();
		filter.setParameters(parameters);

		Node testNode = DOMUtil.getDocument().createElement("tag");
		testNode.setTextContent(input);
		Node node = filter.apply(testNode);

		assertEquals(expected, node.getTextContent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueInElementBadParameters() {
		List<Object> parameters = createBadParameters();
		filter.setParameters(parameters);

		Node testNode = DOMUtil.getDocument().createElement("tag");
		testNode.setTextContent(input);
		Node node = filter.apply(testNode);

		assertEquals(expected, node.getTextContent());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValueInElementBadParameters2() {
		List<Object> parameters = createBadParameters2();
		filter.setParameters(parameters);

		Node testNode = DOMUtil.getDocument().createElement("tag");
		testNode.setTextContent(input);
		Node node = filter.apply(testNode);

		assertEquals(expected, node.getTextContent());
	}

	@Test
	public void testValueInTextNode() {
		List<Object> parameters = createGoodParameters();
		filter.setParameters(parameters);

		Node testNode = DOMUtil.getDocument().createTextNode(input);
		Node node = filter.apply(testNode);

		assertEquals(expected, node.getNodeValue());
	}

	private List<Object> createGoodParameters() {
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(new Object());

		Document document = DOMUtil.getDocument();
		Element replace = document.createElementNS(ELEMENT_REPLACE.getNamespaceURI(),
				ELEMENT_REPLACE.getLocalPart());
		parameters.add(replace);

		Element pattern = document.createElementNS(ELEMENT_PATTERN.getNamespaceURI(),
				ELEMENT_PATTERN.getLocalPart());
		pattern.setTextContent("[aeiouy]");
		replace.appendChild(pattern);

		Element replacement = document.createElementNS(ELEMENT_REPLACEMENT.getNamespaceURI(),
				ELEMENT_REPLACEMENT.getLocalPart());
		replacement.setTextContent("x");
		replace.appendChild(replacement);

		// unknown parameter test
		parameters.add(document.createElementNS("http://example.com", "unknown"));

		return parameters;
	}

	private List<Object> createBadParameters() {
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(new Object());

		Document document = DOMUtil.getDocument();
		Element replace = document.createElementNS(ELEMENT_REPLACE.getNamespaceURI(),
				ELEMENT_REPLACE.getLocalPart());
		parameters.add(replace);

		Element pattern1 = document.createElementNS(ELEMENT_PATTERN.getNamespaceURI(),
				ELEMENT_PATTERN.getLocalPart());
		pattern1.setTextContent("[aeiouy]");
		replace.appendChild(pattern1);

		Element pattern2 = document.createElementNS(ELEMENT_PATTERN.getNamespaceURI(),
				ELEMENT_PATTERN.getLocalPart());
		pattern2.setTextContent("[a-z]");
		replace.appendChild(pattern2);

		Element replacement = document.createElementNS(ELEMENT_REPLACEMENT.getNamespaceURI(),
				ELEMENT_REPLACEMENT.getLocalPart());
		replacement.setTextContent("x");
		replace.appendChild(replacement);

		return parameters;
	}

	private List<Object> createBadParameters2() {
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(new Object());

		Document document = DOMUtil.getDocument();
		Element replace = document.createElementNS(ELEMENT_REPLACE.getNamespaceURI(),
				ELEMENT_REPLACE.getLocalPart());
		parameters.add(replace);

		Element pattern1 = document.createElementNS(ELEMENT_PATTERN.getNamespaceURI(),
				ELEMENT_PATTERN.getLocalPart());
		pattern1.setTextContent("[aeiouy]");
		replace.appendChild(pattern1);

		Element replacement = document.createElementNS(ELEMENT_REPLACEMENT.getNamespaceURI(),
				ELEMENT_REPLACEMENT.getLocalPart());
		replacement.setTextContent("x");
		replace.appendChild(replacement);
		
		Element replacement2 = document.createElementNS(ELEMENT_REPLACEMENT.getNamespaceURI(),
				ELEMENT_REPLACEMENT.getLocalPart());
		replacement2.setTextContent("2");
		replace.appendChild(replacement2);

		return parameters;
	}
}
