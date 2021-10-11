/*
 * Copyright (c) 2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
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
