/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import java.util.Collection;

import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskUnpauseActionType;

import org.jetbrains.annotations.NotNull;

import org.jetbrains.annotations.Nullable;

/**
 *  A task that is directly used to execute the handler code.
 *
 *  It is a very sensitive structure. First of all, it must be thread-safe because it is used for the handler code execution
 *  and at the same time accessed by clients that need to check its state. There are two such situations:
 *  (1) Lightweight Asynchronous Tasks because they have no persistent representation. The only instance that exists
 *      is the one that is being executed by a handler.
 *  (2) When a client asks the task manager for a current state of the task (typically being interested in operational stats).
 *      The information could be fetched from the repository but it would be a bit outdated. This situation can be avoided
 *      by retrieving information always from the repository, sacrificing information timeliness a bit. But the (1) cannot.
 *
 *  Some information related to task execution (e.g. list of lightweight asynchronous tasks, information on task thread, etc)
 *  is relevant only for running tasks. Therefore they are moved here.
 */
public interface RunningTask extends Task, RunningTaskStatisticsCollector, CanRunSupplier {

    /**
     * Creates a transient subtask, ready to execute a given LightweightTaskHandler.
     *
     * Owner is inherited from parent task to subtask.
     */
    @NotNull RunningLightweightTask createSubtask(@NotNull LightweightTaskHandler handler);

    Collection<? extends RunningLightweightTask> getLightweightAsynchronousSubtasks();

    Collection<? extends RunningLightweightTask> getRunningLightweightAsynchronousSubtasks();

    Collection<? extends RunningLightweightTask> getRunnableOrRunningLightweightAsynchronousSubtasks();

    /**
     * Precondition: there are no runnable nor running LATs
     */
    void deleteLightweightAsynchronousSubtasks();

    /**
     * TODO
     */
    @Experimental
    @NotNull String getRootTaskOid();

    /** TODO EXPERIMENTAL */
    @Experimental
    @NotNull Task getRootTask();

    @Experimental
    @Nullable Task getParentTask();

    /**
     * Changes scheduling status to WAITING. Does not change execution state.
     * Currently use only on transient tasks OR from within task handler.
     */
    void makeWaitingForOtherTasks(TaskUnpauseActionType unpauseAction);

    /**
     * Changes scheduling status to WAITING, and execution state to the given value.
     * Currently use only on transient tasks OR from within task handler.
     */
    void makeWaitingForOtherTasks(TaskExecutionStateType execState, TaskUnpauseActionType unpauseAction);

    /**
     * Returns the execution mode (e.g. execute, simulate, dry run) for the current operation.
     * This is a little hack to avoid the need of passing this information throughout the whole call tree.
     * (Originally this was implemented in task extension.)
     */
    @Experimental
    default @NotNull ExecutionModeType getActivityExecutionMode() {
        ExecutionSupport executionSupport = getExecutionSupport();
        return executionSupport != null ? executionSupport.getActivityExecutionMode() : ExecutionModeType.FULL;
    }

    ExecutionSupport getExecutionSupport();

    void setExecutionSupport(ExecutionSupport executionContext);

    /**
     * @return True if this task should not be checked for staleness.
     */
    boolean isExcludedFromStalenessChecking();

    /**
     * Sets the "excluded from staleness checking" flag.
     */
    void setExcludedFromStalenessChecking(boolean value);
}
