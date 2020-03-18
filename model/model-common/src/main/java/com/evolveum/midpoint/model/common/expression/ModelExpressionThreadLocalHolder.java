/*
 * Copyright (c) 2013-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import java.util.ArrayDeque;
import java.util.Deque;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
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

    private static ThreadLocal<Deque<ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition>>> expressionEnvironmentStackTl =
            new ThreadLocal<>();

    public static <F extends ObjectType,V extends PrismValue, D extends ItemDefinition> void pushExpressionEnvironment(ExpressionEnvironment<F,V,D> env) {
        Deque<ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition>> stack = expressionEnvironmentStackTl.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            expressionEnvironmentStackTl.set(stack);
        }
        stack.push((ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition>)env);
    }

    public static <F extends ObjectType,V extends PrismValue, D extends ItemDefinition> void popExpressionEnvironment() {
        Deque<ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition>> stack = expressionEnvironmentStackTl.get();
        stack.pop();
    }

    public static <F extends ObjectType,V extends PrismValue, D extends ItemDefinition> ExpressionEnvironment<F,V,D> getExpressionEnvironment() {
        Deque<ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition>> stack = expressionEnvironmentStackTl.get();
        if (stack == null) {
            return null;
        }
        return (ExpressionEnvironment<F,V,D>) stack.peek();
    }

    public static <F extends ObjectType,V extends PrismValue, D extends ItemDefinition> ModelContext<F> getLensContext() {
        ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return (ModelContext<F>) env.getLensContext();
    }

    public static <F extends ObjectType,V extends PrismValue, D extends ItemDefinition> Mapping<V,D> getMapping() {
        ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return (Mapping<V,D>) env.getMapping();
    }

    public static <F extends ObjectType,V extends PrismValue, D extends ItemDefinition> ModelProjectionContext getProjectionContext() {
        ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return env.getProjectionContext();
    }

    public static Task getCurrentTask() {
        ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return env.getCurrentTask();
    }

    public static OperationResult getCurrentResult() {
        ExpressionEnvironment<ObjectType,PrismValue,ItemDefinition> env = getExpressionEnvironment();
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
            return expression.evaluate(context, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateExpressionInContext(Expression<PrismPropertyValue<T>,
            PrismPropertyDefinition<T>> expression, ExpressionEvaluationContext eeContext, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
        try {
            return expression.evaluate(eeContext, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static PrismValueDeltaSetTriple<PrismReferenceValue> evaluateRefExpressionInContext(Expression<PrismReferenceValue,
            PrismReferenceDefinition> expression, ExpressionEvaluationContext eeContext, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
        try {
            return expression.evaluate(eeContext, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateExpressionInContext(
            Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> expression,
            ExpressionEvaluationContext eeContext,
            ExpressionEnvironment<?, ?, ?> env, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(env);
        PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResultTriple;
        try {
            exprResultTriple = expression.evaluate(eeContext, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
        return exprResultTriple;
    }
}
