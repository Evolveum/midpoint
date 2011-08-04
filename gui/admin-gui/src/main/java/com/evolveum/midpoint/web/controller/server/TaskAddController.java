package com.evolveum.midpoint.web.controller.server;

import javax.faces.event.ActionEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.util.FacesUtils;

@Controller("taskAdd")
@Scope("session")
public class TaskAddController {

	public static final String PAGE_NAVIGATION = "/server/taskAdd?faces-redirect=true";
	@Autowired(required = true)
	private transient TaskManager taskManager;
	@Autowired(required = true)
	private transient TaskListController taskList;
	private TaskItem task;
	@Autowired(required = true)
	private transient TemplateController template;

	
	public String initializeTask(){
		task = new TaskItem();
		return PAGE_NAVIGATION;
	}
	
	public String addTask() {

		OperationResult result = new OperationResult(
				TaskAddController.class.getName() + ".addTask");
		try {
			System.out.println("task properties: "+task);
			System.out.println((task.getName()));
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

		return TaskListController.PAGE_NAVIGATION;
	}
	
	public void importTask(ActionEvent evt){
		template.setSelectedTopId(TemplateController.TOP_CONFIGURATION);
	}

	public TaskItem getTask() {
		return task;
	}

	public void setTask(TaskItem task) {
		this.task = task;
	}

}
