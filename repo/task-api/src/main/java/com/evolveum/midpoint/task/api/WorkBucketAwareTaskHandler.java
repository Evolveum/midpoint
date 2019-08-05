/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import static com.evolveum.midpoint.prism.PrismProperty.getRealValue;

/**
 * @author mederly
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


	TaskWorkBucketProcessingResult run(RunningTask task, WorkBucketType workBucket, TaskPartitionDefinitionType partitionDefinition, TaskWorkBucketProcessingResult previousRunResult);
	
	default TaskWorkBucketProcessingResult onNoMoreBuckets(Task task, TaskWorkBucketProcessingResult previousRunResult) {
		return previousRunResult;
	}

	default QueryType getObjectQueryTypeFromTaskExtension(Task task) {
		return getRealValue(task.getExtensionPropertyOrClone(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY));
	}

}
