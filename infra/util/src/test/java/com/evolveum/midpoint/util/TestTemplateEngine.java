/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import com.evolveum.midpoint.util.template.*;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.AssertJUnit.*;

public class TestTemplateEngine {

    @Test
    public void testTemplateWithJavaProperties() {
        System.out.println("===[ testTemplateWithJavaProperties ]===");
        System.setProperty("t1", "TEST1");
        System.setProperty("t2", "TEST2");

        ReferenceResolver resolver = new JavaPropertiesResolver(null, true);
        TemplateEngine engine = new TemplateEngine(resolver, true, false);
        assertExpand(engine, "", "");
        assertExpand(engine, "abc", "abc");
        assertExpand(engine, "${t1}", "TEST1");
        assertExpand(engine, "${t3}", "");
        assertExpand(engine, "$t1", "TEST1");
        assertExpand(engine, "$t1 $t2", "TEST1 TEST2");
        assertExpand(engine, "abc${t1}", "abcTEST1");
        assertExpand(engine, "abc${t1}def", "abcTEST1def");
        assertExpand(engine, "${t1}${t2}", "TEST1TEST2");
        assertExpand(engine, "${prop:t1} ${prop:t2()}", "TEST1 TEST2");
        assertExpand(engine, "${prop:t1(abc)} ${prop:t2('abc')}", "TEST1 TEST2");
        assertExpand(engine, "${prop:t1(abc)} ${prop:t2('abc\\}')}", "TEST1 TEST2");
        assertError(engine, "${t1", "Unfinished reference");
        assertError(engine, "abc${t1", "Unfinished reference");
        assertError(engine, "${t1('abc", "Unfinished reference");
        assertError(engine, "${t1('abc' abc", "Unexpected content after parameter");
    }

    @Test
    public void testTemplateWithBuiltin() {
        System.out.println("===[ testTemplateWithBuiltin ]===");
        Map<String, String> resolutionMap = new HashMap<>();
        resolutionMap.put("k1", "Key1");
        resolutionMap.put("k2", "Key2");

        AbstractChainedResolver builtinResolver = new AbstractChainedResolver(null, false) {
            @Override
            protected String resolveLocally(String reference, List<String> parameters) {
                if ("concat".equals(reference)) {
                    return String.join("+", parameters);
                } else {
                    return null;
                }
            }

            @NotNull
            @Override
            protected Collection<String> getScopes() {
                return Collections.singleton("");
            }
        };
        ReferenceResolver resolver = new MapResolver(builtinResolver, true, "map", resolutionMap);

        TemplateEngine engine = new TemplateEngine(resolver, true, true);
        assertExpand(engine, "", "");
        assertExpand(engine, "abc", "abc");
        assertExpand(engine, "${k1}", "Key1");
        assertExpand(engine, "${:concat}", "");
        assertExpand(engine, "x${:concat(a,'b','',d)}x", "xa+b++dx");
        assertExpand(engine, "x${ : concat (   a,   'b',    ' ',   d)}x", "xa+b+ +dx");
        assertError(engine, "${wrong}", "couldn't be resolved");
    }

    private void assertExpand(TemplateEngine engine, String template, String expected) {
        String result = engine.expand(template);
        System.out.println(template + " => " + result);
        assertEquals("Wrong result", expected, result);
    }

    private void assertError(TemplateEngine engine, String template, String expected) {
        try {
            String result = engine.expand(template);
            fail("Unexpected success with result=" + result);
        } catch (RuntimeException e) {
            if (e.getMessage().contains(expected)) {
                System.out.println("Got expected exception: " + e.getMessage());
            } else {
                throw new AssertionError("Got unexpected exception: " + e.getMessage(), e);
            }
        }
    }
}
