/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 *  Thread-local context for midPoint test method.
 */
public final class MidpointTestMethodContext {

    private static final ThreadLocal<MidpointTestMethodContext> TEST_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Test method name.
     */
    private final String methodName;

    /**
     * Task used to execute the test.
     */
    private final Task task;

    /**
     * Top-level operation result for test execution.
     */
    private final OperationResult result;

    private MidpointTestMethodContext(String methodName, Task task, OperationResult result) {
        this.methodName = methodName;
        this.task = task;
        this.result = result;
    }

    public String getMethodName() {
        return methodName;
    }

    public Task getTask() {
        return task;
    }

    public OperationResult getResult() {
        return result;
    }

    public static MidpointTestMethodContext get() {
        return TEST_CONTEXT_THREAD_LOCAL.get();
    }

    public static MidpointTestMethodContext setup(String methodName, Task task, OperationResult result) {
        MidpointTestMethodContext ctx = new MidpointTestMethodContext(methodName, task, result);
        TEST_CONTEXT_THREAD_LOCAL.set(ctx);
        return ctx;
    }

    public static void destroy() {
        TEST_CONTEXT_THREAD_LOCAL.remove();
    }
}
