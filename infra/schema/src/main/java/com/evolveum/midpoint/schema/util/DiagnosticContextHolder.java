/**
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.schema.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author semancik
 *
 */
public class DiagnosticContextHolder {
	
	private static ThreadLocal<Deque<DiagnosticContext>> diagStack = new ThreadLocal<>();

	public static void push(DiagnosticContext ctx) {
		Deque<DiagnosticContext> stack = diagStack.get();
		if (stack == null) {
			stack = new ArrayDeque<>();
			diagStack.set(stack);
		}
		stack.push(ctx);
	}

	public static DiagnosticContext pop() {
		Deque<DiagnosticContext> stack = diagStack.get();
		if (stack == null) {
			return null;
		}
		return stack.pop();
	}

	public static DiagnosticContext get() {
		Deque<DiagnosticContext> stack = diagStack.get();
		if (stack == null) {
			return null;
		}
		return stack.peek();
	}
	
	@SuppressWarnings("unchecked")
	public static <D extends DiagnosticContext> D get(Class<D> type) {
		DiagnosticContext ctx = get();
		if (ctx != null && type.isAssignableFrom(ctx.getClass())) {
			return (D) ctx;
		}
		return null;
	}
	
}
