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
package com.evolveum.midpoint.util;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

/**
 * @author semancik
 *
 */
public class TestReflectionUtil {

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
		List<Object> argList = new ArrayList<Object>();
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
		List<Object> argList = new ArrayList<Object>();
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
		List<Object> argList = new ArrayList<Object>();
		argList.add("foo");
		argList.add("bar");
		argList.add("baz");

		// WHEN
		Method method = ReflectionUtil.findMethod(library, "v", argList);

		// THEN
		assertNotNull("No method", method);
		method.invoke(library, new Object[] { new String[] {"foo", "bar", "baz"}});

		assertCalled(library, "v:3");
	}

	@Test
	public void testInvokeMethodByArglist3() throws Exception {
		// GIVEN
		ReflectionTestFunctionLibrary library = new ReflectionTestFunctionLibrary();
		List<Object> argList = new ArrayList<Object>();
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
		List<Object> argList = new ArrayList<Object>();
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
		List<Object> argList = new ArrayList<Object>();
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
		List<Object> argList = new ArrayList<Object>();
		List<String> l = new ArrayList<String>();
		l.add("foo");
		argList.add(l);

		// WHEN
		ReflectionUtil.invokeMethod(library, "l", argList);

		// THEN
		assertCalled(library, "lc");
	}


	private void assertCalled(ReflectionTestFunctionLibrary library, String methodId) {
		assertTrue("The method "+methodId+" was not called. Called: "+library.getCalledIds(), library.wasCalled(methodId));
	}

}
