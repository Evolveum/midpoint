/*
 * Copyright (c) 2013-2016 Evolveum
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

import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensContextPlaceholder;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Radovan Semancik
 *
 */
public class ModelExpressionThreadLocalHolder {

	private static ThreadLocal<Deque<LensContext<ObjectType>>> lensContextStackTl =
			new ThreadLocal<Deque<LensContext<ObjectType>>>();
	private static ThreadLocal<Deque<OperationResult>> currentResultStackTl = new ThreadLocal<Deque<OperationResult>>();
	private static ThreadLocal<Deque<Holder<Task>>> currentTaskStackTl = new ThreadLocal<>();		// to allow task to be null

	public static <F extends ObjectType> void pushLensContext(LensContext<F> ctx) {
		Deque<LensContext<ObjectType>> stack = lensContextStackTl.get();
		if (stack == null) {
			stack = new ArrayDeque<LensContext<ObjectType>>();
			lensContextStackTl.set(stack);
		}
		if (ctx == null) {
			// Deque cannot hold null elements. So we need to create a placeholder
			ctx =  new LensContextPlaceholder<>(null);
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
		LensContext<F> ctx = (LensContext<F>) stack.peek();
		if (ctx instanceof LensContextPlaceholder) {
			return null;
		} else {
			return ctx;
		}
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
		Deque<Holder<Task>> stack = currentTaskStackTl.get();
		if (stack == null) {
			stack = new ArrayDeque<>();
			currentTaskStackTl.set(stack);
		}
		stack.push(new Holder<>(task));
	}
	
	public static void popCurrentTask() {
		Deque<Holder<Task>> stack = currentTaskStackTl.get();
		stack.pop();
	}

	public static Task getCurrentTask() {
		Deque<Holder<Task>> stack = currentTaskStackTl.get();
		if (stack == null) {
			return null;
		}
		Holder<Task> holder = stack.peek();
		return holder != null ? holder.getValue() : null;
	}

	// TODO move to better place
	public static <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateExpressionInContext(Expression<PrismPropertyValue<T>,
			PrismPropertyDefinition<T>> expression, ExpressionEvaluationContext params, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {
		ModelExpressionThreadLocalHolder.pushCurrentResult(result);
		ModelExpressionThreadLocalHolder.pushCurrentTask(task);
		PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResultTriple;
		try {
			exprResultTriple = expression.evaluate(params);
		} finally {
			ModelExpressionThreadLocalHolder.popCurrentResult();
			ModelExpressionThreadLocalHolder.popCurrentTask();
		}
		return exprResultTriple;
	}
}
