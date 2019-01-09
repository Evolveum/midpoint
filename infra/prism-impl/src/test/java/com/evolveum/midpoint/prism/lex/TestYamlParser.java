/*
 * Copyright (c) 2014-2019 Evolveum
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

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.lex.json.YamlLexicalProcessor;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.impl.xnode.RootXNodeImpl;
import com.evolveum.midpoint.util.DebugUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.displayTestTitle;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.createDefaultParsingContext;
import static org.testng.AssertJUnit.assertEquals;

public class TestYamlParser extends AbstractJsonLexicalProcessorTest {

	private static final String OBJECTS_8_MULTI_DOCUMENT = "objects-8-multi-document";

	@Override
	protected String getSubdirName() {
		return "yaml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "yaml";
	}

	@Override
	protected YamlLexicalProcessor createParser() {
		return new YamlLexicalProcessor(PrismTestUtil.getSchemaRegistry());
	}

	@Override
	protected String getWhenItemSerialized() {
		return "when: \"2012-02-24T10:48:52.000Z\"";
	}

	@Test
	public void testParseObjectsIteratively_8_multiDocument() throws Exception {
		final String TEST_NAME = "testParseObjectsIteratively_8_multiDocument";

		displayTestTitle(TEST_NAME);

		// GIVEN
		LexicalProcessor<String> lexicalProcessor = createParser();

		// WHEN (parse to xnode)
		List<RootXNodeImpl> nodes = new ArrayList<>();
		lexicalProcessor.readObjectsIteratively(getFileSource(OBJECTS_8_MULTI_DOCUMENT), createDefaultParsingContext(),
				node -> {
					nodes.add(node);
					return true;
				});

		// THEN
		System.out.println("Parsed objects (iteratively):");
		System.out.println(DebugUtil.debugDump(nodes));

		assertEquals("Wrong # of nodes read", 4, nodes.size());

		final String NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-" + PrismConstants.PRISM_MAJOR_VERSION;
		Iterator<RootXNodeImpl> i = nodes.iterator();
		assertEquals("Wrong namespace for node 1", NS_C, i.next().getRootElementName().getNamespaceURI());
		assertEquals("Wrong namespace for node 2", NS_C, i.next().getRootElementName().getNamespaceURI());
		assertEquals("Wrong namespace for node 3", "", i.next().getRootElementName().getNamespaceURI());
		assertEquals("Wrong namespace for node 4", "http://a/", i.next().getRootElementName().getNamespaceURI());

		// WHEN+THEN (parse in standard way)
		List<RootXNodeImpl> nodesStandard = lexicalProcessor.readObjects(getFileSource(OBJECTS_8_MULTI_DOCUMENT), createDefaultParsingContext());

		System.out.println("Parsed objects (standard way):");
		System.out.println(DebugUtil.debugDump(nodesStandard));

		assertEquals("Nodes are different", nodesStandard, nodes);
	}

}
