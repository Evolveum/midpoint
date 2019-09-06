/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskRecurrenceType;

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
