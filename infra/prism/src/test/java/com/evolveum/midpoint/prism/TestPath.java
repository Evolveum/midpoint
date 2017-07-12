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

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestPath {
	
	private static final String NS = "http://example.com/";

	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	@Test
    public void testPathNormalize() throws Exception {
		System.out.println("\n\n===[ testPathNormalize ]===\n");
		
		// GIVEN
		ItemPath path1 = new ItemPath(new QName(NS, "foo"), new QName(NS, "bar"));
		ItemPath path2 = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment(123L),
									  new NameItemPathSegment(new QName(NS, "bar")));
		ItemPath path22 = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment(123L),
				  new NameItemPathSegment(new QName(NS, "bar")), new IdItemPathSegment(null));
		ItemPath path3 = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment(123L),
				  					  new NameItemPathSegment(new QName(NS, "bar")), new IdItemPathSegment(333L));
		ItemPath path4 = new ItemPath(new QName(NS, "x"));
		ItemPath path5 = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment(123L));
		ItemPath pathE = ItemPath.EMPTY_PATH;
		
		// WHEN
		ItemPath normalized1 = path1.normalize();
		ItemPath normalized2 = path2.normalize();
		ItemPath normalized22 = path22.normalize();
		ItemPath normalized3 = path3.normalize();
		ItemPath normalized4 = path4.normalize();
		ItemPath normalized5 = path5.normalize();
		ItemPath normalizedE = pathE.normalize();
		
		// THEN
		System.out.println("Normalized path 1:" + normalized1);
		System.out.println("Normalized path 2:" + normalized2);
		System.out.println("Normalized path 22:" + normalized22);
		System.out.println("Normalized path 3:" + normalized3);
		System.out.println("Normalized path 4:" + normalized4);
		System.out.println("Normalized path 5:" + normalized5);
		System.out.println("Normalized path E:" + normalizedE);

		assertNormalizedPath(normalized1, "foo", null, "bar");
		assertNormalizedPath(normalized2, "foo", 123L, "bar");
		assertNormalizedPath(normalized22, "foo", 123L, "bar", null);
		assertNormalizedPath(normalized3, "foo", 123L, "bar", 333L);
		assertNormalizedPath(normalized4, "x");
		assertNormalizedPath(normalized5, "foo", 123L);
		assert normalizedE.isEmpty() : "normalizedE is not empty";
	}
	
	@Test
    public void testPathCompare() throws Exception {
		System.out.println("\n\n===[ testPathCompare ]===\n");
		
		// GIVEN
		ItemPath pathFoo = new ItemPath(new QName(NS, "foo"));
		ItemPath pathFooNull = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment());
		ItemPath pathFoo123 = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment(123L));
		ItemPath pathFooBar = new ItemPath(new QName(NS, "foo"), new QName(NS, "bar"));
		ItemPath pathFooBarBaz = new ItemPath(new QName(NS, "foo"), new QName(NS, "bar"), new QName(NS, "baz"));
		ItemPath pathFooNullBar = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment(),
												new NameItemPathSegment(new QName(NS, "bar")));
		ItemPath pathZoo = new ItemPath(new QName(NS, "zoo"));

		List<ItemPath> onlyEmpty = Collections.singletonList(ItemPath.EMPTY_PATH);
		List<ItemPath> onlyFoo = Collections.singletonList(pathFoo);
		List<ItemPath> onlyFooBar = Collections.singletonList(pathFooBar);

		// WHEN, THEN
		assertTrue(pathFoo.isSubPath(pathFooNull));
		assertFalse(pathFoo.equivalent(pathFoo123));
		assertFalse(pathFooNull.equivalent(pathFoo123));
		assertTrue(pathFoo.isSubPath(pathFooBar));
		assertTrue(pathFooBar.isSuperPath(pathFoo));
		assertTrue(pathFooBar.equivalent(pathFooNullBar));
		assertTrue(ItemPath.EMPTY_PATH.isSubPath(pathFoo));
		assertFalse(pathFoo.isSubPath(ItemPath.EMPTY_PATH));

		assertTrue(ItemPath.containsSubpathOrEquivalent(onlyEmpty, pathFoo));
		assertTrue(ItemPath.containsSubpath(onlyEmpty, pathFoo));
		assertFalse(ItemPath.containsSuperpathOrEquivalent(onlyEmpty, pathFoo));
		assertFalse(ItemPath.containsSuperpath(onlyEmpty, pathFoo));

		assertTrue(ItemPath.containsSubpathOrEquivalent(onlyEmpty, ItemPath.EMPTY_PATH));
		assertFalse(ItemPath.containsSubpath(onlyEmpty, ItemPath.EMPTY_PATH));
		assertTrue(ItemPath.containsSuperpathOrEquivalent(onlyEmpty, ItemPath.EMPTY_PATH));
		assertFalse(ItemPath.containsSuperpath(onlyEmpty, ItemPath.EMPTY_PATH));

		assertTrue(ItemPath.containsSubpathOrEquivalent(onlyFoo, pathFoo));
		assertFalse(ItemPath.containsSubpath(onlyFoo, pathFoo));
		assertTrue(ItemPath.containsSuperpathOrEquivalent(onlyFoo, pathFoo));
		assertFalse(ItemPath.containsSuperpath(onlyFoo, pathFoo));

		assertFalse(ItemPath.containsSubpathOrEquivalent(onlyFoo, ItemPath.EMPTY_PATH));
		assertFalse(ItemPath.containsSubpath(onlyFoo, ItemPath.EMPTY_PATH));
		assertTrue(ItemPath.containsSuperpathOrEquivalent(onlyFoo, ItemPath.EMPTY_PATH));
		assertTrue(ItemPath.containsSuperpath(onlyFoo, ItemPath.EMPTY_PATH));

		assertFalse(ItemPath.containsSubpathOrEquivalent(onlyFoo, pathZoo));
		assertFalse(ItemPath.containsSubpath(onlyFoo, pathZoo));
		assertFalse(ItemPath.containsSuperpathOrEquivalent(onlyFoo, pathZoo));
		assertFalse(ItemPath.containsSuperpath(onlyFoo, pathZoo));

		assertFalse(ItemPath.containsSubpathOrEquivalent(onlyFooBar, pathFoo));
		assertFalse(ItemPath.containsSubpath(onlyFooBar, pathFoo));
		assertTrue(ItemPath.containsSuperpathOrEquivalent(onlyFooBar, pathFoo));
		assertTrue(ItemPath.containsSuperpath(onlyFooBar, pathFoo));

		assertTrue(ItemPath.containsSubpathOrEquivalent(onlyFooBar, pathFooBarBaz));
		assertTrue(ItemPath.containsSubpath(onlyFooBar, pathFooBarBaz));
		assertFalse(ItemPath.containsSuperpathOrEquivalent(onlyFooBar, pathFooBarBaz));
		assertFalse(ItemPath.containsSuperpath(onlyFooBar, pathFooBarBaz));
	}
	
	@Test
    public void testPathRemainder() throws Exception {
		System.out.println("\n\n===[ testPathRemainder ]===\n");
		
		// GIVEN
		ItemPath pathFoo = new ItemPath(new QName(NS, "foo"));
		ItemPath pathBar = new ItemPath(new QName(NS, "bar"));
		ItemPath pathFooNull = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment());
		ItemPath pathFoo123 = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment(123L));
		ItemPath pathFooBar = new ItemPath(new QName(NS, "foo"), new QName(NS, "bar"));
		ItemPath pathFooNullBar = new ItemPath(new NameItemPathSegment(new QName(NS, "foo")), new IdItemPathSegment(), 
												new NameItemPathSegment(new QName(NS, "bar")));
				
		// WHEN
		ItemPath remainder1 = pathFooBar.remainder(pathFooNull);
		
		// THEN
		assertEquals("Remainder fooBar, fooNull", pathBar, remainder1);
	}

	private void assertNormalizedPath(ItemPath normalized, Object... expected) {
		assertEquals("wrong path length",normalized.size(), expected.length);
		for(int i=0; i<normalized.size(); i+=2) {
			ItemPathSegment nameSegment = normalized.getSegments().get(i);
			assert nameSegment instanceof NameItemPathSegment : "Expected name segment but it was "+nameSegment.getClass();
			QName name = ((NameItemPathSegment)nameSegment).getName();
			assert name != null : "name is null";
			assert name.getNamespaceURI().equals(NS) : "wrong namespace: "+name.getNamespaceURI();
			assert name.getLocalPart().equals(expected[i]) : "wrong local name, expected "+expected[i]+", was "+name.getLocalPart();
			
			if (i + 1 < expected.length) {
				ItemPathSegment idSegment = normalized.getSegments().get(i+1);
				assert idSegment instanceof IdItemPathSegment : "Expected is segment but it was "+nameSegment.getClass();
				Long id = ((IdItemPathSegment)idSegment).getId();
				assertId(id, (Long)expected[i+1]);
			}
		}
	}

	private void assertId(Long actual, Long expected) {
		if (expected == null && actual == null) {
			return;
		}
		assert expected != null : "Expected null id but it was "+actual;
		assertEquals("wrong id", expected, actual);
	}



}
