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
package com.evolveum.midpoint.util;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.testng.annotations.Test;

/**
 * @author semancik
 *
 */
public class TestMiscUtil {

	@Test
	public void testUnion() {
		System.out.println("===[ testUnion ]===");
		Collection<String> a = new HashSet<String>();
		a.add("A1");
		a.add("X");
		a.add("Y");
		Collection<String> b = new ArrayList<String>();
		b.add("B1");
		b.add("B2");
		b.add("X");
		Collection<String> c = new Vector<String>();
		c.add("C1");
		c.add("X");
		c.add("Y");
		
		Collection<String> union = MiscUtil.union(a,b,c);
		
		System.out.println("Union: "+union);
		
		assertEquals(6,union.size());
		assertTrue(union.contains("A1"));
		assertTrue(union.contains("X"));
		assertTrue(union.contains("Y"));
		assertTrue(union.contains("B1"));
		assertTrue(union.contains("B2"));
		assertTrue(union.contains("C1"));
	}
	
	@Test
	public void testUglyXsiHack1() {
		System.out.println("===[ testUglyXsiHack1 ]===");
		String in = "<?xml>  <!-- sjsdj --> <foobar></foobar>";
		String out = UglyHacks.forceXsiNsDeclaration(in);
		assertEquals("<?xml>  <!-- sjsdj --> <foobar xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'></foobar>", out);
	}

	@Test
	public void testUglyXsiHack2() {
		System.out.println("===[ testUglyXsiHack2 ]===");
		String in = "<foo:bar xmlns:foo='http://foo.com/'></foo:bar>";
		String out = UglyHacks.forceXsiNsDeclaration(in);
		assertEquals("<foo:bar xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:foo='http://foo.com/'></foo:bar>", out);
	}

	@Test
	public void testUglyXsiHack3() {
		System.out.println("===[ testUglyXsiHack3 ]===");
		String in = "<foo:bar xmlns:foo='http://foo.com/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'></foo:bar>";
		String out = UglyHacks.forceXsiNsDeclaration(in);
		assertEquals(in, out);
	}
	
	@Test
	public void testCarthesian() {
		System.out.println("===[ testCarthesian ]===");
		
		// GIVEN
		Collection<Collection<String>> dimensions = new ArrayList<Collection<String>>();
		Collection<String> dim1 = new ArrayList<String>();
		dim1.add("a");
		dim1.add("b");
		dimensions.add(dim1);
		Collection<String> dim2 = new ArrayList<String>();
		dim2.add("1");
		dim2.add("2");
		dim2.add("3");
		dim2.add("4");
		dimensions.add(dim2);
		Collection<String> dim3 = new ArrayList<String>();
		dim3.add("x");
		dim3.add("y");
		dim3.add("z");
		dimensions.add(dim3);
		
		final List<String> combinations = new ArrayList<String>();
		Processor<Collection<String>> processor = new Processor<Collection<String>>() {
			@Override
			public void process(Collection<String> s) {
				System.out.println(s);
				combinations.add(MiscUtil.concat(s));
			}
		};
		
		// WHEN
		MiscUtil.carthesian(dimensions, processor);
		
		// THEN
		System.out.println(combinations);
		assertEquals("Wrong number of results", 24, combinations.size());
	}
}
