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

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.DEFAULT_NAMESPACE_PREFIX;

import java.io.File;
import java.io.IOException;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

public class TestObjectQuery {
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	@Test
	public void testMatchAndFilter() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(new File("src/test/resources/common/user-jack.xml"));
		ObjectFilter filter = AndFilter.createAnd(EqualsFilter.createEqual(null,
				user.findItem(UserType.F_GIVEN_NAME).getDefinition(), null, "Jack"), SubstringFilter
				.createSubstring(null, user.findItem(UserType.F_FULL_NAME).getDefinition(), null, "arr"));
		boolean match = ObjectQuery.match(user, filter);
		AssertJUnit.assertTrue("filter does not match object", match);
	}
	
	
	@Test
	public void testMatchOrFilter() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(new File("src/test/resources/common/user-jack.xml"));
		ObjectFilter filter = OrFilter.createOr(EqualsFilter.createEqual(null,
				user.findItem(UserType.F_GIVEN_NAME).getDefinition(), null, "Jack"), EqualsFilter.createEqual(null,
						user.findItem(UserType.F_GIVEN_NAME).getDefinition(), null, "Jackie"));

		boolean match = ObjectQuery.match(user, filter);
		AssertJUnit.assertTrue("filter does not match object", match);
	}
	
	@Test
	public void testDontMatchEqualFilter() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(new File("src/test/resources/common/user-jack.xml"));
		ObjectFilter filter = EqualsFilter.createEqual(null,
						user.findItem(UserType.F_GIVEN_NAME).getDefinition(), null, "Jackie");

		boolean match = ObjectQuery.match(user, filter);
		AssertJUnit.assertFalse("filter matches object, but it should not", match);
	}
	
	@Test
	public void testComplexMatch() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(new File("src/test/resources/common/user-jack.xml"));
		ObjectFilter filter = AndFilter.createAnd(EqualsFilter.createEqual(null,
				user.findItem(UserType.F_FAMILY_NAME).getDefinition(), null, "Sparrow"), SubstringFilter
				.createSubstring(null, user.findItem(UserType.F_FULL_NAME).getDefinition(), "arr"), OrFilter.createOr(EqualsFilter.createEqual(null,
						user.findItem(UserType.F_GIVEN_NAME).getDefinition(), null, "Jack"), EqualsFilter.createEqual(null,
								user.findItem(UserType.F_GIVEN_NAME).getDefinition(), null, "Jackie")));

		boolean match = ObjectQuery.match(user, filter);
		AssertJUnit.assertTrue("filter does not match object", match);
	}
	
	@Test
	public void testPolystringMatchEqualFilter() throws Exception{
		PrismObject user = PrismTestUtil.parseObject(new File("src/test/resources/common/user-jack.xml"));
		PolyString name = new PolyString("jack", "jack");
		ObjectFilter filter = EqualsFilter.createEqual(null,
						user.findItem(UserType.F_NAME).getDefinition(), null, name);

		boolean match = ObjectQuery.match(user, filter);
		AssertJUnit.assertTrue("filter does not match object", match);
	}
	

}
