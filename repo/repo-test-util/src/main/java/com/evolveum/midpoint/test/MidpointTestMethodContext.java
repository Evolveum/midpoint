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
 * Value object carrying test context information like task, result and method name.
 * <p>
 * Static methods are used for creating the context and working with it via {@link ThreadLocal}.
 * <b>It is important to to call {@link #destroy()} at the end (in some after-method).</b>
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

    public static MidpointTestMethodContext create(String methodName, Task task, OperationResult result) {
        MidpointTestMethodContext ctx = new MidpointTestMethodContext(methodName, task, result);
        TEST_CONTEXT_THREAD_LOCAL.set(ctx);
        return ctx;
    }

    public static MidpointTestMethodContext get() {
        return TEST_CONTEXT_THREAD_LOCAL.get();
    }

    public static void destroy() {
        TEST_CONTEXT_THREAD_LOCAL.remove();
    }
}
