/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResultStatus;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.FINISHED;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.TEMPORARY_ERROR;

/**
 * Single-purpose class to return task run results.
 *
 * More than one value is returned, therefore it is
 * bundled into a class.
 *
 * @author Radovan Semancik
 *
 */
public class TaskRunResult implements Serializable {

    public enum TaskRunResultStatus {

        /**
         * The task run has finished.
         *
         * This does not necessarily mean that the task itself is going to be closed.
         * While single-run tasks are, recurring ones will run again later.
         */
        FINISHED,

        /**
         * The run has failed.
         *
         * The error is permanent. Unless the administrator does something to recover from the situation, there is no point in
         * re-trying the run. The usual case of this error is task misconfiguration.
         */
        PERMANENT_ERROR,

        /**
         * Temporary failure during the run.
         *
         * The error is temporary. The situation may change later when the conditions will be more favorable.
         * It makes sense to retry the run. Usual cases of this error are network timeouts.
         *
         * For single-run tasks we suspend them on such occasions. So the administrator can resume them after
         * correcting the problem.
         */
        TEMPORARY_ERROR,

        /**
         * Like {@link #PERMANENT_ERROR}, but in addition, task and its dependent tasks (parent, grand-parent, ..., plus
         * explicit dependents) are going to be suspended. In fact, this halts the whole task tree.
         *
         * Currently used when handling policy actions that suspend tasks.
         */
        HALTING_ERROR,

        /**
         * Like {@link #HALTING_ERROR}, but in addition, the whole task tree should be restarted.
         * The {@link TaskRunResult#taskRestartInstruction} can provide additional instructions, like the delay before restart.
         */
        RESTART_REQUESTED,

        /**
         * Task run hasn't finished, but nevertheless it must end (for now). An example of such a situation is
         * when a task execution is requested to stop, e.g., when suspending the task or shutting down the node.
         */
        INTERRUPTED,

        /**
         * Task run hasn't finished, but it should go to WAITING state. Typical example is when a task delegated
         * the work to its subtasks and now it is waiting for their completion.
         */
        WAITING
    }

    /**
     * Progress to be recorded in the task. Null means "do not update, take whatever is in the task".
     */
    protected Long progress;

    /**
     * Final status of the run. It drives what should be done next. (E.g. repeat the run in the case of temporary
     * errors and recurring tasks.)
     */
    protected TaskRunResultStatus runResultStatus;

    /**
     * Status to be reported to the user.
     */
    protected OperationResultStatus operationResultStatus;

    /**
     * An exception that has occurred and that is going to be recorded at the root of the operation result.
     * This is the "main" exception that caused the task run to be stopped. (It should be recorded somewhere
     * in the operation result as well, if possible. But here it is designated as _the_ cause of the run being stopped.)
     *
     * If null, we will not overwrite the value that is computed for the result.
     */
    protected Throwable throwable;

    /**
     * Message that should be recorded in the root operation result. It has the same meaning as {@link #throwable}.
     * (Including the fact that if it's null, it won't overwrite whatever is in the result.)
     */
    protected String message;

    /**
     * Instruction how to restart the task (applicable only with {@link #runResultStatus}
     * of {@link TaskRunResultStatus#RESTART_REQUESTED}).
     */
    private TaskRestartInstruction taskRestartInstruction;

    public TaskRunResult() {
    }

    public TaskRunResult(TaskRunResultStatus runResultStatus, OperationResultStatus operationResultStatus) {
        this.runResultStatus = runResultStatus;
        this.operationResultStatus = operationResultStatus;
    }

    public static TaskRunResult of(TaskRunResultStatus taskRunResultStatus, OperationResultStatus operationResultStatus) {
        return new TaskRunResult(taskRunResultStatus, operationResultStatus);
    }

    public static TaskRunResult finished(OperationResultStatus operationResultStatus) {
        return new TaskRunResult(FINISHED, operationResultStatus);
    }

    public static TaskRunResult temporaryError(OperationResultStatus operationResultStatus) {
        return new TaskRunResult(TEMPORARY_ERROR, operationResultStatus);
    }

    public static TaskRunResult permanentFatalError() {
        return new TaskRunResult(TaskRunResultStatus.PERMANENT_ERROR, OperationResultStatus.FATAL_ERROR);
    }

    /**
     * @return the progress
     */
    public Long getProgress() {
        return progress;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(Long progress) {
        this.progress = progress;
    }

    /**
     * @return the status
     */
    public TaskRunResultStatus getRunResultStatus() {
        return runResultStatus;
    }

    /**
     * @param status the status to set
     */
    public void setRunResultStatus(TaskRunResultStatus status) {
        this.runResultStatus = status;
    }

    public OperationResultStatus getOperationResultStatus() {
        return operationResultStatus;
    }

    public void setOperationResultStatus(OperationResultStatus operationResultStatus) {
        this.operationResultStatus = operationResultStatus;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRestartAfter(long delayMillis) {
        this.runResultStatus = TaskRunResultStatus.RESTART_REQUESTED;
        this.taskRestartInstruction = new TaskRestartInstruction(delayMillis);
    }

    public TaskRestartInstruction getTaskRestartInstruction() {
        return taskRestartInstruction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TaskRunResult that = (TaskRunResult) o;
        return Objects.equals(progress, that.progress) &&
                runResultStatus == that.runResultStatus &&
                operationResultStatus == that.operationResultStatus &&
                Objects.equals(throwable, that.throwable) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(progress, runResultStatus, operationResultStatus, throwable, message);
    }

    @Override
    public String toString() {
        return "TaskRunResult("
                + "progress=" + progress
                + ", status=" + runResultStatus
                + (taskRestartInstruction != null ? ", restartInstruction=" + taskRestartInstruction : "")
                + ", result status=" + operationResultStatus
                + ")";
    }

    @NotNull public static TaskRunResult createFailureTaskRunResult(String message, Throwable t) {
        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
        runResult.setOperationResultStatus(OperationResultStatus.FATAL_ERROR);
        runResult.setThrowable(t);
        runResult.setMessage(message);
        return runResult;
    }

    @NotNull public static TaskRunResult createFromTaskException(TaskException e) {
        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(e.getRunResultStatus());
        runResult.setOperationResultStatus(e.getOpResultStatus());
        runResult.setThrowable(e.getCause());
        runResult.setMessage(e.getFullMessage());
        return runResult;
    }

    public static TaskRunResult createNotApplicableTaskRunResult() {
        TaskRunResult runResult = new TaskRunResult();
        runResult.setRunResultStatus(FINISHED);
        runResult.setOperationResultStatus(OperationResultStatus.NOT_APPLICABLE);
        return runResult;
    }

    public record TaskRestartInstruction(long delayMillis) implements Serializable {

        @Serial private static final long serialVersionUID = 1L;

    }
}
