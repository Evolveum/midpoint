/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.lex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

@SuppressWarnings("Duplicates")
public abstract class DelegatingLexicalProcessorTest extends AbstractLexicalProcessorTest {

    private static final String OBJECTS_JSON_YAML_1_INCOMPLETE_LIST = "objects-json-yaml-1-incomplete-list";

    private static final String OBJECTS_2_WRONG_2 = "objects-2-wrong-2";
    private static final String OBJECTS_9_LIST_SINGLE = "objects-9-list-single";
    private static final String OBJECTS_10_LIST_OF_LISTS = "objects-10-list-of-lists";

    @Test
    public void testParseObjects_1_IncompleteList() throws Exception {
        // GIVEN
        LexicalProcessor<String> lexicalProcessor = createLexicalProcessor();

        // WHEN (parse to xnode)
        List<RootXNodeImpl> nodes = new ArrayList<>();
        try {
            lexicalProcessor.readObjectsIteratively(getFileSource(OBJECTS_JSON_YAML_1_INCOMPLETE_LIST), PrismTestUtil.createDefaultParsingContext(),
                    node -> {
                        nodes.add(node);
                        return true;
                    });
            fail("unexpected success");
        } catch (SchemaException e) {
            System.out.println("Got expected exception: " + e);
        }

        // THEN
        System.out.println("Parsed objects (iteratively):");
        System.out.println(DebugUtil.debugDump(nodes));

        // In JSON the parsing of second object does not finish successfully.
        // In YAML the data is formally OK but the parser complains because of object emptiness.
        // (This test is fragile anyway. In case of any problems just delete/disable it.)
        int expectedNodes = this instanceof TestJsonParser ? 1 : 2;
        assertEquals("Wrong # of nodes read", expectedNodes, nodes.size());
    }

    @Test(enabled = false)
    public void testParseObjectsIteratively_2_Wrong_2() throws Exception {
        // GIVEN
        LexicalProcessor<String> lexicalProcessor = createLexicalProcessor();

        // WHEN (parse to xnode)
        List<RootXNodeImpl> nodes = new ArrayList<>();
        try {
            lexicalProcessor.readObjectsIteratively(getFileSource(OBJECTS_2_WRONG_2), PrismTestUtil.createDefaultParsingContext(),
                    node -> {
                        nodes.add(node);
                        return true;
                    });
            fail("unexpected success");
        } catch (SchemaException e) {
            System.out.println("Got expected exception: " + e);
        }

        // THEN
        System.out.println("Parsed objects (iteratively):");
        System.out.println(DebugUtil.debugDump(nodes));

        assertEquals("Wrong # of nodes read", 3, nodes.size());
    }

    @Test(enabled = false)
    public void testParseObjectsIteratively_9_listSingle() throws Exception {
        executeReadObjectsTest(OBJECTS_9_LIST_SINGLE, 1);
    }

    @Test(enabled = false)
    public void testParseObjectsIteratively_10_listOfLists() throws Exception {
        executeReadObjectsTest(OBJECTS_10_LIST_OF_LISTS, 3);
    }
}
