package com.evolveum.midpoint.web.controller.server;

import java.io.Serializable;

import javax.faces.event.ActionEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.util.FacesUtils;

@Controller("taskAdd")
@Scope("session")
public class TaskAddController implements Serializable {

	private static final long serialVersionUID = -6056027549741234709L;
	public static final String PAGE_NAVIGATION = "/admin/server/taskAdd?faces-redirect=true";
	@Autowired(required = true)
	private transient TaskManager taskManager;
	private TaskItem task;
	@Autowired(required = true)
	private transient TemplateController template;
	
	@Autowired(required = true)
	private transient TaskListController taskList;
	@Autowired(required = true)
	private transient TaskDetailsController detailsController;

	


	public TaskDetailsController getDetailsController() {
		return detailsController;
	}

	public void setDetailsController(TaskDetailsController detailsController) {
		this.detailsController = detailsController;
	}

	public String initializeTask() {
		task = new TaskItem();
		task.setHandlerUri("http://midpoint.evolveum.com/model/sync/handler-1");
		
		detailsController.setResourceRefList(detailsController.createResourceList());
		return PAGE_NAVIGATION;
	}

	public String addTask() {

		OperationResult result = new OperationResult(
				TaskAddController.class.getName() + ".addTask");
		try {
			task.setObjectRef(detailsController.getRefFromName(detailsController.getSelectedResurceRef()));
			taskManager.addTask(task.toTaskType(), result);
			FacesUtils.addSuccessMessage("Task added successfully");
			result.recordSuccess();
		} catch (SchemaException ex) {
			result.recordFatalError(
					"Couldn't add task. Reason: " + ex.getMessage(), ex);
			FacesUtils.addErrorMessage(
					"Couldn't add task. Reason: " + ex.getMessage(), ex);
			return null;
		} catch (ObjectAlreadyExistsException ex) {
			result.recordFatalError(
					"Couldn't add task. Reason: " + ex.getMessage(), ex);
			FacesUtils.addErrorMessage(
					"Couldn't add task. Reason: " + ex.getMessage(), ex);
			return null;
		}

		taskList.listFirst();
		template.setSelectedLeftId(TaskListController.PAGE_LEFT_NAVIGATION);
		return TaskListController.PAGE_NAVIGATION;
	}

	public void importTask(ActionEvent evt) {
		template.setSelectedTopId(TemplateController.TOP_CONFIGURATION);
	}

	public TaskItem getTask() {
		return task;
	}

	public void setTask(TaskItem task) {
		this.task = task;
	}

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public void setTaskManager(TaskManager taskManager) {
		this.taskManager = taskManager;
	}
}
