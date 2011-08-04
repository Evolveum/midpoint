package com.evolveum.midpoint.web.controller.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.task.api.TaskManager;


@Controller("taskAdd")
@Scope("session")
public class TaskAddController {

	public static final String PAGE_NAVIGATION = "/server/taskAdd?faces-redirect=true";
	@Autowired(required = true)
	private transient TaskManager taskManager;
	
	
	
	public String addTask(){
		
		
		
		return TaskListController.PAGE_NAVIGATION;
	}
}
