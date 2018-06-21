/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.lex;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.displayTestTitle;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

/**
 * @author mederly
 */
@SuppressWarnings("Duplicates")
public abstract class AbstractJsonLexicalProcessorTest extends AbstractLexicalProcessorTest {

	private static final String OBJECTS_2_WRONG = "objects-2-wrong";
	private static final String OBJECTS_2_WRONG_2 = "objects-2-wrong-2";
	private static final String OBJECTS_9_LIST_SINGLE = "objects-9-list-single";
	private static final String OBJECTS_10_LIST_OF_LISTS = "objects-10-list-of-lists";

	@Test
	public void testParseObjectsIteratively_2_Wrong() throws Exception {
		final String TEST_NAME = "testParseObjectsIteratively_2_Wrong";

		displayTestTitle(TEST_NAME);

		// GIVEN
		LexicalProcessor<String> lexicalProcessor = createParser();

		// WHEN (parse to xnode)
		List<RootXNode> nodes = new ArrayList<>();
		try {
			lexicalProcessor.readObjectsIteratively(getFileSource(OBJECTS_2_WRONG), ParsingContext.createDefault(),
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

		nodes.forEach(n -> assertEquals("Wrong namespace", "", n.getRootElementName().getNamespaceURI()));
		assertEquals("Wrong namespace for node 1", "", getFirstElementNS(nodes, 0));
		assertEquals("Wrong namespace for node 2", "", getFirstElementNS(nodes, 1));
		assertEquals("Wrong namespace for node 3", "", getFirstElementNS(nodes, 2));

		// WHEN+THEN (parse in standard way)
		List<RootXNode> nodesStandard = lexicalProcessor.readObjects(getFileSource(OBJECTS_2_WRONG), ParsingContext.createDefault());

		System.out.println("Parsed objects (standard way):");
		System.out.println(DebugUtil.debugDump(nodesStandard));

		assertFalse("Nodes are not different", nodesStandard.equals(nodes));
	}

	@Test
	public void testParseObjectsIteratively_2_Wrong_2() throws Exception {
		final String TEST_NAME = "testParseObjectsIteratively_2_Wrong_2";

		displayTestTitle(TEST_NAME);

		// GIVEN
		LexicalProcessor<String> lexicalProcessor = createParser();

		// WHEN (parse to xnode)
		List<RootXNode> nodes = new ArrayList<>();
		try {
			lexicalProcessor.readObjectsIteratively(getFileSource(OBJECTS_2_WRONG_2), ParsingContext.createDefault(),
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

	@Test
	public void testParseObjectsIteratively_9_listSingle() throws Exception {
		final String TEST_NAME = "testParseObjectsIteratively_9_listSingle";

		standardTest(TEST_NAME, OBJECTS_9_LIST_SINGLE, 1);
	}

	@Test
	public void testParseObjectsIteratively_10_listOfLists() throws Exception {
		final String TEST_NAME = "testParseObjectsIteratively_10_listOfLists";

		standardTest(TEST_NAME, OBJECTS_10_LIST_OF_LISTS, 3);
	}

}
