/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.IOException;
import java.util.GregorianCalendar;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestCompare {
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	/**
	 * Parse the same files twice, compare the results.
	 */
	@Test
	public void testCompareJack() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testCompareJack ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
		Document document = DOMUtil.parseFile(USER_JACK_FILE);
		Element userElement = DOMUtil.getFirstChildElement(document);
		
		PrismObject<UserType> user1 = prismContext.parseObject(userElement);
		PrismObject<UserType> user2 = prismContext.parseObject(userElement);
		
		// WHEN, THEN
		
		assertTrue("Users not the same (PrismObject)(1)", user1.equals(user2));
		assertTrue("Users not the same (PrismObject)(2)", user2.equals(user1));
		
		// Following line won't work here. We don't have proper generated classes here. 
		// It is tested in the "schema" project that has a proper code generation
		// assertTrue("Users not the same (Objectable)", user1.asObjectable().equals(user2.asObjectable()));
		
		assertTrue("Users not equivalent (1)", user1.equivalent(user2));
		assertTrue("Users not equivalent (2)", user2.equivalent(user1));
	}
	
	/**
	 * Parse original jack and modified Jack. Diff and assert if the resulting
	 * delta is OK.
	 */
	@Test
	public void testDiffJack() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testDiffJack ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
		Document document = DOMUtil.parseFile(USER_JACK_FILE);
		Element userElement = DOMUtil.getFirstChildElement(document);		
		PrismObject<UserType> jackOriginal = prismContext.parseObject(userElement);
		
		document = DOMUtil.parseFile(USER_JACK_MODIFIED_FILE);
		userElement = DOMUtil.getFirstChildElement(document);		
		PrismObject<UserType> jackModified = prismContext.parseObject(userElement);
		
		// WHEN
		ObjectDelta<UserType> jackDelta = jackOriginal.diff(jackModified);
		
		// THEN
		System.out.println("Jack delta:");
		System.out.println(jackDelta.dump());
		
		jackDelta.assertDefinitions();
		jackDelta.checkConsistence(true);
		
		assertEquals("Wrong delta type", ChangeType.MODIFY, jackDelta.getChangeType());
		assertEquals("Wrong delta OID", USER_JACK_OID, jackDelta.getOid());
		assertEquals("Wrong number of modificaitions", 10, jackDelta.getModifications().size());
		
		PrismAsserts.assertPropertyReplace(jackDelta, USER_FULLNAME_QNAME, "Jack Sparrow");
		
		PrismAsserts.assertPropertyDelete(jackDelta, new PropertyPath(USER_EXTENSION_QNAME, USER_EXTENSION_MULTI_QNAME), "dva");
		PrismAsserts.assertPropertyAdd(jackDelta, new PropertyPath(USER_EXTENSION_QNAME, USER_EXTENSION_MULTI_QNAME), "osem");
		// TODO: assert BAR
		
		PrismAsserts.assertPropertyDelete(jackDelta, USER_ADDITIONALNAMES_QNAME, "Captain");
		
		PrismAsserts.assertPropertyAdd(jackDelta, USER_LOCALITY_QNAME, "World's End");
		
		PrismAsserts.assertPropertyReplace(jackDelta, USER_ENABLED_PATH, false);
		PrismAsserts.assertPropertyDelete(jackDelta, USER_VALID_FROM_PATH, USER_JACK_VALID_FROM);
		
		PrismAsserts.assertPropertyReplace(jackDelta, 
				new PropertyPath(
						new PropertyPathSegment(USER_ASSIGNMENT_QNAME,  USER_ASSIGNMENT_2_ID),
						new PropertyPathSegment(USER_DESCRIPTION_QNAME, null)), 
				"Assignment II");
		
		ContainerDelta<?> assignment3Delta = PrismAsserts.assertContainerAdd(jackDelta, new PropertyPath(USER_ASSIGNMENT_QNAME));
		PrismContainerValue<?> assignment3DeltaAddValue = assignment3Delta.getValuesToAdd().iterator().next();
		assertEquals("Assignment 3 wrong ID", USER_ASSIGNMENT_3_ID, assignment3DeltaAddValue.getId());
		
		// TODO assert assignment[i1112]/accountConstruction
	}
	
	@Test
	public void testDiffPatchRoundTrip() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testDiffPatchRoundTrip ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
		Document document = DOMUtil.parseFile(USER_JACK_FILE);
		Element userElement = DOMUtil.getFirstChildElement(document);		
		PrismObject<UserType> jackOriginal = prismContext.parseObject(userElement);
		
		document = DOMUtil.parseFile(USER_JACK_MODIFIED_FILE);
		userElement = DOMUtil.getFirstChildElement(document);		
		PrismObject<UserType> jackModified = prismContext.parseObject(userElement);
		
		ObjectDelta<UserType> jackDelta = jackOriginal.diff(jackModified);
		jackDelta.assertDefinitions();
		jackDelta.checkConsistence(true);
		
		// WHEN
		jackDelta.applyTo(jackOriginal);
		
		// THEN
		assertTrue("Roundtrip failed", jackOriginal.equivalent(jackModified));
	}
}
