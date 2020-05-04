/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author semancik
 *
 */
public class DiagnosticContextHolder {

    private static final ThreadLocal<Deque<DiagnosticContext>> DIAG_STACK_THREAD_LOCAL = new ThreadLocal<>();

    public static void push(DiagnosticContext ctx) {
        Deque<DiagnosticContext> stack = DIAG_STACK_THREAD_LOCAL.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            DIAG_STACK_THREAD_LOCAL.set(stack);
        }
        stack.push(ctx);
    }

    public static DiagnosticContext pop() {
        Deque<DiagnosticContext> stack = DIAG_STACK_THREAD_LOCAL.get();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }

    public static DiagnosticContext get() {
        Deque<DiagnosticContext> stack = DIAG_STACK_THREAD_LOCAL.get();
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
