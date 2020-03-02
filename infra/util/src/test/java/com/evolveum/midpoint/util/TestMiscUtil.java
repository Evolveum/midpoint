/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

public class TestMiscUtil extends AbstractUnitTest {

    @Test
    public void testUnion() {
        Collection<String> a = new HashSet<>();
        a.add("A1");
        a.add("X");
        a.add("Y");
        Collection<String> b = new ArrayList<>();
        b.add("B1");
        b.add("B2");
        b.add("X");
        Collection<String> c = new Vector<>();
        c.add("C1");
        c.add("X");
        c.add("Y");

        //noinspection unchecked
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
        String in = "<?xml>  <!-- sjsdj --> <foobar></foobar>";
        String out = UglyHacks.forceXsiNsDeclaration(in);
        assertEquals("<?xml>  <!-- sjsdj --> <foobar xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'></foobar>", out);
    }

    @Test
    public void testUglyXsiHack2() {
        String in = "<foo:bar xmlns:foo='http://foo.com/'></foo:bar>";
        String out = UglyHacks.forceXsiNsDeclaration(in);
        assertEquals("<foo:bar xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:foo='http://foo.com/'></foo:bar>", out);
    }

    @Test
    public void testUglyXsiHack3() {
        String in = "<foo:bar xmlns:foo='http://foo.com/' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'></foo:bar>";
        String out = UglyHacks.forceXsiNsDeclaration(in);
        assertEquals(in, out);
    }

    @Test
    public void testCarthesian() {
        // GIVEN
        Collection<Collection<String>> dimensions = new ArrayList<>();
        Collection<String> dim1 = new ArrayList<>();
        dim1.add("a");
        dim1.add("b");
        dimensions.add(dim1);
        Collection<String> dim2 = new ArrayList<>();
        dim2.add("1");
        dim2.add("2");
        dim2.add("3");
        dim2.add("4");
        dimensions.add(dim2);
        Collection<String> dim3 = new ArrayList<>();
        dim3.add("x");
        dim3.add("y");
        dim3.add("z");
        dimensions.add(dim3);

        final List<String> combinations = new ArrayList<>();
        Processor<Collection<String>> processor = s -> {
            System.out.println(s);
            combinations.add(MiscUtil.concat(s));
        };

        // WHEN
        MiscUtil.carthesian(dimensions, processor);

        // THEN
        System.out.println(combinations);
        assertEquals("Wrong number of results", 24, combinations.size());
    }
}
