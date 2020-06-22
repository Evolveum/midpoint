/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.json.*;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.DebugUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.createDefaultParsingContext;
import static org.testng.AssertJUnit.assertEquals;

public class TestYamlParser extends DelegatingLexicalProcessorTest {

    private static final String OBJECTS_YAML_1_MULTI_DOCUMENT = "objects-yaml-1-multi-document";

    @Override
    protected String getSubdirName() {
        return "yaml";
    }

    @Override
    protected String getFilenameSuffix() {
        return "yaml";
    }

    @Override
    protected LexicalProcessor<String> createLexicalProcessor() {
        return new DelegatingLexicalProcessor(
                new YamlReader(PrismTestUtil.getSchemaRegistry()),
                new YamlWriter());
    }

    @Override
    protected String getWhenItemSerialized() {
        return "when: \"2012-02-24T10:48:52.000Z\"";
    }

    @Test
    public void testParseObjects_yaml_1_MultiDocument() throws Exception {
        // GIVEN
        LexicalProcessor<String> lexicalProcessor = createLexicalProcessor();

        // WHEN (parse to xnode)
        List<RootXNodeImpl> nodes = new ArrayList<>();
        lexicalProcessor.readObjectsIteratively(getFileSource(OBJECTS_YAML_1_MULTI_DOCUMENT), createDefaultParsingContext(),
                node -> {
                    nodes.add(node);
                    return true;
                });

        // THEN
        System.out.println("Parsed objects (iteratively):");
        System.out.println(DebugUtil.debugDump(nodes));

        assertEquals("Wrong # of nodes read", 4, nodes.size());

        final String NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
        Iterator<RootXNodeImpl> i = nodes.iterator();
        assertEquals("Wrong namespace for node 1", NS_C, i.next().getRootElementName().getNamespaceURI());
        assertEquals("Wrong namespace for node 2", NS_C, i.next().getRootElementName().getNamespaceURI());
        assertEquals("Wrong namespace for node 3", "", i.next().getRootElementName().getNamespaceURI());
        assertEquals("Wrong namespace for node 4", "http://a/", i.next().getRootElementName().getNamespaceURI());

        // WHEN+THEN (parse in standard way)
        List<RootXNodeImpl> nodesStandard = lexicalProcessor.readObjects(getFileSource(OBJECTS_YAML_1_MULTI_DOCUMENT), createDefaultParsingContext());

        System.out.println("Parsed objects (standard way):");
        System.out.println(DebugUtil.debugDump(nodesStandard));

        assertEquals("Nodes are different", nodesStandard, nodes);
    }
}
