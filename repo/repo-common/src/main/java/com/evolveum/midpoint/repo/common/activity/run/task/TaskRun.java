/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * Represents a run of a task.
 *
 * Originally intended to cover multitude of tasks.
 * However, currently there's only one implementation (for activity-based tasks).
 * Therefore, *TODO* consider removing this interface.
 */
public interface TaskRun extends ShortDumpable {

    /**
     * Passes control to the run object.
     *
     * The object is now responsible for the whole execution of this task.
     */
    @NotNull TaskRunResult run(OperationResult result) throws TaskException;

    /**
     * Returns the task associated with this run.
     */
    @NotNull RunningTask getRunningTask();

    @NotNull CommonTaskBeans getBeans();

    default Long heartbeat() {
        return null;
    }

    default void shortDump(StringBuilder sb) {
        sb.append("run of ").append(getRunningTask());
    }

    default Task getRootTask() {
        return getRunningTask().getRootTask();
    }

    default boolean canRun() {
        return getRunningTask().canRun();
    }

    default boolean isRootTask() {
        return getRunningTask().isRoot();
    }
}
