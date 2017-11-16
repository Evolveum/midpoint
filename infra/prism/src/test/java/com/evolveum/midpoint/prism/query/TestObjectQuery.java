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

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.PrismInternalTestUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

public class TestObjectQuery {

	private static MatchingRuleRegistry matchingRuleRegistry;

	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());

		matchingRuleRegistry = MatchingRuleRegistryFactory.createRegistry();
	}

	@Test
	public void testMatchAndFilter() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter =
				QueryBuilder.queryFor(UserType.class, getPrismContext())
						.item(UserType.F_GIVEN_NAME).eq("Jack").matchingCaseIgnore()
						.and().item(UserType.F_FULL_NAME).contains("arr")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}


	@Test
	public void testMatchOrFilter() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_GIVEN_NAME).eq("Jack")
				.or().item(UserType.F_GIVEN_NAME).eq("Jackie")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}

	@Test
	public void testDontMatchEqualFilter() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_GIVEN_NAME).eq("Jackie")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}

	@Test
	public void testMatchEqualMultivalue() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		PrismPropertyDefinitionImpl def = new PrismPropertyDefinitionImpl(new QName("indexedString"), DOMUtil.XSD_STRING, getPrismContext());
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(new ItemPath(UserType.F_EXTENSION, "indexedString"), def).eq("alpha")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}

	@Test
	public void testMatchEqualNonEmptyAgainstEmptyItem() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		// jack has no locality
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_LOCALITY).eq("some")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}

	@Test
	public void testMatchEqualEmptyAgainstEmptyItem() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		// jack has no locality
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_LOCALITY).isNull()
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}

	@Test
	public void testMatchEqualEmptyAgainstNonEmptyItem() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		// jack has no locality
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_NAME).isNull()
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}

	@Test
	public void testComplexMatch() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
//		System.out.println("user given name" + user.asObjectable().getGivenName());
		System.out.println("definition: " +user.findItem(UserType.F_FAMILY_NAME).getDefinition().debugDump());
		ObjectFilter filter =
				QueryBuilder.queryFor(UserType.class, getPrismContext())
						.item(UserType.F_FAMILY_NAME).eq("Sparrow")
						.and().item(UserType.F_FULL_NAME).contains("arr")
						.and()
							.block()
								.item(UserType.F_GIVEN_NAME).eq("Jack")
								.or().item(UserType.F_GIVEN_NAME).eq("Jackie")
							.endBlock()
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}

	@Test
	public void testPolystringMatchEqualFilter() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		PolyString name = new PolyString("jack", "jack");
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_NAME).eq(name)
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}

	@Test   // MID-4120
	public void testMatchSubstringAgainstEmptyItem() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		// jack has no locality
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_LOCALITY).startsWith("C")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}

	@Test   // MID-4173
	public void testExistsNegative() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.exists(UserType.F_ASSIGNMENT)
						.item(AssignmentType.F_DESCRIPTION).eq("Assignment NONE")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}

	@Test   // MID-4173
	public void testExistsPositive() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.exists(UserType.F_ASSIGNMENT)
						.item(AssignmentType.F_DESCRIPTION).eq("Assignment 2")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object, but it should", match);
	}

	@Test   // MID-4173
	public void testExistsAnyNegative() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		user.removeContainer(UserType.F_ASSIGNMENT);
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.exists(UserType.F_ASSIGNMENT)
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}

	@Test   // MID-4173
	public void testExistsAnyPositive() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.exists(UserType.F_ASSIGNMENT)
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object, but it should", match);
	}

	@Test   // MID-4217
	public void testMultiRootPositive() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION).eq("Assignment 2")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object, but it should", match);
	}

	@Test   // MID-4217
	public void testMultiRootNegative() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, getPrismContext())
				.item(UserType.F_ASSIGNMENT, AssignmentType.F_DESCRIPTION).eq("Assignment XXXXX")
				.buildFilter();
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}
}
