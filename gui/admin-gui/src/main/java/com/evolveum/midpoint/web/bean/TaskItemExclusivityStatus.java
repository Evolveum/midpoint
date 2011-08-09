package com.evolveum.midpoint.web.bean;

import com.evolveum.midpoint.task.api.TaskExclusivityStatus;

public enum TaskItemExclusivityStatus {

	CLAIMED("Claimed"),

	RELEASED("Released");

	private String title;

	private TaskItemExclusivityStatus(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public static TaskItemExclusivityStatus fromTask(
			TaskExclusivityStatus exclusivityStatus) {
		if (exclusivityStatus.equals(TaskExclusivityStatus.CLAIMED)) {
			return CLAIMED;
		} 
		if (exclusivityStatus.equals(TaskExclusivityStatus.RELEASED)){
			return RELEASED;
		}
		return null;
	}
	
	public static TaskExclusivityStatus toTask(
			TaskItemExclusivityStatus exclusivityStatus) {
		if (exclusivityStatus.equals(TaskItemExclusivityStatus.CLAIMED)) {
			return TaskExclusivityStatus.CLAIMED;
		} 
		if (exclusivityStatus.equals(TaskItemExclusivityStatus.RELEASED)){
			return TaskExclusivityStatus.RELEASED;
		}
		return null;
	}
	

}
