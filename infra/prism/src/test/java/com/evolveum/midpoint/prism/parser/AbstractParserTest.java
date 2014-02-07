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
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.prism.xml.ns._public.types_2.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.ActivationType;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.ResourceType;
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
import com.evolveum.midpoint.util.DOMUtil;
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

		
		assertUserJackXNodeOrdering("serialized xnode", xnode);
		
		assertUserJack(user);		
		
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
		XNode serializedXNode = processor.serializeObject(user, true);
		String serializedString = parser.serializeToString(serializedXNode, new QName(NS_FOO, "user"));
		
//		try{
//			FileOutputStream out = new FileOutputStream(new File("D:/user-jack-prism.json"));
//			YamlParser jsonSer = new YamlParser();
//			String s = jsonSer.serializeToString((RootXNode) serializedXNode);
//			System.out.println("YAML: \n" + s);
//			
////			FileInputStream in = new FileInputStream(new File("D:/user-jack-prism.json"));
////			XNode afterJson = jsonSer.parseObject(in);
////			
////			// THEN
////					System.out.println("AFTER JSON XNode:");
////					System.out.println(afterJson.dump());
//			
//			} catch (Exception ex){
//				System.out.println( ex);
//				throw ex;
//			}
		
		// THEN
		System.out.println("\nXNode after re-serialization:");
		System.out.println(serializedXNode.dump());
		System.out.println("\nRe-serialized string:");
		System.out.println(serializedString);
		
		assertUserJackXNodeOrdering("serialized xnode", serializedXNode);
		
		validateUserSchema(serializedString, prismContext);
		
		// WHEN (re-parse)
		XNode reparsedXnode = parser.parse(serializedString);
		PrismObject<UserType> reparsedUser = processor.parseObject(reparsedXnode);
		
		// THEN
		System.out.println("\nXNode after re-parsing:");
		System.out.println(reparsedXnode.dump());
		System.out.println("\nRe-parsed user:");
		System.out.println(reparsedUser.dump());
		
		assertUserJackXNodeOrdering("serialized xnode", reparsedXnode);
				
		ObjectDelta<UserType> diff = DiffUtil.diff(user, reparsedUser);
		System.out.println("\nDiff:");
		System.out.println(diff.dump());
		
		PrismObject accountRefObjOrig = findObjectFromAccountRef(user);
		PrismObject accountRefObjRe = findObjectFromAccountRef(reparsedUser);
		
		ObjectDelta<UserType> accountRefObjDiff = DiffUtil.diff(accountRefObjOrig, accountRefObjRe);
		System.out.println("\naccountRef object diff:");
		System.out.println(accountRefObjDiff.dump());
		
		assertTrue("Re-parsed object in accountRef does not match: "+accountRefObjDiff, accountRefObjDiff.isEmpty());
		
		assertTrue("Re-parsed user does not match: "+diff, diff.isEmpty());
	}	
	
	@Test
    public void testParseResourceRumToPrism() throws Exception {
		final String TEST_NAME = "testParseResourceRumToPrism";
		displayTestTitle(TEST_NAME);
		
		// GIVEN
		Parser parser = createParser();
		XNodeProcessor processor = new XNodeProcessor();
		PrismContext prismContext = PrismTestUtil.getPrismContext();
		processor.setPrismContext(prismContext);
		
		// WHEN (parse to xnode)
		XNode xnode = parser.parse(getFile(RESOURCE_RUM_FILE_BASENAME));
		System.out.println("XNode after parsing:");
		System.out.println(xnode.dump());
		
		// WHEN (parse to prism)
		PrismObject<ResourceType> resource = processor.parseObject(xnode);
		
		// THEN
		System.out.println("Parsed user:");
		System.out.println(resource.dump());

		
		assertUserJackXNodeOrdering("serialized xnode", xnode);
		
		assertResourceRum(resource);		
		
	}


	private void assertResourceRum(PrismObject<ResourceType> resource) throws SchemaException {
		resource.checkConsistence();
		resource.assertDefinitions("test");
		
		assertEquals("Wrong oid", RESOURCE_RUM_OID, resource.getOid());
		PrismAsserts.assertObjectDefinition(resource.getDefinition(), RESOURCE_QNAME, RESOURCE_TYPE_QNAME, ResourceType.class);
		PrismAsserts.assertParentConsistency(resource);
		
		assertPropertyValue(resource, "name", new PolyString("Rum Delivery System", "rum delivery system"));
		assertPropertyDefinition(resource, "name", PolyStringType.COMPLEX_TYPE, 0, 1);
	}

	private PrismObject findObjectFromAccountRef(PrismObject<UserType> user) {
		for (PrismReferenceValue rval: user.findReference(UserType.F_ACCOUNT_REF).getValues()) {
			if (rval.getObject() != null) {
				return rval.getObject();
			}
		}
		return null;
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
	
	protected void assertUserJackXNodeOrdering(String message, XNode xnode) {
		if (xnode instanceof RootXNode) {
			xnode = ((RootXNode)xnode).getSubnode();
		}
		MapXNode xmap = getAssertXNode(message+": top", xnode, MapXNode.class);
		Set<Entry<QName, XNode>> reTopMapEntrySet = xmap.entrySet();
		Iterator<Entry<QName, XNode>> reTopMapEntrySetIter = reTopMapEntrySet.iterator();
		Entry<QName, XNode> reTopMapEntry0 = reTopMapEntrySetIter.next();
		assertEquals(message+": Wrong entry 0, the xnodes were shuffled", "oid", reTopMapEntry0.getKey().getLocalPart());
		Entry<QName, XNode> reTopMapEntry1 = reTopMapEntrySetIter.next();
		assertEquals(message+": Wrong entry 1, the xnodes were shuffled", "version", reTopMapEntry1.getKey().getLocalPart());
		Entry<QName, XNode> reTopMapEntry2 = reTopMapEntrySetIter.next();
		assertEquals(message+": Wrong entry 2, the xnodes were shuffled", UserType.F_NAME, reTopMapEntry2.getKey());
		Entry<QName, XNode> reTopMapEntry3 = reTopMapEntrySetIter.next();
		assertEquals(message+": Wrong entry 3, the xnodes were shuffled", UserType.F_DESCRIPTION, reTopMapEntry3.getKey());

	}
	
	protected void validateUserSchema(String dataString, PrismContext prismContext) throws SAXException, IOException {
		// Nothing to do by default
	}
}
