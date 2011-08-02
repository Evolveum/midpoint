package com.evolveum.midpoint.web.controller.server;

import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.Task;


@Controller("taskList")
@Scope("session")
public class TaskListController {

	@Autowired(required = true)
	private transient TaskManager taskManager;

	private Set<Task> runningTasks;
	private boolean activated;

	public static final String PAGE_NAVIGATION = "/server/index?faces-redirect=true";

	
	public TaskListController() {
		super();
	}

	public String listRunningTasks() {
		runningTasks = taskManager.getRunningTasks();
		return PAGE_NAVIGATION;
	}

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public void setTaskManager(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

	public Set<Task> getRunningTasks() {
		return runningTasks;
	}

	public void setRunningTasks(Set<Task> runningTasks) {
		this.runningTasks = runningTasks;
	}

	public void deactivate() {
		taskManager.deactivateServiceThreads();
		setActivated(isActivated());
	}

	public void reactivate() {
		taskManager.reactivateServiceThreads();
		setActivated(isActivated());
	}

	public boolean isActivated() {
		return taskManager.getServiceThreadsActivationState();
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

}
