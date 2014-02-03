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
package com.evolveum.midpoint.prism.parser;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.USER_JACK_FILE_BASENAME;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.displayTestTitle;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.json.PrismJsonSerializer;
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
public class TestDomParser extends AbstractParserTest {
	
	@Override
	protected String getSubdirName() {
		return "xml";
	}

	@Override
	protected String getFilenameSuffix() {
		return "xml";
	}

	@Override
	protected DomParser createParser() {
		return new DomParser(PrismTestUtil.getSchemaRegistry());
	}

	@Test
    public void testParseUserToXNode() throws Exception {
		final String TEST_NAME = "testParseUserToXNode";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		DomParser parser = createParser();
		
		// WHEN
		XNode xnode = parser.parse(getFile(USER_JACK_FILE_BASENAME));
		
		// THEN
		System.out.println("Parsed XNode:");
		System.out.println(xnode.dump());

		RootXNode root = getAssertXNode("root node", xnode, RootXNode.class);
		
//		FileOutputStream out = new FileOutputStream(new File("D:/user-jack.json"));
//		PrismJsonSerializer jsonSer = new PrismJsonSerializer();
//		String s = jsonSer.serializeToString(xnode, new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "user"));
//		System.out.println("JSON: \n" + s);
//		
//		FileInputStream in = new FileInputStream(new File("D:/user-jack.json"));
//		XNode afterJson = jsonSer.parseObject(in);
//		
//		// THEN
//				System.out.println("AFTER JSON XNode:");
//				System.out.println(afterJson.dump());

		
		MapXNode rootMap = getAssertXNode("root subnode", root.getSubnode(), MapXNode.class);
		PrimitiveXNode<String> xname = getAssertXMapSubnode("root map", rootMap, UserType.F_NAME, PrimitiveXNode.class);
		// TODO: assert value
		
		ListXNode xass = getAssertXMapSubnode("root map", rootMap, UserType.F_ASSIGNMENT, ListXNode.class);
		assertEquals("assignment size", 2, xass.size());
		// TODO: asserts
		
		MapXNode xextension = getAssertXMapSubnode("root map", rootMap, UserType.F_EXTENSION, MapXNode.class);
		
		// WHEN (re-serialize)
//		parser.s
	}
}
