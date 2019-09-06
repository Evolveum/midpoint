/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
     * The task is waiting because of other reason.
	 */
	OTHER;


	public static TaskWaitingReason fromTaskType(TaskWaitingReasonType xmlValue) {

		if (xmlValue == null) {
			return null;
		}
        switch (xmlValue) {
            case OTHER_TASKS: return OTHER_TASKS;
            case OTHER: return OTHER;
            default: throw new IllegalArgumentException("Unknown waiting reason type " + xmlValue);
        }
	}

	public TaskWaitingReasonType toTaskType() {

        switch (this) {
            case OTHER_TASKS: return TaskWaitingReasonType.OTHER_TASKS;
            case OTHER: return TaskWaitingReasonType.OTHER;
            default: throw new IllegalArgumentException("Unknown execution status type "+this);
        }
	}
}
