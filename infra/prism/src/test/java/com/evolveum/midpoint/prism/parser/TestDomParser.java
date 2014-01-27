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

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.prism.xml.ns._public.types_2.ObjectReferenceType;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ActivationType;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

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
	protected DOMParser createParser() {
		return new DOMParser(PrismTestUtil.getSchemaRegistry());
	}

	@Test
    public void testParseUserToXNode() throws Exception {
		final String TEST_NAME = "testParseUserToXNode";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		DOMParser parser = createParser();
		
		// WHEN
		XNode xnode = parser.parse(getFile(USER_JACK_FILE_BASENAME));
		
		// THEN
		System.out.println("Parsed XNode:");
		System.out.println(xnode.dump());

		assertTrue("Root node is "+xnode.getClass(), xnode instanceof RootXNode);
		RootXNode root = getAssertXNode("root node", xnode, RootXNode.class);
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
