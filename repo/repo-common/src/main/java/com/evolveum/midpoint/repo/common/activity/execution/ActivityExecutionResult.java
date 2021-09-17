/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.execution;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.createStatusType;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.*;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySimplifiedRealizationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

/**
 * Result of an execution of an activity.
 */
public class ActivityExecutionResult implements ShortDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityExecutionResult.class);

    /** Final operation result status to be reported and recorded. */
    private OperationResultStatus operationResultStatus;

    /** Indicates what to do with the task - what kind of error or execution situation has been occurred. */
    private TaskRunResultStatus runResultStatus;

    public ActivityExecutionResult() {
    }

    public ActivityExecutionResult(OperationResultStatus operationResultStatus, TaskRunResultStatus runResultStatus) {
        this.runResultStatus = runResultStatus;
        this.operationResultStatus = operationResultStatus;
    }

    /**
     * Handles unexpected exception that occurred during execution of an activity
     * at a place that expects {@link ActivityExecutionResult} to be returned.
     *
     * @param e Exception to be handled
     * @param context Instance of activity execution (or other object) in which the exception is converted
     */
    static @NotNull ActivityExecutionResult handleException(@NotNull Exception e, Object context) {
        if (e instanceof ActivityExecutionException) {
            ActivityExecutionException aee = (ActivityExecutionException) e;
            if (aee.getOpResultStatus() != SUCCESS) {
                LoggingUtils.logUnexpectedException(LOGGER, "Exception in {}", e, context);
            }
            return aee.toActivityExecutionResult();
        } else {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception in {}", e, context);
            return ActivityExecutionResult.exception(FATAL_ERROR, PERMANENT_ERROR, e);
        }
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

    public OperationResultStatusType getOperationResultStatusBean() {
        return createStatusType(operationResultStatus);
    }
}
