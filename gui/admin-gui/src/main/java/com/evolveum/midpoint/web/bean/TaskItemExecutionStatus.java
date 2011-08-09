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
	
	public static TaskExecutionStatus toTask(TaskItemExecutionStatus executionStatus) {
		if (executionStatus.equals(TaskItemExecutionStatus.CLOSED)) {
			return TaskExecutionStatus.CLOSED;
		}
		if (executionStatus.equals(TaskItemExecutionStatus.RUNNING)) {
			return TaskExecutionStatus.RUNNING;
		}
		if (executionStatus.equals(TaskItemExecutionStatus.WAITING)) {
			return TaskExecutionStatus.WAITING;
		}

		return null;

	}
}
