package com.evolveum.midpoint.web.bean;

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
}
