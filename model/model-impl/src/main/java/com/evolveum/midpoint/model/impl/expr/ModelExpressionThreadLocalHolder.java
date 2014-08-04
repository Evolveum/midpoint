/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.impl.expr;

import java.util.ArrayDeque;
import java.util.Deque;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensContextPlaceholder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Radovan Semancik
 *
 */
public class ModelExpressionThreadLocalHolder {

	private static ThreadLocal<Deque<LensContext<ObjectType>>> lensContextStackTl =
			new ThreadLocal<Deque<LensContext<ObjectType>>>();
	private static ThreadLocal<Deque<OperationResult>> currentResultStackTl = new ThreadLocal<Deque<OperationResult>>();
	private static ThreadLocal<Deque<Task>> currentTaskStackTl = new ThreadLocal<Deque<Task>>();
	
	public static <F extends ObjectType> void pushLensContext(LensContext<F> ctx) {
		Deque<LensContext<ObjectType>> stack = lensContextStackTl.get();
		if (stack == null) {
			stack = new ArrayDeque<LensContext<ObjectType>>();
			lensContextStackTl.set(stack);
		}
		stack.push((LensContext<ObjectType>)ctx);
	}
	
	public static <F extends ObjectType> void popLensContext() {
		Deque<LensContext<ObjectType>> stack = lensContextStackTl.get();
		stack.pop();
	}

	public static <F extends ObjectType> LensContext<F> getLensContext() {
		Deque<LensContext<ObjectType>> stack = lensContextStackTl.get();
		if (stack == null) {
			return null;
		}
		return (LensContext<F>) stack.peek();
	}
	
	public static void pushCurrentResult(OperationResult result) {
		Deque<OperationResult> stack = currentResultStackTl.get();
		if (stack == null) {
			stack = new ArrayDeque<OperationResult>();
			currentResultStackTl.set(stack);
		}
		stack.push(result);
	}
	
	public static void popCurrentResult() {
		Deque<OperationResult> stack = currentResultStackTl.get();
		stack.pop();
	}

	public static OperationResult getCurrentResult() {
		Deque<OperationResult> stack = currentResultStackTl.get();
		if (stack == null) {
			return null;
		}
		return stack.peek();
	}

	public static void pushCurrentTask(Task task) {
		Deque<Task> stack = currentTaskStackTl.get();
		if (stack == null) {
			stack = new ArrayDeque<Task>();
			currentTaskStackTl.set(stack);
		}
		stack.push(task);
	}
	
	public static void popCurrentTask() {
		Deque<Task> stack = currentTaskStackTl.get();
		stack.pop();
	}

	public static Task getCurrentTask() {
		Deque<Task> stack = currentTaskStackTl.get();
		if (stack == null) {
			return null;
		}
		return stack.peek();
	}

}
