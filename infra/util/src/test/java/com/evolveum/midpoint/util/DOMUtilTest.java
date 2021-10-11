/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;
import static org.testng.AssertJUnit.*;

import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * @author Radovan Semancik
 */
public class DOMUtilTest extends AbstractUnitTest {

    private static final String QNAME_IN_NS = "http://foo.com/bar";
    private static final String QNAME_IN_LOCAL = "baz";
    private static final String ELEMENT_NS = "http://foo.com/barbar";
    private static final String ELEMENT_LOCAL = "el";
    private static final String DEFAULT_NS = "http://foo.com/default";
    private static final String ELEMENT_TOP_LOCAL = "top";
    private static final String FOO_NS = "http://foo.com/foo";
    private static final QName QNAME_ATTR_QNAME = new QName(ELEMENT_NS, "qname");

    private static final String XSD_TYPE_FILENAME = "src/test/resources/domutil/xsi-type.xml";
    private static final String WHITESPACES_FILENAME = "src/test/resources/domutil/whitespaces.xml";
    private static final String QNAMES_FILENAME = "src/test/resources/domutil/qnames.xml";
    private static final String FIX_NAMESPACE_FILENAME = "src/test/resources/domutil/fix-namespace.xml";

    public static final String NS_W3C_XML_SCHEMA_PREFIX = "xsd";
    public static final QName XSD_STRING = new QName(W3C_XML_SCHEMA_NS_URI, "string",
            NS_W3C_XML_SCHEMA_PREFIX);
    public static final QName XSD_INTEGER = new QName(W3C_XML_SCHEMA_NS_URI, "integer",
            NS_W3C_XML_SCHEMA_PREFIX);
    private static final int MAX_GENERATE_QNAME_ITERATIONS = 200;

    public DOMUtilTest() {
    }

    @Test
    public void testQNameRoundTrip() {
        // GIVEN
        Document doc = DOMUtil.getDocument();

        QName in = new QName(QNAME_IN_NS, QNAME_IN_LOCAL, "x");
        Element e = doc.createElementNS(ELEMENT_NS, ELEMENT_LOCAL);

        // WHEN

        DOMUtil.setQNameValue(e, in);

        // THEN

        System.out.println(DOMUtil.serializeDOMToString(e));

        String content = e.getTextContent();
        String[] split = content.split(":");
        // Default namespace should not be used unless explicitly matches existing declaration
        // therefore there should be a prefix
        AssertJUnit.assertEquals(2, split.length);
        String prefix = split[0];
        String localPart = split[1];
        AssertJUnit.assertFalse(prefix.isEmpty());
        String namespaceURI = e.lookupNamespaceURI(prefix);
        AssertJUnit.assertEquals(QNAME_IN_NS, namespaceURI);
        AssertJUnit.assertEquals(QNAME_IN_LOCAL, localPart);

        // WHEN

        QName out = DOMUtil.getQNameValue(e);

        // THEN

        AssertJUnit.assertEquals(in, out);
    }

    @Test
    public void testQNameDefaultNamespace1() {
        // GIVEN
        Document doc = DOMUtil.getDocument();

        QName in = new QName(DEFAULT_NS, QNAME_IN_LOCAL);
        Element topElement = doc.createElementNS(DEFAULT_NS, ELEMENT_TOP_LOCAL);
        // Make sure there is a default ns declaration
        DOMUtil.setNamespaceDeclaration(topElement, "", DEFAULT_NS);
        DOMUtil.setNamespaceDeclaration(topElement, "e", ELEMENT_NS);
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
        // Default namespace should NOT be reused
        AssertJUnit.assertTrue(content.contains(":"));
        AssertJUnit.assertTrue(content.contains(":" + QNAME_IN_LOCAL));
    }

    @Test
    public void testQNameDefaultNamespace2() {
        // GIVEN
        Document doc = DOMUtil.getDocument();

        QName in = new QName(DEFAULT_NS, QNAME_IN_LOCAL, "f");          // the difference w.r.t. testQNameDefaultNamespace1
        Element topElement = doc.createElementNS(DEFAULT_NS, ELEMENT_TOP_LOCAL);
        // Make sure there is a default ns declaration
        DOMUtil.setNamespaceDeclaration(topElement, "", DEFAULT_NS);
        DOMUtil.setNamespaceDeclaration(topElement, "e", ELEMENT_NS);
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
        // Default namespace should NOT be reused
        AssertJUnit.assertEquals("f:" + QNAME_IN_LOCAL, content);
    }

    @Test
    public void testQNameNoNamespace() {
        // GIVEN
        Document doc = DOMUtil.getDocument();

        QName in = new QName(XMLConstants.NULL_NS_URI, QNAME_IN_LOCAL);                 // no namespace
        Element topElement = doc.createElementNS(DEFAULT_NS, ELEMENT_TOP_LOCAL);
        // Make sure there is a default ns declaration
        DOMUtil.setNamespaceDeclaration(topElement, "", DEFAULT_NS);
        DOMUtil.setNamespaceDeclaration(topElement, "e", ELEMENT_NS);
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
        // There should be no namespace prefix
        AssertJUnit.assertEquals(QNAME_IN_LOCAL, content);
    }

    @Test
    public void testXsiType() {
        // GIVEN
        Document doc = DOMUtil.parseFile(XSD_TYPE_FILENAME);
        Element root = DOMUtil.getFirstChildElement(doc);
        Element el1 = DOMUtil.getFirstChildElement(root);

        // WHEN
        QName xsiType = DOMUtil.resolveXsiType(el1);

        // THEN
        assertNotNull(xsiType);
        assertEquals(xsiType, XSD_INTEGER);

        AssertJUnit.assertTrue("Failed to detect xsi:type", DOMUtil.hasXsiType(el1));

    }

    @Test
    public void testWhitespaces() {
        // GIVEN
        Document doc = DOMUtil.parseFile(WHITESPACES_FILENAME);

        // WHEN
        String serialized = DOMUtil.serializeDOMToString(doc);
        System.out.println(serialized);

        // THEN
        Node firstChild = doc.getDocumentElement().getFirstChild();
        System.out.println("firstChild: " + firstChild);
        Node firstChildSibling = firstChild.getNextSibling();
        System.out.println("firstChildSibling: " + firstChildSibling);
        //assertTrue("First child should be Element, it is " + firstChild, firstChild instanceof Element);

        int lines = countLines(serialized);
        assertTrue("Too many lines: " + lines, lines < 20);
    }

    @Test
    public void testFormatting() {
        // GIVEN
        Document doc = DOMUtil.getDocument();
        Element root = DOMUtil.createElement(doc, new QName("root"));
        doc.appendChild(root);
        Element child1 = DOMUtil.createSubElement(root, new QName("child1"));
        Element child2 = DOMUtil.createSubElement(root, new QName("child2"));
        Element child3 = DOMUtil.createSubElement(root, new QName("child3"));
        child1.setTextContent("text1");
        child2.setTextContent("text2");
        child3.setTextContent("text3");

        // WHEN
        String serialized = DOMUtil.serializeDOMToString(doc);
        System.out.println(serialized);

        // THEN

        int lines = countLines(serialized);
        assertEquals("Wrong # of lines", 5, lines);
        assertTrue("Missing indentation", serialized.contains("    <child1>"));
        //assertTrue("Too many lines: " + lines, lines < 20);
    }

    private int countLines(String s) {
        return s.split("\r\n|\r|\n").length;
    }

    @Test
    public void testQNameMethods() {
        Document doc = DOMUtil.parseFile(QNAMES_FILENAME);
        Element root = DOMUtil.getFirstChildElement(doc);

        Element el1 = (Element) root.getElementsByTagNameNS(DEFAULT_NS, "el1").item(0);
        QName refAttrValue = DOMUtil.getQNameAttribute(el1, "ref");
        assertEquals("getQNameAttribute failed", new QName(FOO_NS, "bar"), refAttrValue);

        Element el2 = (Element) root.getElementsByTagNameNS(DEFAULT_NS, "el2").item(0);
        QName el2Value = DOMUtil.getQNameValue(el2);
        assertEquals("getQNameValue failed", new QName(FOO_NS, "BAR"), el2Value);
    }

    @Test
    public void testFixNamespaceDeclarations() {
        Document doc = DOMUtil.parseFile(FIX_NAMESPACE_FILENAME);

        System.out.println("Original XML:");
        System.out.println(DOMUtil.serializeDOMToString(doc));

        Element root = DOMUtil.getFirstChildElement(doc);

        Element target = (Element) root.getElementsByTagNameNS(DEFAULT_NS, "target").item(0);

        DOMUtil.fixNamespaceDeclarations(target);

        System.out.println("Fixed namespaces (doc):");
        System.out.println(DOMUtil.serializeDOMToString(doc));

        System.out.println("Fixed namespaces (target):");
        System.out.println(DOMUtil.serializeDOMToString(target));

        Map<String, String> decls = DOMUtil.getNamespaceDeclarations(target);
        assertEquals("bar decl", "http://foo.com/bar", decls.get("bar"));
        assertEquals("foo decl", "http://foo.com/foo", decls.get("foo"));
        assertEquals("default decl", "http://foo.com/default", decls.get(null));
    }

    @Test
    public void testGeneratedQNamePrefixes() {
        // GIVEN
        Document doc = DOMUtil.getDocument();

        Element topElement = doc.createElementNS(ELEMENT_NS, ELEMENT_TOP_LOCAL);
        doc.appendChild(topElement);

        System.out.println("Before");
        System.out.println(DOMUtil.serializeDOMToString(topElement));

        // WHEN
        for (int i = 0; i < MAX_GENERATE_QNAME_ITERATIONS; i++) {
            Element el = DOMUtil.createElement(doc, new QName(ELEMENT_NS, ELEMENT_LOCAL + i), topElement, topElement);
            topElement.appendChild(el);
            String namespace = "http://exaple.com/gen/" + i;
            QName qNameValue = new QName(namespace, "val" + i);
            DOMUtil.setQNameAttribute(el, QNAME_ATTR_QNAME, qNameValue, topElement);

            QName qNameValueAfter = DOMUtil.getQNameAttribute(el, QNAME_ATTR_QNAME);
            assertEquals("Bada boom! wrong element QName in iteration " + i, qNameValue, qNameValueAfter);
        }

        // THEN
        System.out.println("After");
        System.out.println(DOMUtil.serializeDOMToString(topElement));

        for (int i = 0; i < MAX_GENERATE_QNAME_ITERATIONS; i++) {
            Element el = DOMUtil.getChildElement(topElement, ELEMENT_LOCAL + i);
            String namespace = "http://exaple.com/gen/" + i;
            QName qNameValue = new QName(namespace, "val" + i);
            QName qNameValueAfter = DOMUtil.getQNameAttribute(el, QNAME_ATTR_QNAME);
            assertEquals("BIG bada boom! wrong element QName in iteration " + i, qNameValue, qNameValueAfter);
        }
    }

    @Test
    public void testSupportStringWithSurrogates() {
        //unicode block "CJK Unified Ideographs Extension B"
        String targetString = "\ud864\udd66";
        boolean support = true;
        assertEquals("length is not 2", 2, targetString.length());
        assertEquals("codePoint count is not 1", 1, targetString.codePointCount(0, targetString.length()));
        try {
            DOMUtil.checkValidXmlChars(targetString);
        } catch (IllegalStateException e) {
            support = false;
        }
        assertTrue("Not support string with surrogates in xml", support);
    }

}
