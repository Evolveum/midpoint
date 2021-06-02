/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;

import com.evolveum.midpoint.util.ShortDumpable;

import com.google.common.base.MoreObjects;

/**
 * Result of an execution of an activity.
 */
public class ActivityExecutionResult implements ShortDumpable {

    private TaskRunResultStatus runResultStatus;

    public ActivityExecutionResult() {
    }

    public ActivityExecutionResult(TaskRunResultStatus runResultStatus) {
        this.runResultStatus = runResultStatus;
    }

    public TaskRunResult getTaskRunResult() {
        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(
                MoreObjects.firstNonNull(runResultStatus, TaskRunResultStatus.FINISHED));
        // progress and operation result are intentionally kept null (meaning "do not update these in the task")
        return runResult;
    }

    public void setRunResultStatus(TaskRunResultStatus runResultStatus) {
        this.runResultStatus = runResultStatus;
    }

    public TaskRunResultStatus getRunResultStatus() {
        return runResultStatus;
    }

    public static ActivityExecutionResult finished() {
        return new ActivityExecutionResult(TaskRunResultStatus.FINISHED);
    }

    @Override
    public String toString() {
        return "ActivityExecutionResult{" +
                "runResultStatus=" + runResultStatus +
                '}';
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("status: ").append(runResultStatus);
    }
}
