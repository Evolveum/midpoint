/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.task;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an execution of a task.
 */
public interface TaskExecution extends ShortDumpable {

    /**
     * Passes control to the execution object.
     *
     * The object is now responsible for the whole execution of this task.
     */
    @NotNull TaskRunResult run(OperationResult result) throws TaskException;

    /**
     * Returns the task associated with this execution.
     */
    @NotNull RunningTask getRunningTask();

    @NotNull CommonTaskBeans getBeans();

    default Long heartbeat() {
        return null;
    }

    default void shortDump(StringBuilder sb) {
        sb.append("execution of ").append(getRunningTask());
    }

    default Task getRootTask() {
        return getRunningTask().getRootTask();
    }
}
