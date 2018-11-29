/*
 * Copyright (c) 2010-2013 Evolveum
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

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;

import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.AccountConstructionType;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestFind {

	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}

	@Test
	public void testFindString() throws SchemaException, SAXException, IOException {
		final String TEST_NAME = "testFindString";
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		PrismObject<UserType> user = createUser();
		UniformItemPath path = getPrismContext().path(UserType.F_DESCRIPTION);

		// WHEN
		PrismProperty<String> nameProperty = findProperty(user, path);

		// THEN
		assertEquals("Wrong property value (path="+path+")", USER_JACK_DESCRIPTION, nameProperty.getRealValue());
		assertTrue("QName found something other", nameProperty == (PrismProperty) user.findProperty(UserType.F_DESCRIPTION));
	}

	@Test
	public void testFindPolyString() throws SchemaException, SAXException, IOException {
		final String TEST_NAME = "testFindPolyString";
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		PrismObject<UserType> user = createUser();
		UniformItemPath path = getPrismContext().path(UserType.F_POLY_NAME);

		// WHEN
		PrismProperty<PolyString> nameProperty = findProperty(user, path);

		// THEN
		assertEquals("Wrong property value (path="+path+")", PrismTestUtil.createPolyString(USER_JACK_POLYNAME_ORIG), nameProperty.getRealValue());
		assertTrue("QName found something other", nameProperty == (PrismProperty) user.findProperty(UserType.F_POLY_NAME));
	}

	@Test
	public void testFindPolyStringOrig() throws SchemaException, SAXException, IOException {
		final String TEST_NAME = "testFindPolyStringOrig";
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		UniformItemPath path = getPrismContext().path(UserType.F_POLY_NAME, PolyString.F_ORIG);

		// WHEN
		Object found = findUser(path);

		// THEN
		assertEquals("Wrong property value (path="+path+")", USER_JACK_POLYNAME_ORIG, found);
	}

	@Test
	public void testFindPolyStringNorm() throws SchemaException, SAXException, IOException {
		final String TEST_NAME = "testFindPolyStringNorm";
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		UniformItemPath path = getPrismContext().path(UserType.F_POLY_NAME, PolyString.F_NORM);

		// WHEN
		Object found = findUser(path);

		// THEN
		assertEquals("Wrong property value (path="+path+")", USER_JACK_POLYNAME_NORM, found);
	}

	@Test
	public void testFindExtensionBar() throws SchemaException, SAXException, IOException {
		final String TEST_NAME = "testFindExtensionBar";
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		UniformItemPath path = getPrismContext().path(UserType.F_EXTENSION, EXTENSION_BAR_ELEMENT);

		// WHEN
		PrismProperty<String> property = findUserProperty(path);

		// THEN
		assertEquals("Wrong property value (path="+path+")", "BAR", property.getAnyRealValue());
	}

	@Test
	public void testFindAssignment1Description() throws SchemaException, SAXException, IOException {
		final String TEST_NAME = "testFindAssignment1Description";
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		UniformItemPath path = getPrismContext().path(UserType.F_ASSIGNMENT, USER_ASSIGNMENT_1_ID, AssignmentType.F_DESCRIPTION);

		// WHEN
		PrismProperty<String> property = findUserProperty(path);

		// THEN
		assertEquals("Wrong property value (path="+path+")", "Assignment 1", property.getRealValue());
	}

	@Test
	public void testFindAssignment2Construction() throws SchemaException, SAXException, IOException {
		final String TEST_NAME = "testFindAssignment2ConstructionHowto";
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		UniformItemPath path = getPrismContext().path(UserType.F_ASSIGNMENT, USER_ASSIGNMENT_2_ID, AssignmentType.F_ACCOUNT_CONSTRUCTION);

		// WHEN
		PrismProperty<AccountConstructionType> property = findUserProperty(path);

		// THEN
		assertEquals("Wrong property value (path="+path+")", "Just do it", property.getRealValue().getHowto());
	}

	@Test
	public void testFindAssignment() throws SchemaException, SAXException, IOException {
		final String TEST_NAME = "testFindAssignment";
		System.out.println("===[ "+TEST_NAME+" ]===");

		// GIVEN
		UniformItemPath path = getPrismContext().path(UserType.F_ASSIGNMENT);

		// WHEN
		PrismContainer<AssignmentType> container = findUserContainer(path);

		// THEN
		PrismContainerValue<AssignmentType> value2 = container.getValue(USER_ASSIGNMENT_2_ID);
		assertEquals("Wrong value2 description (path="+path+")", "Assignment 2", value2.findProperty(AssignmentType.F_DESCRIPTION).getRealValue());
	}

	private <T> T findUser(UniformItemPath path) throws SchemaException, SAXException, IOException {
		PrismObject<UserType> user = createUser();
		return find(user, path);
	}

	private <T> T find(PrismObject<UserType> user, UniformItemPath path) {
		System.out.println("Path:");
		System.out.println(path);

		// WHEN
		Object found = user.find(path);

		// THEN
		System.out.println("Found:");
		System.out.println(found);
		return (T) found;
	}

	private <T> PrismProperty<T> findUserProperty(UniformItemPath path) throws SchemaException, SAXException, IOException {
		PrismObject<UserType> user = createUser();
		return findProperty(user, path);
	}

	private <T> PrismProperty<T> findProperty(PrismObject<UserType> user, UniformItemPath path) {
		System.out.println("Path:");
		System.out.println(path);

		// WHEN
		PrismProperty<T> property = user.findProperty(path);

		// THEN
		System.out.println("Found:");
		System.out.println(property);
		return property;
	}

	private <T extends Containerable> PrismContainer<T> findUserContainer(UniformItemPath path) throws SchemaException, SAXException, IOException {
		PrismObject<UserType> user = createUser();
		return findContainer(user, path);
	}

	private <T extends Containerable> PrismContainer<T> findContainer(PrismObject<UserType> user, UniformItemPath path) {
		System.out.println("Path:");
		System.out.println(path);

		// WHEN
		PrismContainer<T> container = user.findContainer(path);

		// THEN
		System.out.println("Found:");
		System.out.println(container);
		return container;
	}


	public PrismObject<UserType> createUser() throws SchemaException, SAXException, IOException {
		return PrismTestUtil.parseObject(USER_JACK_FILE_XML);
	}


}
