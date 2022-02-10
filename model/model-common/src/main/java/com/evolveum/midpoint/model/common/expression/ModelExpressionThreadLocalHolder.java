/*
 * Copyright (c) 2013-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

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

import org.jetbrains.annotations.NotNull;

/**
 * Holds {@link ExpressionEnvironment} (containing e.g. lens context, projection context, mapping, and task) to be used
 * from withing scripts and methods that are called from scripts.
 *
 * @author Radovan Semancik
 */
public class ModelExpressionThreadLocalHolder {

    private static final ThreadLocal<Deque<ExpressionEnvironment<?, ?, ?>>>
            expressionEnvironmentStackTl = new ThreadLocal<>();

    public static void pushExpressionEnvironment(ExpressionEnvironment<?, ?, ?> env) {
        Deque<ExpressionEnvironment<?, ?, ?>> stack = expressionEnvironmentStackTl.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            expressionEnvironmentStackTl.set(stack);
        }
        stack.push(env);
    }

    public static void popExpressionEnvironment() {
        Deque<ExpressionEnvironment<?, ?, ?>> stack = expressionEnvironmentStackTl.get();
        stack.pop();
    }

    public static <F extends ObjectType, V extends PrismValue, D extends ItemDefinition<?>>
    ExpressionEnvironment<F, V, D> getExpressionEnvironment() {
        Deque<ExpressionEnvironment<?, ?, ?>> stack = expressionEnvironmentStackTl.get();
        if (stack == null) {
            return null;
        }
        //noinspection unchecked
        return (ExpressionEnvironment<F, V, D>) stack.peek();
    }

    public static <F extends ObjectType> ModelContext<F> getLensContext() {
        ExpressionEnvironment<?, ?, ?> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        //noinspection unchecked
        return (ModelContext<F>) env.getLensContext();
    }

    @NotNull
    public static <F extends ObjectType> ModelContext<F> getLensContextRequired() {
        return Objects.requireNonNull(getLensContext(), "No lens context");
    }

    public static <V extends PrismValue, D extends ItemDefinition<?>> Mapping<V,D> getMapping() {
        ExpressionEnvironment<?, ?, ?> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        //noinspection unchecked
        return (Mapping<V,D>) env.getMapping();
    }

    public static ModelProjectionContext getProjectionContext() {
        ExpressionEnvironment<?, ?, ?> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return env.getProjectionContext();
    }

    public static Task getCurrentTask() {
        ExpressionEnvironment<?, ?, ?> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return env.getCurrentTask();
    }

    public static OperationResult getCurrentResult() {
        ExpressionEnvironment<?, ?, ?> env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return env.getCurrentResult();
    }

    // TODO move to better place
    public static PrismValueDeltaSetTriple<?> evaluateAnyExpressionInContext(Expression<?, ?> expression,
            ExpressionEvaluationContext context, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
        try {
            return expression.evaluate(context, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateExpressionInContext(
            Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> expression,
            ExpressionEvaluationContext eeContext,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
        try {
            return expression.evaluate(eeContext, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static PrismValueDeltaSetTriple<PrismReferenceValue> evaluateRefExpressionInContext(
            Expression<PrismReferenceValue, PrismReferenceDefinition> expression,
            ExpressionEvaluationContext eeContext,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
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
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
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
