/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 *
 */
public class TestFilterSimplifier {

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void test010All() throws Exception {
		System.out.println("===[ test010All ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.all().buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
    }

	@Test
	public void test020None() throws Exception {
		System.out.println("===[ test020None ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.none().buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
    }

	@Test
	public void test030Undefined() throws Exception {
		System.out.println("===[ test030Undefined ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.undefined().buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
	}

	@Test
	public void test100AndLevel1() throws Exception {
		System.out.println("===[ test100AndLevel1 ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.all()
				.and().none()
				.and().undefined()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test110AndLevel1WithoutNone() throws Exception {
		System.out.println("===[ test110AndLevel1WithoutNone ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.all()
				.and().undefined()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
	}

	@Test
	public void test120AndEmpty() throws Exception {
		System.out.println("===[ test120AndEmpty ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = AndFilter.createAnd();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
	}

	@Test
	public void test150OrLevel1() throws Exception {
		System.out.println("===[ test150OrLevel1 ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.all()
				.or().none()
				.or().undefined()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
	}

	@Test
	public void test160OrLevel1WithoutAll() throws Exception {
		System.out.println("===[ test160OrLevel1WithoutAll ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.none()
				.or().undefined()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test170OrLevel1Undefined() throws Exception {
		System.out.println("===[ test170OrLevel1Undefined ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = OrFilter.createOr(UndefinedFilter.createUndefined());
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test180OrEmpty() throws Exception {
		System.out.println("===[ test180OrEmpty ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = OrFilter.createOr();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test200AndLevel2() throws Exception {
		System.out.println("===[ test200AndLevel2 ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.all()
				.and().undefined()
				.and()
					.block()
						.none().or().none()
					.endBlock()
				.and()
					.block()
						.none().or().none()
					.endBlock()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test210OrLevel2() throws Exception {
		System.out.println("===[ test210OrLevel2 ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.none()
				.or().undefined()
				.or()
					.block()
						.none().or().none()
					.endBlock()
				.or()
					.block()
						.none().or().none()
					.endBlock()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test300NotAll() throws Exception {
		System.out.println("===[ test300NotAll ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.not().all()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test310NotNone() throws Exception {
		System.out.println("===[ test310NotNone ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.not().none()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
	}

	@Test
	public void test320NotNotAll() throws Exception {
		System.out.println("===[ test320NotNotAll ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.not().block().not().all().endBlock()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified == null || simplified instanceof AllFilter);
	}

	@Test
	public void test330NotNotNone() throws Exception {
		System.out.println("===[ test330NotNotNone ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.not().block().not().none().endBlock()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test400TypeAll() throws Exception {
		System.out.println("===[ test400TypeAll ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.type(UserType.class).all()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof TypeFilter);
		TypeFilter typeSimplified = (TypeFilter) simplified;
		assertEquals("Wrong simplified filter type", UserType.COMPLEX_TYPE, typeSimplified.getType());
		assertTrue("Wrong simplified filter subfilter: " + typeSimplified.getFilter(), ObjectQueryUtil.isAll(typeSimplified.getFilter()));
	}

	@Test
	public void test410TypeNone() throws Exception {
		System.out.println("===[ test410TypeNone ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.type(UserType.class).none()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test420TypeUndefined() throws Exception {
		System.out.println("===[ test420TypeUndefined ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.type(UserType.class).undefined()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof TypeFilter);
		TypeFilter typeSimplified = (TypeFilter) simplified;
		assertEquals("Wrong simplified filter type", UserType.COMPLEX_TYPE, typeSimplified.getType());
		assertTrue("Wrong simplified filter subfilter: " + typeSimplified.getFilter(), ObjectQueryUtil.isAll(typeSimplified.getFilter()));
	}

	@Test
	public void test500ExistsAll() throws Exception {
		System.out.println("===[ test500ExistsAll ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.exists(UserType.F_ASSIGNMENT).all()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof ExistsFilter);
		ExistsFilter existsSimplified = (ExistsFilter) simplified;
		assertEquals("Wrong simplified filter path", new ItemPath(UserType.F_ASSIGNMENT), existsSimplified.getFullPath());
		assertTrue("Wrong simplified filter subfilter: " + existsSimplified.getFilter(), ObjectQueryUtil.isAll(existsSimplified.getFilter()));
	}

	@Test
	public void test510ExistsNone() throws Exception {
		System.out.println("===[ test510ExistsNone ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.exists(UserType.F_ASSIGNMENT).none()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof NoneFilter);
	}

	@Test
	public void test520ExistsUndefined() throws Exception {
		System.out.println("===[ test520ExistsUndefined ]===");

		// GIVEN
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		// WHEN
		ObjectFilter filter = QueryBuilder.queryFor(UserType.class, prismContext)
				.exists(UserType.F_ASSIGNMENT).undefined()
				.buildFilter();
		System.out.println("Original filter:\n" + filter.debugDump());

		// THEN
		ObjectFilter simplified = ObjectQueryUtil.simplify(filter);
		System.out.println("Simplified filter:\n" + DebugUtil.debugDump(simplified));
		assertTrue("Wrong simplified filter: " + simplified, simplified instanceof ExistsFilter);
		ExistsFilter existsSimplified = (ExistsFilter) simplified;
		assertEquals("Wrong simplified filter path", new ItemPath(UserType.F_ASSIGNMENT), existsSimplified.getFullPath());
		assertTrue("Wrong simplified filter subfilter: " + existsSimplified.getFilter(), ObjectQueryUtil.isAll(existsSimplified.getFilter()));
	}

}
