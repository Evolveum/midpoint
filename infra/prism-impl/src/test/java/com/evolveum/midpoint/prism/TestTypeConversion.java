/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.impl.xml.XmlTypeConverterInternal;
import com.evolveum.midpoint.prism.util.CloneUtil;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author semancik
 *
 */
public class TestTypeConversion extends AbstractPrismTest {

    private static final String MY_NS = "http://midpoint.evolveum.com/xml/ns/testing/xmlconversion";
    private static final String MY_ELEMENT_NAME = "foo";
    private static final QName MY_ELEMENT_QNAME = new QName(MY_NS, MY_ELEMENT_NAME);

    @Test
    public void testXmlDateTimeType() {
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
        String xmlTextContent = XmlTypeConverterInternal.toXmlTextContent(xmlCal, MY_ELEMENT_QNAME);
        assertEquals("Wrong xml value", stringDate, xmlTextContent);
    }

    @Test
    public void testJavaStringToXMLGregorianCalendar() {
        String stringDate = "1975-05-30T21:30:00.000Z";

        // WHEN
        XMLGregorianCalendar xmlGregorianCalendar = JavaTypeConverter.convert(XMLGregorianCalendar.class, stringDate);

        // THEN
        XMLGregorianCalendar expectedCal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);
        PrismAsserts.assertEquals("Wrong java value", expectedCal, xmlGregorianCalendar);
    }

    @Test
    public void testJavaStringToXMLGregorianCalendarWrongFormat() {
        String stringDate = "blah blah blah";

        try {
            // WHEN
            JavaTypeConverter.convert(XMLGregorianCalendar.class, stringDate);

            AssertJUnit.fail("Unexpected success");
        } catch (IllegalArgumentException e) {
            // This is expected
            // THEN
            assertTrue("Wrong exception message: "+e.getMessage(), e.getMessage().contains("Unable to parse the date"));
        }
    }

    @Test
    public void testJavaXMLGregorianCalendarToString() {
        XMLGregorianCalendar cal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);

        // WHEN
        String string = JavaTypeConverter.convert(String.class, cal);

        // THEN
        PrismAsserts.assertEquals("Wrong java value", "1975-05-30T21:30:00.000Z", string);
    }

    @Test
    public void testJavaXMLGregorianCalendarToLong() {
        XMLGregorianCalendar cal = XmlTypeConverter.createXMLGregorianCalendar(1975, 5, 30, 21, 30, 0);

        // WHEN
        Long val = JavaTypeConverter.convert(Long.class, cal);

        // THEN
        PrismAsserts.assertEquals("Wrong java value", 170717400000L, val);
    }

    @Test
    public void testDurationConversion() {
        Duration duration = XmlTypeConverter.createDuration("P3M");
        Duration clone = CloneUtil.clone(duration);
        assertEquals("Clone differs from the original", duration, clone);
        assertEquals("Clone string representation differs from the original", duration.toString(), clone.toString());
    }

    private Element createElement(String string) {
        Document doc = DOMUtil.getDocument();
        Element element = doc.createElementNS(MY_NS, MY_ELEMENT_NAME);
        element.setTextContent(string);
        return element;
    }

}
