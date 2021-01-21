/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Used to signal that we have to exit handler processing with a given run result.
 *
 * Necessary for creation of separate methods for parts of task handler that need to return something
 * but also exit handler immediately if there's any issue.
 *
 * Experimental. Maybe we can remove it.
 *
 * @author mederly
 */
@Experimental
public class ExitWorkBucketHandlerException extends Exception {

    @NotNull private final TaskWorkBucketProcessingResult runResult;

    public ExitWorkBucketHandlerException(@NotNull TaskWorkBucketProcessingResult runResult) {
        this.runResult = runResult;
    }

    @NotNull
    public TaskWorkBucketProcessingResult getRunResult() {
        return runResult;
    }
}
