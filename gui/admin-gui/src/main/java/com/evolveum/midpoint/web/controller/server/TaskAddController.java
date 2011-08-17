package com.evolveum.midpoint.web.controller.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.bean.TaskItemExclusivityStatus;
import com.evolveum.midpoint.web.bean.TaskItemExecutionStatus;
import com.evolveum.midpoint.web.bean.TaskItemRecurrenceStatus;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.AccountShadowDto;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

@Controller("taskAdd")
@Scope("session")
public class TaskAddController implements Serializable {

	private static final long serialVersionUID = -6056027549741234709L;
	public static final String PAGE_NAVIGATION = "/server/taskAdd?faces-redirect=true";
	@Autowired(required = true)
	private transient TaskManager taskManager;
	private TaskItem task;
	@Autowired(required = true)
	private transient TemplateController template;
	
	@Autowired(required = true)
	private transient TaskListController taskList;
	@Autowired(required = true)
	private transient TaskItemController itemController;

	


	public TaskItemController getItemController() {
		return itemController;
	}

	public void setItemController(TaskItemController itemController) {
		this.itemController = itemController;
	}

	public String initializeTask() {
		task = new TaskItem();
		task.setHandlerUri("http://midpoint.evolveum.com/model/sync/handler-1");
		itemController.setResourceRefList(itemController.createResourceList());
		itemController.setExclusivityStatus(TaskItemExclusivityStatus.values());
		itemController.setExecutionStatus(TaskItemExecutionStatus.values());
		itemController.setRecurrenceStatus(TaskItemRecurrenceStatus.values());
		return PAGE_NAVIGATION;
	}

	public String addTask() {

		OperationResult result = new OperationResult(
				TaskAddController.class.getName() + ".addTask");
		try {
			task.setObjectRef(itemController.getRefFromName(itemController.getSelectedResurceRef()));
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
