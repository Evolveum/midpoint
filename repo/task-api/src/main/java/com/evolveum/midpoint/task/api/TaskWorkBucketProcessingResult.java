/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * EXPERIMENTAL
 */
@Experimental
public class TaskWorkBucketProcessingResult extends TaskRunResult {

    /**
     * Can the task manager mark the bucket as complete?
     *
     * Notes:
     *
     * 1. Currently this is set to true iff {@link RunningTask#canRun()} is true. But these two may diverge in the future.
     * 2. The default reaction on getting false by the executor is to stop the execution.
     *
     * TODO Consider if we should remove this field.
     */
    private boolean bucketComplete;

    public boolean isBucketComplete() {
        return bucketComplete;
    }

    public void setBucketComplete(boolean bucketComplete) {
        this.bucketComplete = bucketComplete;
    }

    @Override
    public String toString() {
        return "TaskWorkBucketProcessingResult{" +
                "bucketComplete=" + bucketComplete +
                ", progress=" + progress +
                ", runResultStatus=" + runResultStatus +
                ", operationResult=" + operationResult +
                '}';
    }
}
