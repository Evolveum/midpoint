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
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.*;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestPath {

	private static final String NS = "http://example.com/";

	private static Map<Character, Function<Object[], ItemPath>> creators = new HashMap<>();
	static {
		creators.put('C', seq -> getPrismContext().path(seq));
		creators.put('S', ItemPath::create);
		creators.put('N', seq -> seq.length == 1 ? ItemName.fromQName((QName) seq[0]) : ItemPath.create(seq));
		creators.put('R', seq -> creators.get("CSN".charAt((int) (Math.random() * 3))).apply(seq));
	}

	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}

	@Test
    public void testPathNormalize() {
		System.out.println("\n\n===[ testPathNormalize ]===\n");

		PrismContext prismContext = getPrismContext();

		// GIVEN
		UniformItemPath path1 = prismContext.path(new QName(NS, "foo"), new QName(NS, "bar"));
		UniformItemPath path2 = prismContext.path(new QName(NS, "foo"), 123L, new QName(NS, "bar"));
		UniformItemPath path22 = prismContext.path(new QName(NS, "foo"), 123L, new QName(NS, "bar"), null);
		UniformItemPath path3 = prismContext.path(new QName(NS, "foo"), 123L, new QName(NS, "bar"), 333L);
		UniformItemPath path4 = prismContext.path(new QName(NS, "x"));
		UniformItemPath path5 = prismContext.path(new QName(NS, "foo"), 123L);
		UniformItemPath pathE = prismContext.emptyPath();

		// WHEN
		UniformItemPath normalized1 = path1.normalize();
		UniformItemPath normalized2 = path2.normalize();
		UniformItemPath normalized22 = path22.normalize();
		UniformItemPath normalized3 = path3.normalize();
		UniformItemPath normalized4 = path4.normalize();
		UniformItemPath normalized5 = path5.normalize();
		UniformItemPath normalizedE = pathE.normalize();

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

	@SuppressWarnings("SpellCheckingInspection")
	@Test
    public void testPathCompareComplex() {
		testPathCompare("CCCCCCCCCCCC");
	}

	@SuppressWarnings("SpellCheckingInspection")
	@Test
    public void testPathCompareSimple() {
		testPathCompare("SSSSSSSSSSSS");
	}

	@SuppressWarnings("SpellCheckingInspection")
	@Test
    public void testPathCompareSingleNames() {
		testPathCompare("NNNNNNNNNNNN");
	}

	@SuppressWarnings("SpellCheckingInspection")
	@Test
    public void testPathCompareRandom() {
		testPathCompare("RRRRRRRRRRRR");
	}

	@SuppressWarnings("SpellCheckingInspection")
	@Test
	public void testPathCompareHalf1() {
		testPathCompare("CCCCCCSSSSSS");
	}

	@SuppressWarnings("SpellCheckingInspection")
	@Test
	public void testPathCompareHalf2() {
		testPathCompare("SSSSSSNNNNNN");
	}

	private void testPathCompare(String source) {
		System.out.println("\n\n===[ testPathCompare (" + source + ") ]===\n");

		// GIVEN
		
		// /foo
		ItemPath pathFoo = create(source, 0, new QName(NS, "foo"));
		
		// /foo/
		ItemPath pathFooNull = create(source, 1, new QName(NS, "foo"), null);
		
		// /foo/[123]
		ItemPath pathFoo123 = create(source, 2, new QName(NS, "foo"), 123L);
		
		// /foo/bar
		ItemPath pathFooBar = create(source, 3, new QName(NS, "foo"), new QName(NS, "bar"));
		
		// /foo/[123]/bar
		ItemPath pathFoo123Bar = create(source, 4, new QName(NS, "foo"), 123L, new QName(NS, "bar"));
		
		// /foo/bar/baz
		ItemPath pathFooBarBaz = create(source, 5, new QName(NS, "foo"), new QName(NS, "bar"), new QName(NS, "baz"));
		
		// /foo/[123]/bar/baz
		ItemPath pathFoo123BarBaz = create(source, 6, new QName(NS, "foo"), 123L, new QName(NS, "bar"), new QName(NS, "baz"));
		
		// /foo/baz/baz
		ItemPath pathFooBazBaz = create(source, 7, new QName(NS, "foo"), new QName(NS, "baz"), new QName(NS, "baz"));

		// /foo/[123]/baz/baz
		ItemPath pathFoo123BazBaz = create(source, 8, new QName(NS, "foo"), 123L, new QName(NS, "baz"), new QName(NS, "baz"));
		
		// /foo//bar
		ItemPath pathFooNullBar = create(source, 9, new QName(NS, "foo"), null, new QName(NS, "bar"));
		
		// /zoo
		ItemPath pathZoo = create(source, 10, new QName(NS, "zoo"));

		ItemPath empty = create(source, 11);

		List<ItemPath> onlyEmpty = Collections.singletonList(empty);
		List<ItemPath> onlyFoo = Collections.singletonList(pathFoo);
		List<ItemPath> onlyFooBar = Collections.singletonList(pathFooBar);

		// WHEN, THEN
		assertTrue(pathFoo.isSubPath(pathFooNull));
		assertFalse(pathFoo.equivalent(pathFoo123));
		assertFalse(pathFooNull.equivalent(pathFoo123));
		assertTrue(pathFoo.isSubPath(pathFooBar));
		assertTrue(pathFooBar.isSuperPath(pathFoo));
		assertTrue(pathFooBar.equivalent(pathFooNullBar));
		assertTrue(empty.isSubPath(pathFoo));
		assertFalse(pathFoo.isSubPath(empty));
		
		assertTrue(pathFoo123Bar.isSubPathOrEquivalent(pathFoo123BarBaz));
		assertFalse(pathFoo123Bar.isSubPathOrEquivalent(pathZoo));
		assertFalse(pathFoo123Bar.isSubPathOrEquivalent(pathFoo123BazBaz));
		
		assertFalse(pathFooBar.isSubPathOrEquivalent(pathFoo123BarBaz));
		assertFalse(pathFooBar.isSubPathOrEquivalent(pathZoo));
		assertFalse(pathFooBar.isSubPathOrEquivalent(pathFoo123BazBaz));
		
		assertTrue(pathFooBar.isSubPathOrEquivalent(pathFoo123BarBaz.namedSegmentsOnly()));
		
		assertTrue(pathFooBar.isSubPathOrEquivalent(pathFooBar));
		assertFalse(pathFooBar.isSubPathOrEquivalent(pathZoo));
		assertFalse(pathFooBar.isSubPathOrEquivalent(pathFooBazBaz));

		assertTrue(ItemPathCollectionsUtil.containsSubpathOrEquivalent(onlyEmpty, pathFoo));
		assertTrue(ItemPathCollectionsUtil.containsSubpath(onlyEmpty, pathFoo));
		assertFalse(ItemPathCollectionsUtil.containsSuperpathOrEquivalent(onlyEmpty, pathFoo));
		assertFalse(ItemPathCollectionsUtil.containsSuperpath(onlyEmpty, pathFoo));

		assertTrue(ItemPathCollectionsUtil.containsSubpathOrEquivalent(onlyEmpty, empty));
		assertFalse(ItemPathCollectionsUtil.containsSubpath(onlyEmpty, empty));
		assertTrue(ItemPathCollectionsUtil.containsSuperpathOrEquivalent(onlyEmpty, empty));
		assertFalse(ItemPathCollectionsUtil.containsSuperpath(onlyEmpty, empty));

		assertTrue(ItemPathCollectionsUtil.containsSubpathOrEquivalent(onlyFoo, pathFoo));
		assertFalse(ItemPathCollectionsUtil.containsSubpath(onlyFoo, pathFoo));
		assertTrue(ItemPathCollectionsUtil.containsSuperpathOrEquivalent(onlyFoo, pathFoo));
		assertFalse(ItemPathCollectionsUtil.containsSuperpath(onlyFoo, pathFoo));

		assertFalse(ItemPathCollectionsUtil.containsSubpathOrEquivalent(onlyFoo, empty));
		assertFalse(ItemPathCollectionsUtil.containsSubpath(onlyFoo, empty));
		assertTrue(ItemPathCollectionsUtil.containsSuperpathOrEquivalent(onlyFoo, empty));
		assertTrue(ItemPathCollectionsUtil.containsSuperpath(onlyFoo, empty));

		assertFalse(ItemPathCollectionsUtil.containsSubpathOrEquivalent(onlyFoo, pathZoo));
		assertFalse(ItemPathCollectionsUtil.containsSubpath(onlyFoo, pathZoo));
		assertFalse(ItemPathCollectionsUtil.containsSuperpathOrEquivalent(onlyFoo, pathZoo));
		assertFalse(ItemPathCollectionsUtil.containsSuperpath(onlyFoo, pathZoo));

		assertFalse(ItemPathCollectionsUtil.containsSubpathOrEquivalent(onlyFooBar, pathFoo));
		assertFalse(ItemPathCollectionsUtil.containsSubpath(onlyFooBar, pathFoo));
		assertTrue(ItemPathCollectionsUtil.containsSuperpathOrEquivalent(onlyFooBar, pathFoo));
		assertTrue(ItemPathCollectionsUtil.containsSuperpath(onlyFooBar, pathFoo));

		assertTrue(ItemPathCollectionsUtil.containsSubpathOrEquivalent(onlyFooBar, pathFooBarBaz));
		assertTrue(ItemPathCollectionsUtil.containsSubpath(onlyFooBar, pathFooBarBaz));
		assertFalse(ItemPathCollectionsUtil.containsSuperpathOrEquivalent(onlyFooBar, pathFooBarBaz));
		assertFalse(ItemPathCollectionsUtil.containsSuperpath(onlyFooBar, pathFooBarBaz));
	}

	private ItemPath create(String source, int index, Object... components) {
		return creators.get(source.charAt(index)).apply(components);
	}

	@Test
    public void testPathRemainder() throws Exception {
		System.out.println("\n\n===[ testPathRemainder ]===\n");

		PrismContext prismContext = getPrismContext();

		// GIVEN
		UniformItemPath pathFoo = prismContext.path(new QName(NS, "foo"));
		UniformItemPath pathBar = prismContext.path(new QName(NS, "bar"));
		UniformItemPath pathFooNull = prismContext.path(new QName(NS, "foo"), null);
		UniformItemPath pathFoo123 = prismContext.path(new QName(NS, "foo"), 123L);
		UniformItemPath pathFooBar = prismContext.path(new QName(NS, "foo"), new QName(NS, "bar"));
		UniformItemPath pathFooNullBar = prismContext.path(new QName(NS, "foo"), null,
												new QName(NS, "bar"));

		// WHEN
		UniformItemPath remainder1 = pathFooBar.remainder(pathFooNull);

		// THEN
		assertEquals("Remainder fooBar, fooNull", pathBar, remainder1);
	}

	private void assertNormalizedPath(UniformItemPath normalized, Object... expected) {
		assertEquals("wrong path length",normalized.size(), expected.length);
		for(int i=0; i<normalized.size(); i+=2) {
			ItemPathSegment nameSegment = normalized.getSegment(i);
			assert ItemPath.isName(nameSegment) : "Expected name segment but it was "+nameSegment.getClass();
			QName name = ItemPath.toName(nameSegment);
			assert name != null : "name is null";
			assert name.getNamespaceURI().equals(NS) : "wrong namespace: "+name.getNamespaceURI();
			assert name.getLocalPart().equals(expected[i]) : "wrong local name, expected "+expected[i]+", was "+name.getLocalPart();

			if (i + 1 < expected.length) {
				Object idSegment = normalized.getSegment(i+1);
				assert ItemPath.isId(idSegment) : "Expected is segment but it was "+idSegment.getClass();
				Long id = ItemPath.toId(idSegment);
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
