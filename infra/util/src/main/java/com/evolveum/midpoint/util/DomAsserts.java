/*
 * Copyright (c) 2014 Evolveum
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

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

public class DomAsserts {

	public static void assertElementQName(Element element, QName expectedQName) {
		QName actual = DOMUtil.getQName(element);
		assertEquals("Wrong element name", expectedQName, actual);
	}

	public static void assertEquals(String message, Object expected, Object actual) {
		assert MiscUtil.equals(expected, actual) : message
				+ ": expected " + MiscUtil.getValueWithClass(expected)
				+ ", was " + MiscUtil.getValueWithClass(actual);
	}

	public static void assertSubElements(Element element, int expectedNumberOfSubelements) {
		List<Element> childred = DOMUtil.listChildElements(element);
		assertEquals("Wrong number of subelements in element "+DOMUtil.getQName(element), expectedNumberOfSubelements, childred.size());
	}

	public static void assertSubElement(Element element, QName expectedSubElementName) {
		Element subElement = DOMUtil.getChildElement(element, expectedSubElementName);
		assert subElement != null : "No subelement "+expectedSubElementName+" in element "+DOMUtil.getQName(element);
	}

	public static void assertTextContent(Element element, String expected) {
		assertEquals("Wrong content in element "+DOMUtil.getQName(element), expected, element.getTextContent());
	}

}
