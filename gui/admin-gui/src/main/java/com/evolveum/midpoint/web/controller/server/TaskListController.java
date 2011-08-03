package com.evolveum.midpoint.web.controller.server;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.controller.resource.ResourceDetailsController;
import com.evolveum.midpoint.web.controller.util.ListController;
import com.evolveum.midpoint.web.util.FacesUtils;


@Controller("taskList")
@Scope("session")
public class TaskListController extends ListController<TaskItem>{

	@Autowired(required = true)
	private transient TaskManager taskManager;
	@Autowired(required = true)
	private transient TaskDetailsController taskDetails;

//	private Set<TaskItem> runningTasks;
	private boolean activated;

	public static final String PAGE_NAVIGATION = "/server/index?faces-redirect=true";
	private static final String PARAM_TASK_OID = "taskOid";
	
	public TaskListController() {
		super();
	}

	@Override
	protected String listObjects() {
		Set<Task> tasks = taskManager.getRunningTasks();
		List<TaskItem> runningTasks = getObjects();
		runningTasks.clear();
		for (Task task : tasks){
			runningTasks.add(new TaskItem(task));
		}
		
		return PAGE_NAVIGATION;		
	}
	
	private TaskItem getSelectedTaskItem() {
		String taskOid = FacesUtils.getRequestParameter(PARAM_TASK_OID);
		if (StringUtils.isEmpty(taskOid)) {
			FacesUtils.addErrorMessage("Task oid not defined in request.");
			return null;
		}

		TaskItem taskItem = getTaskItem(taskOid);
		if (StringUtils.isEmpty(taskOid)) {
			FacesUtils.addErrorMessage("Task for oid '" + taskOid + "' not found.");
			return null;
		}

		if (taskDetails == null) {
			FacesUtils.addErrorMessage("Task details controller was not autowired.");
			return null;
		}

		return taskItem;
	}

	private TaskItem getTaskItem(String resourceOid) {
		for (TaskItem item : getObjects()) {
			if (item.getOid().equals(resourceOid)) {
				return item;
			}
		}

		return null;
	}

	
	
	public String showTaskDetails(){
		TaskItem taskItem = getSelectedTaskItem();
		if (taskItem == null) {
			return null;
		}

		taskDetails.setTask(taskItem);

//		template.setSelectedLeftId(ResourceDetailsController.NAVIGATION_LEFT);
		return TaskDetailsController.PAGE_NAVIGATION;
	}
	

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public void setTaskManager(TaskManager taskManager) {
		this.taskManager = taskManager;
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
