/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

/**
 * EXPERIMENTAL
 *
 * @author mederly
 */
public class TaskWorkBucketProcessingResult extends TaskRunResult {

    private boolean bucketComplete;         // "bucket not complete" implies "should not continue"
    private boolean shouldContinue;

    public boolean isBucketComplete() {
        return bucketComplete;
    }

    public void setBucketComplete(boolean bucketComplete) {
        this.bucketComplete = bucketComplete;
    }

    public boolean isShouldContinue() {
        return shouldContinue;
    }

    public void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    @Override
    public String toString() {
        return "TaskWorkBucketProcessingResult{" +
                "bucketComplete=" + bucketComplete +
                ", shouldContinue=" + shouldContinue +
                ", progress=" + progress +
                ", runResultStatus=" + runResultStatus +
                ", operationResult=" + operationResult +
                '}';
    }
}
