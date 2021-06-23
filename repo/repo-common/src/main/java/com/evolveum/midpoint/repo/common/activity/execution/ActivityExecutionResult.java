/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.evolveum.midpoint.schema.util.OperationResultUtil;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySimplifiedRealizationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Result of an execution of an activity.
 */
public class ActivityExecutionResult implements ShortDumpable {

    /** TODO */
    private OperationResultStatus operationResultStatus;

    /** TODO */
    private TaskRunResultStatus runResultStatus;

    public ActivityExecutionResult() {
    }

    public ActivityExecutionResult(OperationResultStatus operationResultStatus, TaskRunResultStatus runResultStatus) {
        this.runResultStatus = runResultStatus;
        this.operationResultStatus = operationResultStatus;
    }

    public TaskRunResult createTaskRunResult() {
        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(
                MoreObjects.firstNonNull(runResultStatus, FINISHED));
        runResult.setOperationResultStatus(operationResultStatus);
        // progress and operation result are intentionally kept null (meaning "do not update these in the task")
        return runResult;
    }

    public TaskRunResultStatus getRunResultStatus() {
        return runResultStatus;
    }

    public void setRunResultStatus(TaskRunResultStatus runResultStatus) {
        this.runResultStatus = runResultStatus;
    }

    public OperationResultStatus getOperationResultStatus() {
        return operationResultStatus;
    }

    public void setOperationResultStatus(OperationResultStatus operationResultStatus) {
        this.operationResultStatus = operationResultStatus;
    }

    public static ActivityExecutionResult standardResult(boolean canRun) {
        return canRun ? finished(SUCCESS) : interrupted();
    }

    public static ActivityExecutionResult success() {
        return finished(SUCCESS);
    }

    public static ActivityExecutionResult interrupted() {
        return new ActivityExecutionResult(IN_PROGRESS, INTERRUPTED);
    }

    public static ActivityExecutionResult finished(OperationResultStatus opResultStatus) {
        return new ActivityExecutionResult(opResultStatus, FINISHED);
    }

    public static ActivityExecutionResult finished(OperationResultStatusType opResultStatusBean) {
        return new ActivityExecutionResult(parseStatusType(opResultStatusBean), FINISHED);
    }

    public static ActivityExecutionResult waiting() {
        return new ActivityExecutionResult(OperationResultStatus.IN_PROGRESS, IS_WAITING);
    }

    public static ActivityExecutionResult exception(OperationResultStatus opStatus, TaskRunResultStatus runStatus, Throwable t) {
        // TODO what with t?
        return new ActivityExecutionResult(opStatus, runStatus);
    }

    @Override
    public String toString() {
        return "ActivityExecutionResult{" +
                "opStatus=" + operationResultStatus +
                ", runStatus=" + runResultStatus +
                '}';
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("opStatus: ").append(operationResultStatus);
        sb.append(", runStatus: ").append(runResultStatus);
    }

    // TODO move to AbstractCompositeActivityExecution?
    void updateRunResultStatus(@NotNull ActivityExecutionResult childExecutionResult, boolean canRun) {
        assert runResultStatus == null;
        if (childExecutionResult.isInterrupted() || !canRun) {
            runResultStatus = INTERRUPTED;
        } else if (childExecutionResult.isPermanentError()) {
            runResultStatus = PERMANENT_ERROR;
        } else if (childExecutionResult.isTemporaryError()) {
            runResultStatus = TEMPORARY_ERROR;
        } else if (childExecutionResult.isWaiting()) {
            runResultStatus = IS_WAITING;
        }
    }

    // TODO move to AbstractCompositeActivityExecution?
    void updateOperationResultStatus(List<ActivityExecutionResult> childResults) {
        if (isWaiting() || isInterrupted()) {
            operationResultStatus = IN_PROGRESS;
            return;
        }

        Set<OperationResultStatus> statuses = childResults.stream()
                .map(r -> r.operationResultStatus)
                .collect(Collectors.toSet());

        // Note that we intentionally do not check the _run_result_ being error here.
        // We rely on the fact that in the case of temporary/permanent error the appropriate
        // operation result status should be set as well.
        operationResultStatus = OperationResultUtil.aggregateFinishedResults(statuses);
    }

    public boolean isError() {
        return runResultStatus == PERMANENT_ERROR || runResultStatus == TEMPORARY_ERROR;
    }

    public boolean isPermanentError() {
        return runResultStatus == PERMANENT_ERROR;
    }

    public boolean isTemporaryError() {
        return runResultStatus == TEMPORARY_ERROR;
    }

    public boolean isFinished() {
        return runResultStatus == FINISHED;
    }

    public boolean isWaiting() {
        return runResultStatus == IS_WAITING;
    }

    public boolean isInterrupted() {
        return runResultStatus == INTERRUPTED;
    }

    public ActivitySimplifiedRealizationStateType getSimplifiedRealizationState() {
        return isFinished() ? ActivitySimplifiedRealizationStateType.COMPLETE : ActivitySimplifiedRealizationStateType.IN_PROGRESS;
    }

    public ActivityExecutionStateType getExecutionState() {
        return isFinished() ? ActivityExecutionStateType.COMPLETE : ActivityExecutionStateType.NOT_EXECUTING;
    }
}
