/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.schema.result.OperationResultStatus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;

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
     * Error that prevents activity from running, if the task has more activities, it should continue with the next one.
     * If the task does not have any more activities, it should finish.
     */
    SKIP_ACTIVITY_ERROR(TaskRunResultStatus.HALTING_ERROR), // FIXME rethink to mapping to TRRS

    /**
     * Error that prevents the activity from running.
     * The current activity should be restarted.
     * Limits might be in place to limit the number of restarts and to decide what to do if the limit is exceeded.
     */
    RESTART_ACTIVITY_ERROR(TaskRunResultStatus.RESTART_REQUESTED),

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

    @NotNull private final TaskRunResultStatus taskRunResultStatus;

    ActivityRunResultStatus(@NotNull TaskRunResultStatus taskRunResultStatus) {
        this.taskRunResultStatus = taskRunResultStatus;
    }

    public @NotNull TaskRunResultStatus toTaskRunResultStatus() {
        return taskRunResultStatus;
    }
}
