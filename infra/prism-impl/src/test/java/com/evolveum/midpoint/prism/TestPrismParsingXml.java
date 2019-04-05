/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.util.DOMUtil;

public class TestPrismParsingXml extends TestPrismParsing {

	@Override
	protected String getSubdirName() {
		return "xml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "xml";
	}

	@Override
	protected String getOutputFormat() {
		return PrismContext.LANG_XML;
	}

	@Test
	public void testPrismParseDom() throws Exception {
		final String TEST_NAME = "testPrismParseDom";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Document document = DOMUtil.parseFile(getFile(USER_JACK_FILE_BASENAME));
		Element userElement = DOMUtil.getFirstChildElement(document);

		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parserFor(userElement).parse();

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserJack(user, true);
	}

	@Test
	public void testPrismParseDomAdhoc() throws Exception {
		final String TEST_NAME = "testPrismParseDomAdhoc";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		Document document = DOMUtil.parseFile(getFile(USER_JACK_ADHOC_BASENAME));
		Element userElement = DOMUtil.getFirstChildElement(document);

		PrismContext prismContext = constructInitializedPrismContext();

		// WHEN
		PrismObject<UserType> user = prismContext.parserFor(userElement).parse();

		// THEN
		System.out.println("User:");
		System.out.println(user.debugDump());
		assertNotNull(user);

		assertUserAdhoc(user, true);
	}

	@Override
	protected void validateXml(String xmlString, PrismContext prismContext) throws SAXException, IOException {
//		Document xmlDocument = DOMUtil.parseDocument(xmlString);
//		Schema javaxSchema = prismContext.getSchemaRegistry().getJavaxSchema();
//		Validator validator = javaxSchema.newValidator();
//		validator.setResourceResolver(prismContext.getEntityResolver());
//		validator.validate(new DOMSource(xmlDocument));
	}
}
