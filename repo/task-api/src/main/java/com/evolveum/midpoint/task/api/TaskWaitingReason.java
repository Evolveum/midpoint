/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.TaskWaitingReasonType;

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
