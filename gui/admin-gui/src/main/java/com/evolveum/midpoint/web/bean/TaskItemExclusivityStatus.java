package com.evolveum.midpoint.web.bean;

import java.util.ArrayList;
import java.util.List;

public enum TaskItemExclusivityStatus {
	
	CLAIMED("Claimed"),
	
	RELEASED("Released");
	
	private String title;
	
	private TaskItemExclusivityStatus(String title){
		this.title=title;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
