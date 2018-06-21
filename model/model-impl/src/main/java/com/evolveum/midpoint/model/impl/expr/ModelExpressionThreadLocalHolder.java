/*
 * Copyright (c) 2013-2018 Evolveum
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

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Radovan Semancik
 *
 */
public class ModelExpressionThreadLocalHolder {

	private static ThreadLocal<Deque<ExpressionEnvironment<ObjectType>>> expressionEnvironmentStackTl =
			new ThreadLocal<>();

	public static <F extends ObjectType> void pushExpressionEnvironment(ExpressionEnvironment<F> env) {
		Deque<ExpressionEnvironment<ObjectType>> stack = expressionEnvironmentStackTl.get();
		if (stack == null) {
			stack = new ArrayDeque<>();
			expressionEnvironmentStackTl.set(stack);
		}
		stack.push((ExpressionEnvironment<ObjectType>)env);
	}

	public static <F extends ObjectType> void popExpressionEnvironment() {
		Deque<ExpressionEnvironment<ObjectType>> stack = expressionEnvironmentStackTl.get();
		stack.pop();
	}

	public static <F extends ObjectType> ExpressionEnvironment<F> getExpressionEnvironment() {
		Deque<ExpressionEnvironment<ObjectType>> stack = expressionEnvironmentStackTl.get();
		if (stack == null) {
			return null;
		}
		return (ExpressionEnvironment<F>) stack.peek();
	}

	public static <F extends ObjectType> LensContext<F> getLensContext() {
		ExpressionEnvironment<ObjectType> env = getExpressionEnvironment();
		if (env == null) {
			return null;
		}
		return (LensContext<F>) env.getLensContext();
	}

	public static <F extends ObjectType> LensProjectionContext getProjectionContext() {
		ExpressionEnvironment<ObjectType> env = getExpressionEnvironment();
		if (env == null) {
			return null;
		}
		return env.getProjectionContext();
	}

	public static Task getCurrentTask() {
		ExpressionEnvironment<ObjectType> env = getExpressionEnvironment();
		if (env == null) {
			return null;
		}
		return env.getCurrentTask();
	}

	public static OperationResult getCurrentResult() {
		ExpressionEnvironment<ObjectType> env = getExpressionEnvironment();
		if (env == null) {
			return null;
		}
		return env.getCurrentResult();
	}

	// TODO move to better place
	public static PrismValueDeltaSetTriple<?> evaluateAnyExpressionInContext(Expression<?, ?> expression,
			ExpressionEvaluationContext context, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
		try {
			return expression.evaluate(context);
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
	}

	public static <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateExpressionInContext(Expression<PrismPropertyValue<T>,
			PrismPropertyDefinition<T>> expression, ExpressionEvaluationContext eeContext, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
		try {
			return expression.evaluate(eeContext);
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
	}

	public static PrismValueDeltaSetTriple<PrismReferenceValue> evaluateRefExpressionInContext(Expression<PrismReferenceValue,
			PrismReferenceDefinition> expression, ExpressionEvaluationContext eeContext, Task task, OperationResult result)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
		try {
			return expression.evaluate(eeContext);
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
	}

	public static <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateExpressionInContext(
			Expression<PrismPropertyValue<T>,PrismPropertyDefinition<T>> expression,
			ExpressionEvaluationContext eeContext,
			ExpressionEnvironment<?> env)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
		PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResultTriple;
		try {
			exprResultTriple = expression.evaluate(eeContext);
		} finally {
			ModelExpressionThreadLocalHolder.popExpressionEnvironment();
		}
		return exprResultTriple;
	}
}
