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

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author semancik
 *
 */
public class ReflectionTestFunctionLibrary {
	
	Collection<String> calledIds = new ArrayList<String>();
	
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
