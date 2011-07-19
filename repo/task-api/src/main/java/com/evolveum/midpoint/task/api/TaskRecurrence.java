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

import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskRecurrenceType;

/**
 * TODO
 * 
 * @author Radovan Semancik
 *
 */
public enum TaskRecurrence {
	/**
	 * TODO
	 * The task is executed only once, at the first moment that the scedule specifies. If that moment is in
     * the past, the task will be executed as soon as any execution environment (node) is available.
	 * Once the task is finished, it will not be executed again.
	 */
	SINGLE,

	/**
	 * TODO
	 * The task is executed as many times as the schedule specifies.
	 */
	RECURRING;

	public static TaskRecurrence fromTaskType(TaskRecurrenceType recurrenceType) {
			if (recurrenceType == null) {
				return null;
			}
			if (recurrenceType == TaskRecurrenceType.SINGLE) {
				return SINGLE;
			}
			if (recurrenceType == TaskRecurrenceType.RECURRING) {
				return RECURRING;
			}
			throw new IllegalArgumentException("Unknown recurrence type "+recurrenceType);
	}

	public TaskRecurrenceType toTaskType() {
		if (this==RECURRING) {
			return TaskRecurrenceType.RECURRING;
		}
		if (this==SINGLE) {
			return TaskRecurrenceType.SINGLE;
		}
		throw new IllegalArgumentException("Unknown recurrence type "+this);
	}

}
