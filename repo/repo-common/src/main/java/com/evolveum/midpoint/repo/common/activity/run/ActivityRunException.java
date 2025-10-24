/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.task.api.TaskException;
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
    @NotNull private final ActivityRunResultStatus runResultStatus;

    public ActivityRunException(String message, @NotNull OperationResultStatus opResultStatus,
            @NotNull ActivityRunResultStatus runResultStatus, Throwable cause) {
        super(message, cause);
        this.opResultStatus = opResultStatus;
        this.runResultStatus = runResultStatus;
    }

    public ActivityRunException(String message, @NotNull OperationResultStatus opResultStatus,
            @NotNull ActivityRunResultStatus runResultStatus) {
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
        return new TaskException(getMessage(), opResultStatus, runResultStatus.toTaskRunResultStatus(), getCause());
    }
}
