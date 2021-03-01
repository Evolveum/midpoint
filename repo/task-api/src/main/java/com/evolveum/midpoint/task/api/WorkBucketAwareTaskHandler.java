/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

/**
 * Task handler that supports bucketed tasks. Its `run` method provides bucket-related parameters
 * that allow the implementation to (typically) narrow the query according to the current bucket.
 * Also, the run result is enhanced with bucket-related properties.
 */
public interface WorkBucketAwareTaskHandler extends TaskHandler {

    @Override
    default TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partitionDefinition) {
        throw new UnsupportedOperationException("run with no work bucket is not supported here");
    }

    TaskWorkBucketProcessingResult run(RunningTask task, WorkBucketType workBucket,
            TaskPartitionDefinitionType partitionDefinition, TaskWorkBucketProcessingResult previousRunResult);

    default TaskWorkBucketProcessingResult onNoMoreBuckets(Task task, TaskWorkBucketProcessingResult previousRunResult) {
        return previousRunResult;
    }
}
