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
public class MidpointTestMethodContext {

    private static final ThreadLocal<MidpointTestMethodContext> testContextThreadLocal = new ThreadLocal<>();

    /**
     * Task used to execute the test.
     */
    private final Task task;

    /**
     * Top-level operation result for test execution.
     */
    private final OperationResult result;

    private MidpointTestMethodContext(Task task, OperationResult result) {
        this.task = task;
        this.result = result;
    }

    public Task getTask() {
        return task;
    }

    public OperationResult getResult() {
        return result;
    }

    public static MidpointTestMethodContext get() {
        return testContextThreadLocal.get();
    }

    public static MidpointTestMethodContext setup(Task task, OperationResult result) {
        MidpointTestMethodContext ctx = new MidpointTestMethodContext(task, result);
        testContextThreadLocal.set(ctx);
        return ctx;
    }

    public static void destroy() {
        testContextThreadLocal.remove();
    }
}
