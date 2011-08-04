package com.evolveum.midpoint.web.controller.server;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
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
	private TaskItem selectedTask;

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

	public String deleteTask() {

		
		if (selectedTask.getOid() == null) {
			FacesUtils.addErrorMessage("No task to delete defined");
			throw new IllegalArgumentException("No task to delete defined.");
		}

		OperationResult result = new OperationResult(
				TaskDetailsController.class.getName() + ".deleteTask");
		result.addParam("taskOid", selectedTask.getOid());

		try {
			taskManager.deleteTask(selectedTask.getOid(), result);
			getObjects().remove(selectedTask);
			FacesUtils.addSuccessMessage("Task deleted sucessfully");
		} catch (ObjectNotFoundException ex) {
			FacesUtils.addErrorMessage("Task with oid " + selectedTask.getOid()
					+ " not found. Reason: " + ex.getMessage(), ex);
			result.recordFatalError("Task with oid " + selectedTask.getOid()
					+ " not found. Reason: " + ex.getMessage(), ex);
			return null;
		}

		return PAGE_NAVIGATION;
	}
	
	public String showTaskDetails(){
		selectedTask = getSelectedTaskItem();
		if (selectedTask == null) {
			return null;
		}

		taskDetails.setTask(selectedTask);

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

	public TaskItem getSelectedTask() {
		return selectedTask;
	}

	public void setSelectedTask(TaskItem selectedTask) {
		this.selectedTask = selectedTask;
	}

	

}
