/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.task.api.TaskRunResult.createFailureTaskRunResult;

/**
 * Exception signalling we should exit the task handler execution.
 *
 * TODO shouldn't we distinguish between "internal" (in task-quartz-impl) and "external" use of this exception?
 */
@Experimental
public class ExitExecutionException extends Exception {

    @NotNull private final TaskRunResult runResult;

    public ExitExecutionException(@NotNull TaskRunResult runResult) {
        this.runResult = runResult;
    }

    public ExitExecutionException(RunningTask task, String message, Throwable cause) {
        this.runResult = createFailureTaskRunResult(task, message, cause);
    }

    @NotNull public TaskRunResult getRunResult() {
        return runResult;
    }
}
