/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Exception that carries supplementary information on how it should be treated
 * (with the respect to operation result and task run result status).
 */
public class TaskException extends Exception {

    @NotNull private final OperationResultStatus opResultStatus;
    @NotNull private final TaskRunResult.TaskRunResultStatus runResultStatus;

    public TaskException(
            String message,
            @NotNull OperationResultStatus opResultStatus,
            @NotNull TaskRunResult.TaskRunResultStatus runResultStatus,
            Throwable cause) {
        super(message, cause);
        this.opResultStatus = opResultStatus;
        this.runResultStatus = runResultStatus;
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull OperationResultStatus getOpResultStatus() {
        return opResultStatus;
    }

    public @NotNull TaskRunResult.TaskRunResultStatus getRunResultStatus() {
        return runResultStatus;
    }

    @SuppressWarnings("WeakerAccess")
    public String getFullMessage() {
        Throwable cause = getCause();
        return getMessage() + (cause != null ? ": " + cause.getMessage() : "");
    }
}
