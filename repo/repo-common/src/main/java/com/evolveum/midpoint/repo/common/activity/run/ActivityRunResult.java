/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.*;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.*;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import com.evolveum.midpoint.repo.common.activity.AbortingInformationAware;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Result of an run of an activity.
 *
 * Fields are currently not final, in order to allow setting them stepwise.
 */
public class ActivityRunResult implements ShortDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityRunResult.class);

    /** Final operation result status to be reported and recorded. Should be non-null after run result is finished. */
    private OperationResultStatus operationResultStatus;

    /**
     * Indicates what to do with the activity and/or the task - what kind of error or exception situation has been occurred.
     * Should be non-null after run result is finished.
     */
    private ActivityRunResultStatus runResultStatus;

    /** The original exception (if any). */
    private Throwable throwable;

    /** Optional message. Overrides the message in {@link #throwable}. */
    @Nullable private String message;

    /**
     * Details for {@link #runResultStatus} being {@link ActivityRunResultStatus#ABORTED}.
     * The same as is later written to {@link ActivityStateType#F_ABORTING_INFORMATION}.
     * Produced by the activity run, written to the state at appropriate place in {@link AbstractActivityRun}.
     */
    @Nullable private ActivityAbortingInformationType abortingInformation;

    /** Details for {@link #runResultStatus} being {@link ActivityRunResultStatus#RESTART_REQUESTED}. */
    @Nullable private RestartRequestingInformation restartRequestingInformation;

    public ActivityRunResult() {
    }

    public ActivityRunResult(
            @NotNull OperationResultStatus operationResultStatus,
            @NotNull ActivityRunResultStatus runResultStatus) {
        this(operationResultStatus, runResultStatus, null, null);
    }

    private ActivityRunResult(
            @NotNull OperationResultStatus operationResultStatus,
            @NotNull ActivityRunResultStatus runResultStatus,
            @Nullable Throwable throwable,
            @Nullable ActivityAbortingInformationType abortingInformation) {
        this.operationResultStatus = operationResultStatus;
        this.runResultStatus = runResultStatus;
        this.throwable = throwable;
        this.abortingInformation = abortingInformation;
    }

    //region Static factory and utility methods
    /**
     * Converts exception into {@link ActivityRunResult}.
     *
     * Used when the exception is not {@link ActivityRunException} (that provides its own statuses).
     * Otherwise, consider #fromException(Exception).
     */
    public static ActivityRunResult fromException(
            OperationResultStatus opStatus, ActivityRunResultStatus runStatus, Throwable throwable) {
        return new ActivityRunResult(
                opStatus,
                runStatus,
                throwable,
                throwable instanceof AbortingInformationAware aia ? aia.getAbortingInformation() : null);
    }

    /**
     * Converts exception into {@link ActivityRunResult}. No logging or recording is done. For complex processing,
     * use {@link #handleException(Exception, OperationResult, AbstractActivityRun)}.
     */
    static ActivityRunResult fromException(Throwable throwable) {
        if (throwable instanceof ActivityRunException aee) {
            return fromException(aee.getOpResultStatus(), aee.getRunResultStatus(), aee.getCause());
        } else {
            return fromException(FATAL_ERROR, computeRunResultStatus(throwable), throwable);
        }
    }

    private static @NotNull ActivityRunResultStatus computeRunResultStatus(Throwable throwable) {
        if (throwable instanceof ActivityRunException activityRunException) {
            return activityRunException.getRunResultStatus(); // just in case, should not happen now
        } else if (throwable instanceof ThresholdPolicyViolationException) {
            return HALTING_ERROR;
        } else {
            // TODO In the future we should distinguish between permanent and temporary errors here.
            return PERMANENT_ERROR;
        }
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

    public static ActivityRunResult aborted(
            OperationResultStatus opResultStatus, ActivityAbortingInformationType abortingInformation) {
        return new ActivityRunResult(
                opResultStatus,
                ABORTED,
                null,
                abortingInformation);
    }

    public static ActivityRunResult waiting() {
        return new ActivityRunResult(OperationResultStatus.IN_PROGRESS, WAITING);
    }

    /**
     * Handles exceptions that occurred during run of an activity:
     *
     * . converts them into {@link ActivityRunResult}
     * . logs them appropriately
     * . records them into the provided {@link OperationResult}
     *
     * (Logging and recording are things we usually do when catching exceptions.)
     *
     * This method is to be called at places that expect {@link ActivityRunResult} to be returned, instead of throwing exceptions.
     *
     * @param e Exception to be handled
     * @param opResult Operation result into which the exception should be recorded
     * @param activityRun Instance of activity run in which the exception is converted.
     * It is used just for logging purposes.
     *
     * @see #fromException(OperationResultStatus, ActivityRunResultStatus, Throwable)
     */
    public static @NotNull ActivityRunResult handleException(
            @NotNull Exception e,
            @NotNull OperationResult opResult,
            @NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        ActivityRunResult runResult = fromException(e);
        logException(e, runResult.getOperationResultStatus(), activityRun.getDiagName());
        if (e instanceof ActivityRunException aee) {
            opResult.recordStatus(runResult.getOperationResultStatus(), aee.getFullMessage(), aee.getCause());
        } else {
            opResult.recordStatus(runResult.getOperationResultStatus(), e.getMessage(), e);
        }
        return runResult;
    }

    private static void logException(Exception e, OperationResultStatus status, String runDiagName) {
        if (status == WARNING) {
            LOGGER.warn("{}; in {}", e.getMessage(), runDiagName);
        } else if (status == HANDLED_ERROR) {
            // Should we even log handled errors like this?
            LOGGER.warn("Handled error in {}: {}", runDiagName, e.getMessage(), e);
        } else if (status != SUCCESS && status != NOT_APPLICABLE) {
            // What about other kinds of status (in progress? unknown? - they should not occur at this point)
            LoggingUtils.logUnexpectedException(LOGGER, "Exception in {}", e, runDiagName);
        }
    }
    //endregion

    //region Run result status
    public ActivityRunResultStatus getRunResultStatus() {
        return runResultStatus;
    }

    void setRunResultStatus(ActivityRunResultStatus runResultStatus) {
        this.runResultStatus = runResultStatus;
    }

    void setRunResultStatus(ActivityRunResultStatus runResultStatus, Throwable throwable) {
        this.runResultStatus = runResultStatus;
        this.throwable = throwable;
    }

    public boolean isError() {
        return runResultStatus == PERMANENT_ERROR
                || runResultStatus == TEMPORARY_ERROR
                || runResultStatus == HALTING_ERROR
                || runResultStatus == ABORTED;
    }

    public boolean isFinished() {
        return runResultStatus == FINISHED;
    }

    public boolean isWaiting() {
        return runResultStatus == WAITING;
    }

    public boolean isInterrupted() {
        return runResultStatus == INTERRUPTED;
    }

    public boolean isAborted() {
        return runResultStatus == ABORTED;
    }

    boolean isToBeSkipped() {
        return isAborted()
                && getAbortingInformationRequired().getPolicyAction() instanceof SkipActivityPolicyActionType;
    }

    boolean isToBeRestarted() {
        return isAborted()
                && getAbortingInformationRequired().getPolicyAction() instanceof RestartActivityPolicyActionType;
    }

    public ActivitySimplifiedRealizationStateType getSimplifiedRealizationState() {
        return isFinished() ? ActivitySimplifiedRealizationStateType.COMPLETE : ActivitySimplifiedRealizationStateType.IN_PROGRESS;
    }

    public boolean isRestartRequested() {
        return runResultStatus == RESTART_REQUESTED;
    }
    //endregion

    //region Operation result status
    public OperationResultStatus getOperationResultStatus() {
        return operationResultStatus;
    }

    void setOperationResultStatus(OperationResultStatus operationResultStatus) {
        this.operationResultStatus = operationResultStatus;
    }

    public OperationResultStatusType getOperationResultStatusBean() {
        return createStatusType(operationResultStatus);
    }
    //endregion

    //region Other fields
    public Throwable getThrowable() {
        return throwable;
    }

    public ActivityRunResult message(String message) {
        this.message = message;
        return this;
    }

    public @Nullable String getMessage() {
        return message;
    }

    @NotNull ActivityAbortingInformationType getAbortingInformationRequired() {
        return stateNonNull(abortingInformation, "Aborting information is missing in aborted activity run result");
    }

    void setAbortingInformation(@Nullable ActivityAbortingInformationType abortingInformation) {
        this.abortingInformation = abortingInformation;
    }

    /** Returns the path of the activity that should be ultimately skipped or restarted. Fail of none. */
    @NotNull ActivityPath getSkippedOrRestartedActivityPath() {
        var pathBean = stateNonNull(
                getAbortingInformationRequired().getActivityPath(),
                "No activity path in aborting information");
        return ActivityPath.fromBean(pathBean);
    }

    @Nullable RestartRequestingInformation getRestartRequestingInformation() {
        return restartRequestingInformation;
    }

    public @NotNull RestartRequestingInformation getRestartRequestingInformationRequired() {
        return stateNonNull(
                restartRequestingInformation,
                "No restart requesting information in restart-requested activity run result");
    }

    void setRestartRequestingInformation(@Nullable RestartRequestingInformation restartRequestingInformation) {
        this.restartRequestingInformation = restartRequestingInformation;
    }
    //endregion

    //region Misc
    @Override
    public String toString() {
        return "ActivityRunResult{" +
                "opStatus=" + operationResultStatus +
                ", runStatus=" + runResultStatus +
                (throwable != null ? ", throwable=" + throwable : "") +
                (message != null ? ", message=" + message : "") +
                ", abortingInformation=" + abortingInformation +
                ", restartRequestingInformation=" + restartRequestingInformation +
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

    /**
     * "Closes" the result by converting null or "in progress" values into finished/interrupted/success/default ones.
     */
    public void close(boolean canRun, OperationResultStatus status) {
        if (runResultStatus == null) {
            runResultStatus = canRun ? ActivityRunResultStatus.FINISHED : ActivityRunResultStatus.INTERRUPTED;
        }
        if (operationResultStatus == null) {
            operationResultStatus = status;
        }
        if (isFinished()
                && (operationResultStatus == null || operationResultStatus == OperationResultStatus.IN_PROGRESS)) {
            operationResultStatus = OperationResultStatus.SUCCESS;
        }
    }
    //endregion

    /** Details for {@link #runResultStatus} being {@link ActivityRunResultStatus#RESTART_REQUESTED}. */
    public record RestartRequestingInformation(
            @NotNull RestartActivityPolicyActionType restartAction,
            int executionAttempt) {
    }
}
