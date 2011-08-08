package com.evolveum.midpoint.web.bean;

public enum TaskItemRecurrenceStatus {
	
	SINGLE("Single"),
	
	RECURRING("Recurring");

	private String title;
	
	private TaskItemRecurrenceStatus(String title){
		this.title = title;
	}
}
