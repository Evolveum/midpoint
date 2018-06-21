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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author semancik
 *
 */
public class TestTypeConversion {

	private static final String MY_NS = "http://midpoint.evolveum.com/xml/ns/testing/xmlconversion";
	private static final String MY_ELEMENT_NAME = "foo";
	private static final QName MY_ELEMENT_QNAME = new QName(MY_NS, MY_ELEMENT_NAME);

	@BeforeSuite
	public void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}

	@Test
	public void testXmlDateTimeType() throws Exception {
		assertEquals("Wrong datetime class", XMLGregorianCalendar.class, XsdTypeMapper.toJavaType(DOMUtil.XSD_DATETIME));
		assertEquals("Wrong datetime class", DOMUtil.XSD_DATETIME, XsdTypeMapper.toXsdType(XMLGregorianCalendar.class));
	}

	@Test
	public void testXmlDateTimeValue() throws Exception {
		String stringDate = "1975-05-30T21:30:00.000Z";
		Element xmlElement = createElement(stringDate);
		Object javaValue = XmlTypeConverter.toJavaValue(xmlElement, DOMUtil.XSD_DATETIME);
		XMLGregorianCalendar xmlCal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);
		PrismAsserts.assertEquals("Wrong java value", xmlCal, javaValue);
		String xmlTextContent = XmlTypeConverter.toXmlTextContent(xmlCal, MY_ELEMENT_QNAME);
		assertEquals("Wrong xml value", stringDate, xmlTextContent);
	}

	@Test
	public void testJavaStringToXMLGregorianCalendar() throws Exception {
		String stringDate = "1975-05-30T21:30:00.000Z";

		// WHEN
		XMLGregorianCalendar xmlGregorianCalendar = JavaTypeConverter.convert(XMLGregorianCalendar.class, stringDate);

		// THEN
		XMLGregorianCalendar expectedCal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);
		PrismAsserts.assertEquals("Wrong java value", expectedCal, xmlGregorianCalendar);
	}

	@Test
	public void testJavaStringToXMLGregorianCalendarWrongFormat() throws Exception {
		String stringDate = "blah blah blah";

		try {
			// WHEN
			XMLGregorianCalendar xmlGregorianCalendar = JavaTypeConverter.convert(XMLGregorianCalendar.class, stringDate);

			AssertJUnit.fail("Unexpected success");
		} catch (IllegalArgumentException e) {
			// This is expected
			// THEN
			assertTrue("Wrong exception message: "+e.getMessage(), e.getMessage().contains("Unable to parse the date"));
		}
	}

	@Test
	public void testJavaXMLGregorianCalendarToString() throws Exception {
		XMLGregorianCalendar cal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);

		// WHEN
		String string = JavaTypeConverter.convert(String.class, cal);

		// THEN
		PrismAsserts.assertEquals("Wrong java value", "1975-05-30T21:30:00.000Z", string);
	}

	@Test
	public void testJavaXMLGregorianCalendarToLong() throws Exception {
		XMLGregorianCalendar cal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);

		// WHEN
		Long val = JavaTypeConverter.convert(Long.class, cal);

		// THEN
		PrismAsserts.assertEquals("Wrong java value", 170717400000L, val);
	}

	private Element createElement(String string) {
		Document doc = DOMUtil.getDocument();
		Element element = doc.createElementNS(MY_NS, MY_ELEMENT_NAME);
		element.setTextContent(string);
		return element;
	}

}
