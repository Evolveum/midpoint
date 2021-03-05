/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.statistics.StructuredProgressCollector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingRootType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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
public interface RunningTask extends Task, StructuredProgressCollector {

    /**
     * Returns true if the task can run (was not interrupted).
     *
     * Will return false e.g. if shutdown was signaled.
     *
     * BEWARE: this flag is present only on the instance of the task that is being "executed", i.e. passed to
     * task execution routine and task handler(s).
     *
     * @return true if the task can run
     */
    boolean canRun();

    /**
     * Creates a transient subtask, ready to execute a given LightweightTaskHandler.
     *
     * Owner is inherited from parent task to subtask.
     */
    RunningTask createSubtask(LightweightTaskHandler handler);

    /**
     * Returns the in-memory version of the parent task. Applicable only to lightweight subtasks.
     * EXPERIMENTAL (use with care)
     */
    RunningTask getParentForLightweightAsynchronousTask();

    LightweightTaskHandler getLightweightTaskHandler();

    boolean isLightweightAsynchronousTask();

    Collection<? extends RunningTask> getLightweightAsynchronousSubtasks();

    Collection<? extends RunningTask> getRunningLightweightAsynchronousSubtasks();

    Collection<? extends RunningTask> getRunnableOrRunningLightweightAsynchronousSubtasks();

    boolean lightweightHandlerStartRequested();

    /**
     * Starts execution of a transient task carrying a LightweightTaskHandler.
     * (just a shortcut to analogous call in TaskManager)
     */
    void startLightweightHandler();

    void startCollectingOperationStats(@NotNull StatisticsCollectionStrategy strategy, boolean initialExecution);

    /**
     * Stores operation stats to prism object and to pending modifications.
     * Should be accompanied by flushing that modifications.
     *
     * TODO better name
     */
    void storeOperationStatsDeferred();

    /**
     * Call from the thread that executes the task ONLY! Otherwise wrong data might be recorded.
     */
    void refreshLowLevelStatistics();

    /**
     * Stores operation stats and progress from current in-memory state to the repository.
     * This is quite a hack: it accepts the fact that task in memory differs from the task in repository
     * (in these aspects). The synchronization occurs only from time to time, in order to avoid
     * wasting of the resources.
     *
     * CALL ONLY FROM THE THREAD EXECUTING THE TASK!
     */
    void storeOperationStatsAndProgress();

    // CALL ONLY FROM THE THREAD EXECUTING THE TASK!
    // stores operation statistics if the time has come
    void storeOperationStatsAndProgressIfNeeded();

    Long getLastOperationStatsUpdateTimestamp();

    void setOperationStatsUpdateInterval(long interval);

    long getOperationStatsUpdateInterval();

    /**
     * Increments the progress. Stores the stat to repo if the time interval came.
     *
     * Beware: ignores structured progress.
     *
     * CALL ONLY FROM THE THREAD EXECUTING THE TASK!
     */
    void incrementProgressAndStoreStatsIfNeeded();

    void deleteLightweightAsynchronousSubtasks();

    // EXPERIMENTAL; consider moving to AbstractSearchIterativeResultHandler
    int getAndIncrementObjectsSeen();

    /**
     * Must be called from the thread that executes the task.
     * EXPERIMENTAL; consider moving to AbstractSearchIterativeResultHandler
     */
    void startDynamicProfilingIfNeeded(RunningTask coordinatorTask, int objectsSeen);

    /**
     * Must be called from the thread that executes the task.
     */
    void stopDynamicProfiling();

    /**
     * EXPERIMENTAL
     */
    boolean requestTracingIfNeeded(RunningTask coordinatorTask, int objectsSeen, TracingRootType defaultTracingRoot);

    /**
     * EXPERIMENTAL
     */
    void stopTracing();

    /**
     * TODO
     * EXPERIMENTAL
     */
    @NotNull String getRootTaskOid();
}
