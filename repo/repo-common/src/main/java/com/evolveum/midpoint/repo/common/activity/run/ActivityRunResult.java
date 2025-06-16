/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.createStatusType;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.*;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.google.common.base.MoreObjects;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivitySimplifiedRealizationStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;

import org.jetbrains.annotations.Nullable;

/**
 * Result of an run of an activity.
 */
public class ActivityRunResult implements ShortDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityRunResult.class);

    /** Final operation result status to be reported and recorded. */
    private OperationResultStatus operationResultStatus;

    /** Indicates what to do with the task - what kind of error or exception situation has been occurred. */
    private TaskRunResultStatus runResultStatus;

    /** The original exception (if any). */
    private Throwable throwable;

    /** Optional message. Overrides the message in {@link #throwable}. */
    @Nullable private String message;

    public ActivityRunResult() {
    }

    public ActivityRunResult(OperationResultStatus operationResultStatus, TaskRunResultStatus runResultStatus) {
        this(operationResultStatus, runResultStatus, null);
    }

    public ActivityRunResult(
            OperationResultStatus operationResultStatus,
            TaskRunResultStatus runResultStatus,
            Throwable throwable) {
        this.operationResultStatus = operationResultStatus;
        this.runResultStatus = runResultStatus;
        this.throwable = throwable;
    }

    /**
     * Handles unexpected exception that occurred during run of an activity
     * at a place that expects {@link ActivityRunResult} to be returned.
     *
     * @param e Exception to be handled
     * @param opResult Operation result into which the exception should be recorded
     * @param activityRun Instance of activity run in which the exception is converted.
     * It is used just for logging purposes.
     */
    static @NotNull ActivityRunResult handleException(@NotNull Exception e, @NotNull OperationResult opResult,
            @NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        if (e instanceof ActivityRunException) {
            ActivityRunException aee = (ActivityRunException) e;
            OperationResultStatus status = aee.getOpResultStatus();
            if (status == WARNING) {
                LOGGER.warn("{}; in {}", e.getMessage(), activityRun.getDiagName());
            } else if (status == HANDLED_ERROR) {
                // Should we even log handled errors like this?
                LOGGER.warn("Handled error in {}: {}", activityRun.getDiagName(), e.getMessage(), e);
            } else if (status != SUCCESS && status != NOT_APPLICABLE) {
                // What about other kinds of status (in progress? unknown? - they should not occur at this point)
                LoggingUtils.logUnexpectedException(LOGGER, "Exception in {}", e, activityRun.getDiagName());
            }
            opResult.recordStatus(status, aee.getFullMessage(), aee.getCause());
            return aee.toActivityRunResult();
        } else {
            LoggingUtils.logUnexpectedException(LOGGER, "Unhandled exception in {}", e, activityRun.getDiagName());
            opResult.recordFatalError(e);
            return ActivityRunResult.exception(FATAL_ERROR, PERMANENT_ERROR, e);
        }
    }

    public TaskRunResult createTaskRunResult() {
        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(
                MoreObjects.firstNonNull(runResultStatus, FINISHED));
        runResult.setOperationResultStatus(operationResultStatus);
        runResult.setThrowable(throwable);
        if (message != null) {
            runResult.setMessage(message);
        } else if (throwable != null) {
            runResult.setMessage(throwable.getMessage());
        }
        // progress is intentionally kept null (meaning "do not update it in the task")
        return runResult;
    }

    TaskRunResultStatus getRunResultStatus() {
        return runResultStatus;
    }

    void setRunResultStatus(TaskRunResultStatus runResultStatus) {
        this.runResultStatus = runResultStatus;
    }

    void setRunResultStatus(TaskRunResultStatus runResultStatus, Throwable throwable) {
        this.runResultStatus = runResultStatus;
        this.throwable = throwable;
    }

    OperationResultStatus getOperationResultStatus() {
        return operationResultStatus;
    }

    void setOperationResultStatus(OperationResultStatus operationResultStatus) {
        this.operationResultStatus = operationResultStatus;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    static ActivityRunResult standardResult(boolean canRun) {
        return canRun ? finished(SUCCESS) : interrupted();
    }

    public static ActivityRunResult success() {
        return finished(SUCCESS);
    }

    public static ActivityRunResult interrupted() {
        return new ActivityRunResult(IN_PROGRESS, INTERRUPTED);
    }

    public static ActivityRunResult finished(OperationResultStatus opResultStatus) {
        return new ActivityRunResult(opResultStatus, FINISHED);
    }

    public static ActivityRunResult finished(OperationResultStatusType opResultStatusBean) {
        return new ActivityRunResult(parseStatusType(opResultStatusBean), FINISHED);
    }

    public static ActivityRunResult waiting() {
        return new ActivityRunResult(OperationResultStatus.IN_PROGRESS, IS_WAITING);
    }

    public static ActivityRunResult exception(OperationResultStatus opStatus, TaskRunResultStatus runStatus,
            Throwable throwable) {
        return new ActivityRunResult(opStatus, runStatus, throwable);
    }

    @Override
    public String toString() {
        return "ActivityRunResult{" +
                "opStatus=" + operationResultStatus +
                ", runStatus=" + runResultStatus +
                (throwable != null ? ", throwable=" + throwable : "") +
                (message != null ? ", message=" + message : "") +
                '}';
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("opStatus: ").append(operationResultStatus);
        sb.append(", runStatus: ").append(runResultStatus);
        if (throwable != null) {
            sb.append(", throwable: ").append(throwable);
        }
        if (message != null) {
            sb.append(", message: ").append(message);
        }
    }

    public ActivityRunResult message(String message) {
        this.message = message;
        return this;
    }

    public boolean isError() {
        return runResultStatus == PERMANENT_ERROR
                || runResultStatus == TEMPORARY_ERROR
                || runResultStatus == HALTING_ERROR
                || runResultStatus == SKIP_ACTIVITY_ERROR
                || runResultStatus == RESTART_ACTIVITY_ERROR;
    }

    boolean isPermanentError() {
        return runResultStatus == PERMANENT_ERROR;
    }

    boolean isHaltingError() {
        return runResultStatus == HALTING_ERROR;
    }

    boolean isTemporaryError() {
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

    public boolean isRestartActivityError() {
        return runResultStatus == RESTART_ACTIVITY_ERROR;
    }

    public boolean isSkipActivityError() {
        return runResultStatus == SKIP_ACTIVITY_ERROR;
    }

    public ActivitySimplifiedRealizationStateType getSimplifiedRealizationState() {
        return isFinished() ? ActivitySimplifiedRealizationStateType.COMPLETE : ActivitySimplifiedRealizationStateType.IN_PROGRESS;
    }

    public OperationResultStatusType getOperationResultStatusBean() {
        return createStatusType(operationResultStatus);
    }

    /**
     * "Closes" the result by converting null or "in progress" values into finished/interrupted/success/default ones.
     */
    public void close(boolean canRun, OperationResultStatus status) {
        if (runResultStatus == null) {
            runResultStatus = canRun ? TaskRunResultStatus.FINISHED : TaskRunResultStatus.INTERRUPTED;
        }
        if (operationResultStatus == null) {
            operationResultStatus = status;
        }
        if (isFinished()
                && (operationResultStatus == null || operationResultStatus == OperationResultStatus.IN_PROGRESS)) {
            operationResultStatus = OperationResultStatus.SUCCESS;
        }
    }
}
