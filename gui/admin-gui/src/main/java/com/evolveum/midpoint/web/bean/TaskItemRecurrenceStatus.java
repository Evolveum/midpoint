package com.evolveum.midpoint.web.bean;

import com.evolveum.midpoint.task.api.TaskRecurrence;

public enum TaskItemRecurrenceStatus {
	
	SINGLE("Single"),
	
	RECURRING("Recurring");

	private String title;
	
	private TaskItemRecurrenceStatus(String title){
		this.title = title;
	}

	public String getTitle() {
		return title;
	}
	
	public static TaskItemRecurrenceStatus fromTask(TaskRecurrence recurrence){
		if (recurrence.equals(TaskRecurrence.SINGLE)){
			return SINGLE;
		}
		if (recurrence.equals(TaskRecurrence.RECURRING)){
			return RECURRING;
		}
		return null;
	}
	
}
