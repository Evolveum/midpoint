package com.evolveum.midpoint.web.controller.server;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.util.FacesUtils;

@Controller("taskDetails")
@Scope("session")
public class TaskDetailsController implements Serializable {

	private static final long serialVersionUID = -5990159771865483929L;
	public static final String PAGE_NAVIGATION = "/server/taskDetails?faces-redirect=true";

	@Autowired(required = true)
	private transient TaskManager taskManager;

	public TaskDetailsController() {

	}

	private TaskItem task;

	public TaskItem getTask() {
		return task;
	}

	public void setTask(TaskItem task) {
		this.task = task;
	}

	

	public void createInstance() {

		 taskManager.createTaskInstance(task.toTaskType());
		 FacesUtils.addSuccessMessage("Task instance created sucessfully");
	}


	public TaskManager getTaskManager() {
		return taskManager;
	}

	public void setTaskManager(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

}
