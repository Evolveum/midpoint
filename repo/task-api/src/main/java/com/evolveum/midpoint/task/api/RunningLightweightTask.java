/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Running lightweight asynchronous task.
 */
public interface RunningLightweightTask extends RunningTask {

    /**
     * Returns the in-memory version of the parent task. Applicable only to lightweight subtasks.
     * EXPERIMENTAL (use with care)
     */
    @Experimental
    RunningTask getLightweightTaskParent();

    /**
     * Returns the task handler.
     */
    @NotNull LightweightTaskHandler getLightweightTaskHandler();

    /**
     * Request the start of the execution of this task's handler.
     * The handler is executed asynchronously.
     */
    void startLightweightHandler();

    /**
     * Was the task requested to start? (It may or may not actually started.)
     */
    boolean lightweightHandlerStartRequested();
}
