/*
 * Copyright (c) 2014 Evolveum
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

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.USER_JACK_FILE_BASENAME;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.displayTestTitle;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;

import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.lex.dom.DomLexicalProcessor;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;

/**
 * @author semancik
 *
 */
public class TestDomParser extends AbstractLexicalProcessorTest {

	@Override
	protected String getSubdirName() {
		return "xml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "xml";
	}

	@Override
	protected DomLexicalProcessor createParser() {
		return new DomLexicalProcessor(PrismTestUtil.getSchemaRegistry());
	}

	@Test
    public void testParseUserToXNode() throws Exception {
		final String TEST_NAME = "testParseUserToXNode";
		displayTestTitle(TEST_NAME);

		// GIVEN
		DomLexicalProcessor parser = createParser();

		// WHEN
		XNode xnode = parser.read(getFile(USER_JACK_FILE_BASENAME), ParsingContext.createDefault());

		// THEN
		System.out.println("Parsed XNode:");
		System.out.println(xnode.debugDump());

		RootXNode root = getAssertXNode("root node", xnode, RootXNode.class);

		MapXNode rootMap = getAssertXNode("root subnode", root.getSubnode(), MapXNode.class);
		PrimitiveXNode<String> xname = getAssertXMapSubnode("root map", rootMap, UserType.F_NAME, PrimitiveXNode.class);
		// TODO: assert value

		ListXNode xass = getAssertXMapSubnode("root map", rootMap, UserType.F_ASSIGNMENT, ListXNode.class);
		assertEquals("assignment size", 2, xass.size());
		// TODO: asserts

		MapXNode xextension = getAssertXMapSubnode("root map", rootMap, UserType.F_EXTENSION, MapXNode.class);

	}

	private void validateSchemaCompliance(String xmlString, PrismContext prismContext)  throws SAXException, IOException {
//		Document xmlDocument = DOMUtil.parseDocument(xmlString);
//		Schema javaxSchema = prismContext.getSchemaRegistry().getJavaxSchema();
//		Validator validator = javaxSchema.newValidator();
//		validator.setResourceResolver(prismContext.getEntityResolver());
//		validator.validate(new DOMSource(xmlDocument));
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
