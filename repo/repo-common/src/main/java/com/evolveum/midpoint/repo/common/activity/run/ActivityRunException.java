/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Exception that carries supplementary information on how it should be treated
 * (with the respect to operation result and task run result status).
 *
 * See also {@link TaskException}.
 */
@Experimental
public class ActivityRunException extends Exception {

    @NotNull private final OperationResultStatus opResultStatus;
    @NotNull private final TaskRunResult.TaskRunResultStatus runResultStatus;

    public ActivityRunException(String message, @NotNull OperationResultStatus opResultStatus,
            @NotNull TaskRunResult.TaskRunResultStatus runResultStatus, Throwable cause) {
        super(message, cause);
        this.opResultStatus = opResultStatus;
        this.runResultStatus = runResultStatus;
    }

    public ActivityRunException(String message, @NotNull OperationResultStatus opResultStatus,
            @NotNull TaskRunResult.TaskRunResultStatus runResultStatus) {
        this(message, opResultStatus, runResultStatus, null);
    }

    public @NotNull OperationResultStatus getOpResultStatus() {
        return opResultStatus;
    }

    String getFullMessage() {
        Throwable cause = getCause();
        return getMessage() + (cause != null ? ": " + cause.getMessage() : "");
    }

    ActivityRunResult toActivityRunResult() {
        return new ActivityRunResult(opResultStatus, runResultStatus, getCause());
    }

    public TaskException toTaskException() {
        return new TaskException(getMessage(), opResultStatus, runResultStatus, getCause());
    }
}
