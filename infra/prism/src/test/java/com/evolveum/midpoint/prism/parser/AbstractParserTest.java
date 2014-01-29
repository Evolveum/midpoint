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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.xml.namespace.QName;

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
import com.evolveum.midpoint.prism.json.PrismJsonSerializer;
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
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public abstract class AbstractParserTest {
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	protected abstract String getSubdirName();
	
	protected abstract String getFilenameSuffix();
	
	protected File getCommonSubdir() {
		return new File(COMMON_DIR_PATH, getSubdirName());
	}
	
	protected File getFile(String baseName) {
		return new File(getCommonSubdir(), baseName+"."+getFilenameSuffix());
	}
	
	protected abstract Parser createParser();
	

	@Test
    public void testParseUserToPrism() throws Exception {
		final String TEST_NAME = "testParseUserToPrism";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Parser parser = createParser();
		XNodeProcessor processor = new XNodeProcessor();
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		processor.setPrismContext(prismContext);
		
		// WHEN (parse to xnode)
		XNode xnode = parser.parse(getFile(USER_JACK_FILE_BASENAME));
		System.out.println("XNode after parsing:");
		System.out.println(xnode.dump());
		
		
		// WHEN (parse to prism)
		PrismObject<UserType> user = processor.parseObject(xnode);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(user.dump());

		
		
		assertUserJack(user);
		
		// WHEN (re-serialize to XNode)
		XNode serializedXNode = processor.serializeObject(user);
		try{
		FileOutputStream out = new FileOutputStream(new File("D:/user-jack-prism.json"));
		PrismJsonSerializer jsonSer = new PrismJsonSerializer();
		String s = jsonSer.serializeToString(serializedXNode, new QName("http://midpoint.evolveum.com/xml/ns/test/foo-1.xsd", "user"));
		System.out.println("JSON: \n" + s);
		
		FileInputStream in = new FileInputStream(new File("D:/user-jack-prism.json"));
		XNode afterJson = jsonSer.parseObject(in);
		
		// THEN
				System.out.println("AFTER JSON XNode:");
				System.out.println(afterJson.dump());
		
		} catch (Exception ex){
			System.out.println( ex);
			throw ex;
		}
		// THEN
		System.out.println("XNode after re-serialization:");
		System.out.println(serializedXNode.dump());
	}

	@Test
    public void testParseUserRoundTrip() throws Exception {
		final String TEST_NAME = "testParseUserRoundTrip";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Parser parser = createParser();
		XNodeProcessor processor = new XNodeProcessor();
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		processor.setPrismContext(prismContext);
		
		// WHEN (parse)
		XNode xnode = parser.parse(getFile(USER_JACK_FILE_BASENAME));
		PrismObject<UserType> user = processor.parseObject(xnode);
		
		// THEN
		System.out.println("\nParsed user:");
		System.out.println(user.dump());
		
		assertUserJack(user);
		
		// WHEN (re-serialize to XNode)
		XNode serializedXNode = processor.serializeObject(user);
		String serializedString = parser.serializeToString(serializedXNode, new QName(NS_FOO, "user"));
		
		// THEN
		System.out.println("\nXNode after re-serialization:");
		System.out.println(serializedXNode.dump());
		System.out.println("\nRe-serialized string:");
		System.out.println(serializedString);
		
		// WHEN (re-parse)
		XNode reparsedXnode = parser.parse(serializedString);
		PrismObject<UserType> reparsedUser = processor.parseObject(reparsedXnode);
		
		// THEN
		System.out.println("\nRe-parsed user:");
		System.out.println(reparsedUser.dump());
		
		ObjectDelta<UserType> diff = DiffUtil.diff(user, reparsedUser);
		System.out.println("\nDiff:");
		System.out.println(diff.dump());
		
		assertTrue("Re-parsed user does not mathc: "+diff, diff.isEmpty());
	}

	protected <X extends XNode> X getAssertXNode(String message, XNode xnode, Class<X> expectedClass) {
		assertNotNull(message+" is null", xnode);
		assertTrue(message+", expected "+expectedClass.getSimpleName()+", was "+xnode.getClass().getSimpleName(),
				expectedClass.isAssignableFrom(xnode.getClass()));
		return (X) xnode;
	}
	
	protected <X extends XNode> X getAssertXMapSubnode(String message, MapXNode xmap, QName key, Class<X> expectedClass) {
		XNode xsubnode = xmap.get(key);
		assertNotNull(message+" no key "+key, xsubnode);
		return getAssertXNode(message+" key "+key, xsubnode, expectedClass);
	}
}
