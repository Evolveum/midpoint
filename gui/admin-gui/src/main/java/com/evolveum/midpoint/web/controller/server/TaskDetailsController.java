package com.evolveum.midpoint.web.controller.server;

import java.io.Serializable;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.bean.TaskItem;

@Controller("taskDetails")
@Scope("session")
public class TaskDetailsController implements Serializable{

	private static final long serialVersionUID = -5990159771865483929L;
	public static final String PAGE_NAVIGATION = "/server/taskDetails?faces-redirect=true";
	
	
	public TaskDetailsController(){
		
	}
	
	private TaskItem task;

	public TaskItem getTask() {
		return task;
	}

	public void setTask(TaskItem task) {
		this.task = task;
	}
	
	
	
}
