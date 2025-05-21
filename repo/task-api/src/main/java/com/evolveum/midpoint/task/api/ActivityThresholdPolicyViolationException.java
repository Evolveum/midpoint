/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;

public class ActivityThresholdPolicyViolationException extends ThresholdPolicyViolationException {

    private final TaskRunResult.TaskRunResultStatus taskRunResultStatus;

    public ActivityThresholdPolicyViolationException(
            LocalizableMessage userFriendlyMessage, String technicalMessage, @NotNull TaskRunResult.TaskRunResultStatus taskRunResultStatus) {
        super(userFriendlyMessage, technicalMessage);

        this.taskRunResultStatus = taskRunResultStatus;
    }

    public TaskRunResult.TaskRunResultStatus getTaskRunResultStatus() {
        return taskRunResultStatus;
    }
}
