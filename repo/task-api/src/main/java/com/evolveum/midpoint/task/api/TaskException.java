/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Exception that carries supplementary information on how it should be treated
 * (with the respect to operation result and task run result status).
 *
 * Very experimental.
 *
 * Currently not supported directly by task-quartz-impl module.
 * (Only by helper classes in higher layers.) This will be changed.
 */
@Experimental
public class TaskException extends Exception {

    private final OperationResultStatus opResultStatus;
    private final TaskRunResult.TaskRunResultStatus runResultStatus;

    public TaskException(String message, OperationResultStatus opResultStatus,
            TaskRunResult.TaskRunResultStatus runResultStatus, Throwable cause) {
        super(message, cause);
        this.opResultStatus = opResultStatus;
        this.runResultStatus = runResultStatus;
    }

    public TaskException(String message, OperationResultStatus opResultStatus,
            TaskRunResult.TaskRunResultStatus runResultStatus) {
        this(message, opResultStatus, runResultStatus, null);
    }

    public OperationResultStatus getOpResultStatus() {
        return opResultStatus;
    }

    public TaskRunResult.TaskRunResultStatus getRunResultStatus() {
        return runResultStatus;
    }

}
