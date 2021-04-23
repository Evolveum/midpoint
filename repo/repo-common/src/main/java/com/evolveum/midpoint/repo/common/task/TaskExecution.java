/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an execution of a task.
 */
public interface TaskExecution {

    /**
     * Passes control to the execution object.
     *
     * The object is now responsible for the whole execution of this task.
     * @param result
     */
    @NotNull TaskRunResult run(OperationResult result) throws TaskException, PreconditionViolationException, CommonException;

    /**
     * Returns the task associated with this execution.
     */
    @NotNull RunningTask getTask();

    default Long heartbeat() {
        return null;
    }
}
