/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.schema.result.OperationResultStatus;

import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;

import org.jetbrains.annotations.Nullable;

/**
 * Result status of an activity run. Besides providing the success or failure status (with severity), it also indicates
 * the nature of the failure (if any) - whether it is permanent or temporary, whether the activity should be restarted, etc.
 *
 * Derived from {@link TaskRunResultStatus}, but separated to make the distinction between task and activity clearer.
 * Many activity result statuses directly map to task result statuses, but some are activity-specific.
 *
 * NOTE: We are still not entirely sure about the set of statuses and their semantics. Even the name of this class
 * is not ideal, because it collides with {@link OperationResultStatus}. So this is subject to change.
 */
public enum ActivityRunResultStatus {

    /**
     * Activity is not meant to run again, and no special action from neither activity framework nor task engine is needed.
     * The activity itself may be completed or aborted.
     *
     * @see TaskRunResultStatus#FINISHED
     */
    FINISHED(TaskRunResultStatus.FINISHED),

    /**
     * @see TaskRunResultStatus#PERMANENT_ERROR
     */
    PERMANENT_ERROR(TaskRunResultStatus.PERMANENT_ERROR),

    /**
     * @see TaskRunResultStatus#TEMPORARY_ERROR
     */
    TEMPORARY_ERROR(TaskRunResultStatus.TEMPORARY_ERROR),

    /**
     * @see TaskRunResultStatus#HALTING_ERROR
     */
    HALTING_ERROR(TaskRunResultStatus.HALTING_ERROR),

    /**
     * Situation (typically an error) that makes activity abort, i.e., definitely stop its execution, without
     * an option of continuing. The activity can be either skipped or restarted (later).
     *
     * There's no mapping to {@link TaskRunResultStatus}, because there's nontrivial logic deciding between
     * {@link TaskRunResultStatus#FINISHED} and {@link TaskRunResultStatus#RESTART_REQUESTED}.
     */
    ABORTED(null),

    /**
     * When an activity has to be restarted (because of a policy rule), it is marked as aborted with a pending restart,
     * and then the enclosing task run result is set to {@link TaskRunResultStatus#RESTART_REQUESTED}. However, it there
     * are intermediate activities that go to the task boundary, they must be stepped out first. This status is used to do that.
     */
    RESTART_REQUESTED(TaskRunResultStatus.RESTART_REQUESTED),

    /**
     * The activity run cannot continue now, because the enclosing task was interrupted (e.g. by suspending it or
     * by shutting down the system).
     *
     * @see TaskRunResultStatus#INTERRUPTED
     */
    INTERRUPTED(TaskRunResultStatus.INTERRUPTED),

    /**
     * The activity run cannot continue now, but it is expected to continue later - after dependent task(s) finish.
     * The enclosing task should go into WAITING state.
     *
     * Used mainly for delegated and distributed activities, where we need to the subtask(s) to finish first.
     *
     * @see TaskRunResultStatus#WAITING
     */
    WAITING(TaskRunResultStatus.WAITING);

    /** Some statuses may not have a direct mapping to {@link TaskRunResultStatus}; in that case, this is null. */
    @Nullable private final TaskRunResultStatus taskRunResultStatus;

    ActivityRunResultStatus(@Nullable TaskRunResultStatus taskRunResultStatus) {
        this.taskRunResultStatus = taskRunResultStatus;
    }

    public @Nullable TaskRunResultStatus toTaskRunResultStatus() {
        return taskRunResultStatus;
    }
}
