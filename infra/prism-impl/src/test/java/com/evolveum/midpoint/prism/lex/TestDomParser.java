/*
 * Copyright (c) 2014-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.lex;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.USER_JACK_FILE_BASENAME;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.createDefaultParsingContext;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.prism.ParserFileSource;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.util.DebugUtil;

import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.impl.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.MapXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;

/**
 * @author semancik
 *
 */
public class TestDomParser extends AbstractLexicalProcessorTest {

    private static final String OBJECTS_XML_1_NO_NS = "objects-xml-1-no-ns";
    private static final String OBJECTS_XML_2_NS = "objects-xml-2-ns";

    @Override
    protected String getSubdirName() {
        return "xml";
    }

    @Override
    protected String getFilenameSuffix() {
        return "xml";
    }

    @Override
    protected DomLexicalProcessor createLexicalProcessor() {
        return new DomLexicalProcessor(PrismTestUtil.getSchemaRegistry());
    }

    @Test
    public void testParseUserToXNode() throws Exception {
        // GIVEN
        DomLexicalProcessor parser = createLexicalProcessor();

        // WHEN
        XNodeImpl xnode = parser.read(new ParserFileSource(getFile(USER_JACK_FILE_BASENAME)), createDefaultParsingContext());

        // THEN
        System.out.println("Parsed XNode:");
        System.out.println(xnode.debugDump());

        RootXNodeImpl root = getAssertXNode("root node", xnode, RootXNodeImpl.class);

        MapXNodeImpl rootMap = getAssertXNode("root subnode", root.getSubnode(), MapXNodeImpl.class);
        PrimitiveXNodeImpl<String> xname = getAssertXMapSubnode("root map", rootMap, UserType.F_NAME, PrimitiveXNodeImpl.class);
        // TODO: assert value

        ListXNodeImpl xass = getAssertXMapSubnode("root map", rootMap, UserType.F_ASSIGNMENT, ListXNodeImpl.class);
        assertEquals("assignment size", 3, xass.size());        // one is IncompleteMarker
        // TODO: asserts

        MapXNodeImpl xextension = getAssertXMapSubnode("root map", rootMap, UserType.F_EXTENSION, MapXNodeImpl.class);

    }

    @Test
    public void testParseObjects_xml_1_NoNs() throws Exception {
        // GIVEN
        LexicalProcessor<String> lexicalProcessor = createLexicalProcessor();

        // WHEN (parse to xnode)
        List<RootXNodeImpl> nodes = new ArrayList<>();
        lexicalProcessor.readObjectsIteratively(getFileSource(OBJECTS_XML_1_NO_NS), createDefaultParsingContext(),
                node -> {
                    nodes.add(node);
                    return true;
                });

        // THEN
        System.out.println("Parsed objects (iteratively):");
        System.out.println(DebugUtil.debugDump(nodes));

        assertEquals("Wrong # of nodes read", 3, nodes.size());

        nodes.forEach(n -> assertEquals("Wrong namespace",
                "", n.getRootElementName().getNamespaceURI()));
        assertEquals("Wrong namespace for node 1", "http://b/", getFirstElementNS(nodes, 0));
        assertEquals("Wrong namespace for node 2", "http://c/", getFirstElementNS(nodes, 1));
        assertEquals("Wrong namespace for node 3", "http://d/", getFirstElementNS(nodes, 2));

        // WHEN+THEN (parse in standard way)
        List<RootXNodeImpl> nodesStandard = lexicalProcessor.readObjects(getFileSource(OBJECTS_XML_1_NO_NS), createDefaultParsingContext());

        System.out.println("Parsed objects (standard way):");
        System.out.println(DebugUtil.debugDump(nodesStandard));

        assertEquals("Nodes are different", nodesStandard, nodes);
    }

    @Test
    public void testParseObjects_xml_2_Ns() throws Exception {
        // GIVEN
        LexicalProcessor<String> lexicalProcessor = createLexicalProcessor();

        // WHEN (parse to xnode)
        List<RootXNodeImpl> nodes = new ArrayList<>();
        lexicalProcessor.readObjectsIteratively(getFileSource(OBJECTS_XML_2_NS), createDefaultParsingContext(),
                node -> {
                    nodes.add(node);
                    return true;
                });

        // THEN
        System.out.println("Parsed objects (iteratively):");
        System.out.println(DebugUtil.debugDump(nodes));

        assertEquals("Wrong # of nodes read", 3, nodes.size());

        nodes.forEach(n -> assertEquals("Wrong namespace",
                "http://a/", n.getRootElementName().getNamespaceURI()));
        assertEquals("Wrong namespace for node 1", "http://b/", getFirstElementNS(nodes, 0));
        assertEquals("Wrong namespace for node 2", "http://c/", getFirstElementNS(nodes, 1));
        assertEquals("Wrong namespace for node 3", "http://d/", getFirstElementNS(nodes, 2));

        // WHEN+THEN (parse in standard way)
        List<RootXNodeImpl> nodesStandard = lexicalProcessor.readObjects(getFileSource(OBJECTS_XML_2_NS), createDefaultParsingContext());

        System.out.println("Parsed objects (standard way):");
        System.out.println(DebugUtil.debugDump(nodesStandard));

        assertEquals("Nodes are different", nodesStandard, nodes);
    }


    private void validateSchemaCompliance(String xmlString, PrismContext prismContext)  throws SAXException, IOException {
//        Document xmlDocument = DOMUtil.parseDocument(xmlString);
//        Schema javaxSchema = prismContext.getSchemaRegistry().getJavaxSchema();
//        Validator validator = javaxSchema.newValidator();
//        validator.setResourceResolver(prismContext.getEntityResolver());
//        validator.validate(new DOMSource(xmlDocument));
    }

    @Override
    protected void validateUserSchema(String xmlString, PrismContext prismContext) throws SAXException, IOException {
        validateSchemaCompliance(xmlString, prismContext);
    }

    @Override
    protected void validateResourceSchema(String xmlString, PrismContext prismContext) throws SAXException, IOException {
        validateSchemaCompliance(xmlString, prismContext);
    }

    @Override
    protected String getWhenItemSerialized() {
        return "<when>2012-02-24T10:48:52.000Z</when>";
    }
}
