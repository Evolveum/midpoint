/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import static com.evolveum.midpoint.prism.PrismProperty.getRealValue;

/**
 * TODO
 */
public interface WorkBucketAwareTaskHandler extends TaskHandler {

    @Override
    default TaskRunResult run(RunningTask task) {
        throw new UnsupportedOperationException("run with no work bucket is not supported here");
    }

    @Override
    default TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partitionDefinition) {
        throw new UnsupportedOperationException("run with no work bucket is not supported here");
    }

    TaskWorkBucketProcessingResult run(RunningTask task, WorkBucketType workBucket,
            TaskPartitionDefinitionType partitionDefinition, TaskWorkBucketProcessingResult previousRunResult);

    default TaskWorkBucketProcessingResult onNoMoreBuckets(Task task, TaskWorkBucketProcessingResult previousRunResult) {
        return previousRunResult;
    }

    default QueryType getObjectQueryTypeFromTaskExtension(Task task) {
        return getRealValue(task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY));
    }

}
