/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.template.*;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.AssertJUnit.*;

public class TestStringSubstitutorUtil extends AbstractUnitTest {

    @Test
    public void testSimpleExpand() {
        Map<String, String> map = new HashMap<>();
        map.put("t1", "TEST1");
        map.put("t2", "TEST2");

        assertSimpleExpand(map, "", "");
        assertSimpleExpand(map, "abc", "abc");
        assertSimpleExpand(map, "{t1}", "TEST1");
        assertSimpleExpand(map, "{t3}", "{t3}");
        assertSimpleExpand(map, "abc{t1}", "abcTEST1");
        assertSimpleExpand(map, "abc{t1}def", "abcTEST1def");
        assertSimpleExpand(map, "{t1}{t2}", "TEST1TEST2");
    }

    private void assertSimpleExpand(Map<String, String> map, String template, String expected) {
        String result = StringSubstitutorUtil.simpleExpand(template, map);
        System.out.println(template + " => " + result);
        assertEquals("Wrong result", expected, result);
    }
}
