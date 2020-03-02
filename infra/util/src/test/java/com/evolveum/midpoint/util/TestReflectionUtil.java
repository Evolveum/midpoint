/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

public class TestReflectionUtil extends AbstractUnitTest {

    @Test
    public void testFindMethodByArity3() throws Exception {
        // GIVEN
        ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();

        // WHEN
        Method method = ReflectionUtil.findMethod(library, "m", 3);

        // THEN
        assertNotNull("No method", method);
        method.invoke(library, "foo", 1, 2L);

        assertCalled(library, "m3");
    }

    @Test
    public void testFindMethodByArglist3() throws Exception {
        // GIVEN
        ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();
        List<Object> argList = new ArrayList<>();
        argList.add("foo");
        argList.add(1);
        argList.add(2L);

        // WHEN
        Method method = ReflectionUtil.findMethod(library, "m", argList);

        // THEN
        assertNotNull("No method", method);
        method.invoke(library, "foo", 1, 2L);

        assertCalled(library, "m3");
    }

    @Test
    public void testFindMethodByArglist2() throws Exception {
        // GIVEN
        ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();
        List<Object> argList = new ArrayList<>();
        argList.add("foo");
        argList.add(1);

        // WHEN
        Method method = ReflectionUtil.findMethod(library, "m", argList);

        // THEN
        assertNotNull("No method", method);
        method.invoke(library, "foo", 1);

        assertCalled(library, "m2i");
    }

    @Test
    public void testFindMethodByArglistVararg() throws Exception {
        // GIVEN
        ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();
        List<Object> argList = new ArrayList<>();
        argList.add("foo");
        argList.add("bar");
        argList.add("baz");

        // WHEN
        Method method = ReflectionUtil.findMethod(library, "v", argList);

        // THEN
        assertNotNull("No method", method);
        method.invoke(library, new Object[] { new String[] { "foo", "bar", "baz" } });

        assertCalled(library, "v:3");
    }

    @Test
    public void testInvokeMethodByArglist3() throws Exception {
        // GIVEN
        ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();
        List<Object> argList = new ArrayList<>();
        argList.add("foo");
        argList.add(1);
        argList.add(2L);

        // WHEN
        ReflectionUtil.invokeMethod(library, "m", argList);

        // THEN
        assertCalled(library, "m3");
    }

    @Test
    public void testInvokeMethodByArglist2() throws Exception {
        // GIVEN
        ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();
        List<Object> argList = new ArrayList<>();
        argList.add("foo");
        argList.add(1);

        // WHEN
        ReflectionUtil.invokeMethod(library, "m", argList);

        // THEN
        assertCalled(library, "m2i");
    }

    @Test
    public void testInvokeMethodByArglistVararg() throws Exception {
        // GIVEN
        ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();
        List<Object> argList = new ArrayList<>();
        argList.add("foo");
        argList.add("bar");
        argList.add("baz");

        // WHEN
        ReflectionUtil.invokeMethod(library, "v", argList);

        // THEN
        assertCalled(library, "v:3");
    }

    @Test
    public void testInvokeMethodByArglistCollection() throws Exception {
        // GIVEN
        ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();
        List<Object> argList = new ArrayList<>();
        List<String> l = new ArrayList<>();
        l.add("foo");
        argList.add(l);

        // WHEN
        ReflectionUtil.invokeMethod(library, "l", argList);

        // THEN
        assertCalled(library, "lc");
    }

    private void assertCalled(ReflectionTestFunctionLibrary library, String methodId) {
        assertTrue("The method " + methodId + " was not called. Called: " + library.getCalledIds(), library.wasCalled(methodId));
    }
}
