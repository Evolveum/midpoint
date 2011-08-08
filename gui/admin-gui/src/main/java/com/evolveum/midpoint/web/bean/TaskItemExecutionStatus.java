package com.evolveum.midpoint.web.bean;

import com.evolveum.midpoint.task.api.TaskExecutionStatus;

public enum TaskItemExecutionStatus {

	RUNNING("runn.png", "Running"),

	WAITING("hourglass.png", "Waiting"),

	CLOSED("stop.png", "Closed");

	private String icon;
	private String title;

	private TaskItemExecutionStatus(String icon, String title) {
		this.icon = icon;
		this.title = title;
	}

	public String getIcon() {
		return icon;
	}

	public String getTitle() {
		return title;
	}

	public static TaskItemExecutionStatus fromTask(TaskExecutionStatus executionStatus) {
		if (executionStatus.equals(TaskExecutionStatus.CLOSED)) {
			return CLOSED;
		}
		if (executionStatus.equals(TaskExecutionStatus.RUNNING)) {
			return RUNNING;
		}
		if (executionStatus.equals(TaskExecutionStatus.WAITING)) {
			return WAITING;
		}

		return null;

	}
}
