/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.task.api.Task;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds {@link ExpressionEnvironment} (containing e.g. current task and operation result; or other items in the subclasses)
 * to be used from withing scripts and methods that are called from scripts.
 *
 * @author Radovan Semancik
 */
public class ExpressionEnvironmentThreadLocalHolder {

    private static final ThreadLocal<Deque<ExpressionEnvironment>> EXPRESSION_ENVIRONMENT_STACK_TL = new ThreadLocal<>();

    /** Just a shortcut method. */
    public static void pushExpressionEnvironment(Task task, OperationResult result) {
        pushExpressionEnvironment(new ExpressionEnvironment(task, result));
    }

    public static void pushExpressionEnvironment(ExpressionEnvironment env) {
        Deque<ExpressionEnvironment> stack = EXPRESSION_ENVIRONMENT_STACK_TL.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            EXPRESSION_ENVIRONMENT_STACK_TL.set(stack);
        }
        stack.push(env);
    }

    public static void popExpressionEnvironment() {
        Deque<ExpressionEnvironment> stack = EXPRESSION_ENVIRONMENT_STACK_TL.get();
        stack.pop();
    }

    public static ExpressionEnvironment getExpressionEnvironment() {
        Deque<ExpressionEnvironment> stack = EXPRESSION_ENVIRONMENT_STACK_TL.get();
        return stack != null ? stack.peek() : null;
    }

    public static Task getCurrentTask() {
        ExpressionEnvironment env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return env.getCurrentTask();
    }

    public static OperationResult getCurrentResult() {
        ExpressionEnvironment env = getExpressionEnvironment();
        if (env == null) {
            return null;
        }
        return env.getCurrentResult();
    }
}
