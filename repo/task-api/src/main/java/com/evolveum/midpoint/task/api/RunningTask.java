/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import java.util.Collection;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TracingRootType;

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
public interface RunningTask extends Task, RunningTaskStatisticsCollector {

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
    @NotNull RunningLightweightTask createSubtask(@NotNull LightweightTaskHandler handler);

    Collection<? extends RunningLightweightTask> getLightweightAsynchronousSubtasks();

    Collection<? extends RunningLightweightTask> getRunningLightweightAsynchronousSubtasks();

    Collection<? extends RunningLightweightTask> getRunnableOrRunningLightweightAsynchronousSubtasks();

    /**
     * Precondition: there are no runnable nor running LATs
     */
    void deleteLightweightAsynchronousSubtasks();

    // EXPERIMENTAL; consider moving to AbstractSearchIterativeResultHandler
    @Experimental
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
