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
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;

import java.io.IOException;
import java.util.GregorianCalendar;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestXmlConversion {
	
	private static final String MY_NS = "http://midpoint.evolveum.com/xml/ns/testing/xmlconversion";
	private static final String MY_ELEMENT_NAME = "foo";
	private static final QName MY_ELEMENT_QNAME = new QName(MY_NS, MY_ELEMENT_NAME);

	@BeforeSuite
	public void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}
	
	@Test
	public void testDateTimeType() throws SchemaException, SAXException, IOException {
		assertEquals("Wrong datetime class", XMLGregorianCalendar.class, XsdTypeMapper.toJavaType(DOMUtil.XSD_DATETIME));
		assertEquals("Wrong datetime class", DOMUtil.XSD_DATETIME, XsdTypeMapper.toXsdType(XMLGregorianCalendar.class));
	}
	
	@Test
	public void testDateTimeValue() throws SchemaException, SAXException, IOException {
		String stringDate = "1975-05-30T21:30:00.000Z";
		Element xmlElement = createElement(stringDate);
		Object javaValue = XmlTypeConverter.toJavaValue(xmlElement, DOMUtil.XSD_DATETIME);
		XMLGregorianCalendar xmlCal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);
		PrismAsserts.assertEquals("Wrong java value", xmlCal, javaValue);
		String xmlTextContent = XmlTypeConverter.toXmlTextContent(xmlCal, MY_ELEMENT_QNAME);
		assertEquals("Wrong xml value", stringDate, xmlTextContent);
	}

	private Element createElement(String string) {
		Document doc = DOMUtil.getDocument();
		Element element = doc.createElementNS(MY_NS, MY_ELEMENT_NAME);
		element.setTextContent(string);
		return element;
	}
	
}
