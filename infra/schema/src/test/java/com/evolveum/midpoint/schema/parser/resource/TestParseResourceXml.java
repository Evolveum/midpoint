/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.schema.parser.resource;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.TestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class TestParseResourceXml extends TestParseResource {

	@Override
	protected String getLanguage() {
		return PrismContext.LANG_XML;
	}

	@Override
	protected String getFilenameSuffix() {
		return "xml";
	}

	@Test
	public void testParseResourceDom() throws Exception {
		final String TEST_NAME = "testParseResourceDom";
		PrismTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		DomLexicalProcessor parserDom = prismContext.getParserDom();
		XNode xnode = parserDom.read(getFile(TestConstants.RESOURCE_FILE_BASENAME), null);
		PrismObject<ResourceType> resource = prismContext.getXnodeProcessor().parseObject(xnode, ParsingContext.createDefault());

		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResource(resource, true, true, false);
	}

	@Test
	public void testParseResourceDomSimple() throws Exception {
		System.out.println("===[ testParseResourceDomSimple ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		Document document = DOMUtil.parseFile(getFile(TestConstants.RESOURCE_FILE_SIMPLE_BASENAME));
		Element resourceElement = DOMUtil.getFirstChildElement(document);

		// WHEN
		PrismObject<ResourceType> resource = prismContext.parserFor(resourceElement).parse();

		// THEN
		System.out.println("Parsed resource:");
		System.out.println(resource.debugDump());

		assertResource(resource, true, true, true);
	}

}
