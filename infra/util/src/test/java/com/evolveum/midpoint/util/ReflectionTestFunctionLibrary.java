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

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author semancik
 *
 */
public class ReflectionTestFunctionLibrary {

	Collection<String> calledIds = new ArrayList<>();

	// Test functions

	public void m(String a1) {
		calledIds.add("m1");
	}

	public void m(String a1, Integer a2) {
		calledIds.add("m2i");
	}

	public void m(String a1, String a2) {
		calledIds.add("m2s");
	}

	public void m(String a1, Object a2) {
		calledIds.add("m2o");
	}

	public void m(String a1, Integer a2, Long a3) {
		calledIds.add("m3");
	}

	public void m(String a1, Integer a2, Long a3, Boolean a4) {
		calledIds.add("m4");
	}

	public void l(String s) {
		calledIds.add("ls");
	}

	public void l(Collection<String> c) {
		calledIds.add("lc");
	}

	// Test functions: varargs

	public void v(String... strings) {
		calledIds.add("v:"+strings.length);
	}

	// Utility

	public boolean wasCalled(String methodId) {
		return calledIds.contains(methodId);
	}

	public void reset() {
		calledIds.clear();
	}

	public Collection<String> getCalledIds() {
		return calledIds;
	}

}
