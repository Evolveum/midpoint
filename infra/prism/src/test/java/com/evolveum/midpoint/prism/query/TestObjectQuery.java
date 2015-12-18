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
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistryFactory;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;

public class TestObjectQuery {
	
	private static MatchingRuleRegistry matchingRuleRegistry;
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
		
		matchingRuleRegistry = MatchingRuleRegistryFactory.createRegistry();
	}
	
	@Test
	public void testMatchAndFilter() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = 
				AndFilter.createAnd(
						EqualFilter.createEqual(UserType.F_GIVEN_NAME, user.findProperty(UserType.F_GIVEN_NAME).getDefinition(), StringIgnoreCaseMatchingRule.NAME, "Jack"), 
						SubstringFilter.createSubstring(UserType.F_FULL_NAME, user.findProperty(UserType.F_FULL_NAME).getDefinition(), "arr"));
		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}
	
	
	@Test
	public void testMatchOrFilter() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = OrFilter.createOr(
				EqualFilter.createEqual(UserType.F_GIVEN_NAME, user.findProperty(UserType.F_GIVEN_NAME).getDefinition(), null, "Jack"), 
				EqualFilter.createEqual(UserType.F_GIVEN_NAME, user.findProperty(UserType.F_GIVEN_NAME).getDefinition(), null, "Jackie"));

		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}
	
	@Test
	public void testDontMatchEqualFilter() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		ObjectFilter filter = EqualFilter.createEqual(UserType.F_GIVEN_NAME, user.findProperty(UserType.F_GIVEN_NAME).getDefinition(), null, "Jackie");

		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}
	
	@Test
	public void testComplexMatch() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
//		System.out.println("user given name" + user.asObjectable().getGivenName());
		System.out.println("definition: " +user.findItem(UserType.F_FAMILY_NAME).getDefinition().debugDump());
		ObjectFilter filter = 
				AndFilter.createAnd(
						EqualFilter.createEqual(UserType.F_FAMILY_NAME, user.findProperty(UserType.F_FAMILY_NAME).getDefinition(), null, "Sparrow"), 
						SubstringFilter.createSubstring(UserType.F_FULL_NAME, user.findProperty(UserType.F_FULL_NAME).getDefinition(), "arr"), 
						OrFilter.createOr(
								EqualFilter.createEqual(UserType.F_GIVEN_NAME, user.findProperty(UserType.F_GIVEN_NAME).getDefinition(), null, "Jack"), 
								EqualFilter.createEqual(UserType.F_GIVEN_NAME, user.findProperty(UserType.F_GIVEN_NAME).getDefinition(), null, "Jackie")));

		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}
	
	@Test
	public void testPolystringMatchEqualFilter() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(PrismInternalTestUtil.USER_JACK_FILE_XML);
		PolyString name = new PolyString("jack", "jack");
		ObjectFilter filter = EqualFilter.createEqual(UserType.F_NAME, user.findProperty(UserType.F_NAME).getDefinition(), null, name);

		boolean match = ObjectQuery.match(user, filter, matchingRuleRegistry);
		AssertJUnit.assertTrue("filter does not match object", match);
	}
	

}
