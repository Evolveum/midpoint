/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskWaitingReasonType;

/**
 * Task waiting reason.
 *
 * @author mederly
 *
 */
public enum TaskWaitingReason {
	/**
	 * The task is waiting for other (dependent) tasks - either its subtasks, or tasks explicitly marked
     * as "prerequisites" for this task (via dependentTask property)
	 */
	OTHER_TASKS,

	/**
	 * The task is waiting for a workflow process (that it monitors/shadows) to be finished.
	 */
	WORKFLOW,

	/**
     * The task is waiting because of other reason.
	 */
	OTHER;


	public static TaskWaitingReason fromTaskType(TaskWaitingReasonType xmlValue) {

		if (xmlValue == null) {
			return null;
		}
        switch (xmlValue) {
            case OTHER_TASKS: return OTHER_TASKS;
            case WORKFLOW: return WORKFLOW;
            case OTHER: return OTHER;
            default: throw new IllegalArgumentException("Unknown waiting reason type " + xmlValue);
        }
	}

	public TaskWaitingReasonType toTaskType() {

        switch (this) {
            case OTHER_TASKS: return TaskWaitingReasonType.OTHER_TASKS;
            case WORKFLOW: return TaskWaitingReasonType.WORKFLOW;
            case OTHER: return TaskWaitingReasonType.OTHER;
            default: throw new IllegalArgumentException("Unknown execution status type "+this);
        }
	}
}
