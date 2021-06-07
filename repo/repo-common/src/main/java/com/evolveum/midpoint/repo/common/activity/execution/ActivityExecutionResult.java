/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import com.evolveum.midpoint.repo.common.task.ErrorState;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;

import com.evolveum.midpoint.util.ShortDumpable;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

/**
 * Result of an execution of an activity.
 */
public class ActivityExecutionResult implements ShortDumpable {

    // TODO restrict to supported values only (permanent/temporary error, finished)
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

    public static ActivityExecutionResult exception(TaskRunResultStatus status, Throwable t) {
        // TODO what with t?
        return new ActivityExecutionResult(status);
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

    public void update(ErrorState errorState) {
        Throwable stoppingException = errorState.getStoppingException();
        if (stoppingException != null) {
            runResultStatus = TaskRunResultStatus.PERMANENT_ERROR;
            // TODO In the future we should distinguish between permanent and temporary errors here.
        }
    }

    public void update(@NotNull ActivityExecutionResult childExecutionResult) {
        if (childExecutionResult.isPermanentError()) {
            runResultStatus = TaskRunResultStatus.PERMANENT_ERROR;
        } else if (childExecutionResult.isTemporaryError()) {
            runResultStatus = TaskRunResultStatus.TEMPORARY_ERROR;
        } else {
            // probably OK
        }
    }

    public boolean isError() {
        assert runResultStatus != TaskRunResultStatus.INTERRUPTED;
        assert runResultStatus != TaskRunResultStatus.IS_WAITING;
        return runResultStatus == TaskRunResultStatus.PERMANENT_ERROR || runResultStatus == TaskRunResultStatus.TEMPORARY_ERROR;
    }

    public boolean isPermanentError() {
        return runResultStatus == TaskRunResultStatus.PERMANENT_ERROR;
    }

    public boolean isTemporaryError() {
        return runResultStatus == TaskRunResultStatus.TEMPORARY_ERROR;
    }

    public void markFinishedIfNoError() {
        if (runResultStatus == null) {
            runResultStatus = TaskRunResultStatus.FINISHED;
        }
    }

    public boolean isFinished() {
        return runResultStatus == TaskRunResultStatus.FINISHED;
    }
}
