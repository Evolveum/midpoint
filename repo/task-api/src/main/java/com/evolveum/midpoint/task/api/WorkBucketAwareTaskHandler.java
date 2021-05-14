/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import org.jetbrains.annotations.NotNull;

/**
 * Task handler that supports bucketed tasks. Its `run` method provides bucket-related parameters
 * that allow the implementation to (typically) narrow the query according to the current bucket.
 * Also, the run result is enhanced with bucket-related properties.
 */
public interface WorkBucketAwareTaskHandler extends TaskHandler {

    @Override
    default TaskRunResult run(@NotNull RunningTask task) {
        throw new UnsupportedOperationException("run with no work bucket is not supported here");
    }

    TaskWorkBucketProcessingResult run(RunningTask task, WorkBucketType workBucket,
            ActivityDefinitionType partitionDefinition, TaskWorkBucketProcessingResult previousRunResult);

    default TaskWorkBucketProcessingResult onNoMoreBuckets(RunningTask task,
            TaskWorkBucketProcessingResult previousRunResult, OperationResult result) {
        return previousRunResult;
    }
}
