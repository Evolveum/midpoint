/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.task.api.TaskRunResult;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;

public class ActivityThresholdPolicyViolationException extends ThresholdPolicyViolationException {

    private final TaskRunResult.TaskRunResultStatus taskRunResultStatus;

    private final String ruleId;

    public ActivityThresholdPolicyViolationException(
            LocalizableMessage userFriendlyMessage,
            String technicalMessage,
            @NotNull TaskRunResult.TaskRunResultStatus taskRunResultStatus,
            @NotNull String ruleId) {

        super(userFriendlyMessage, technicalMessage);

        this.taskRunResultStatus = taskRunResultStatus;
        this.ruleId = ruleId;
    }

    public TaskRunResult.TaskRunResultStatus getTaskRunResultStatus() {
        return taskRunResultStatus;
    }

    public String getRuleId() {
        return ruleId;
    }
}
