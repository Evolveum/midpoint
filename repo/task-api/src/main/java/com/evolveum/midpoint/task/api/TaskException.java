/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Exception that carries supplementary information on how it should be treated
 * (with the respect to operation result and task run result status).
 */
@Experimental
public class TaskException extends Exception {

    @NotNull private final OperationResultStatus opResultStatus;
    @NotNull private final TaskRunResult.TaskRunResultStatus runResultStatus;

    public TaskException(String message, @NotNull OperationResultStatus opResultStatus,
            @NotNull TaskRunResult.TaskRunResultStatus runResultStatus, Throwable cause) {
        super(message, cause);
        this.opResultStatus = opResultStatus;
        this.runResultStatus = runResultStatus;
    }

    public TaskException(String message, @NotNull OperationResultStatus opResultStatus,
            @NotNull TaskRunResult.TaskRunResultStatus runResultStatus) {
        this(message, opResultStatus, runResultStatus, null);
    }

    public @NotNull OperationResultStatus getOpResultStatus() {
        return opResultStatus;
    }

    public @NotNull TaskRunResult.TaskRunResultStatus getRunResultStatus() {
        return runResultStatus;
    }

    public String getFullMessage() {
        Throwable cause = getCause();
        return getMessage() + (cause != null ? ": " + cause.getMessage() : "");
    }
}
