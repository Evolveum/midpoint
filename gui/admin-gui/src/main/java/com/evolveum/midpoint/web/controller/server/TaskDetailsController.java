package com.evolveum.midpoint.web.controller.server;

import java.io.Serializable;

import javax.faces.event.ActionEvent;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.bean.TaskItemExclusivityStatus;
import com.evolveum.midpoint.web.bean.TaskItemExecutionStatus;
import com.evolveum.midpoint.web.bean.TaskItemRecurrenceStatus;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

@Controller("taskDetails")
@Scope("session")
public class TaskDetailsController implements Serializable {

	private static final long serialVersionUID = -5990159771865483929L;
	public static final String PAGE_NAVIGATION = "/server/taskDetails?faces-redirect=true";

	@Autowired(required = true)
	private transient TaskManager taskManager;
	@Autowired(required = true)
	private transient TaskItemController itemController;

	private boolean editMode = false;
	private List<OperationResultType> results;

	public TaskDetailsController() {

	}

	private TaskItem task;

	public TaskItem getTask() {
		return task;
	}

	public void setTask(TaskItem task) {
		this.task = task;
	}

	public TaskItemController getItemController() {
		return itemController;
	}

	public void setItemController(TaskItemController itemController) {
		this.itemController = itemController;
	}

	public String backPerformed(){
		editMode = false;
		task = null;
		return TaskListController.PAGE_NAVIGATION;
	}
	
	public void editPerformed() {
		itemController.setResourceRefList(itemController.createResourceList());
		itemController.setExclusivityStatus(TaskItemExclusivityStatus.values());
		itemController.setExecutionStatus(TaskItemExecutionStatus.values());
		itemController.setRecurrenceStatus(TaskItemRecurrenceStatus.values());
		editMode = true;
	}

	public void savePerformed() {
		OperationResult result = new OperationResult(
				TaskAddController.class.getName() + ".modifyTask");
		try {
			task.setObjectRef(itemController.getRefFromName(itemController
					.getSelectedResurceRef()));
			TaskType oldObject = null;

			oldObject = taskManager.getTask(task.getOid(), result)
					.getTaskTypeObject();

			ObjectModificationType modification = CalculateXmlDiff
					.calculateChanges(oldObject, task.toTaskType());
			taskManager.modifyTask(modification, result);
			FacesUtils.addSuccessMessage("Task modified successfully");
			result.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError("Couldn't get task, oid: " + task.getOid()
					+ ". Reason: " + ex.getMessage(), ex);
			FacesUtils.addErrorMessage(
					"Couldn't get task, oid: " + task.getOid() + ". Reason: "
							+ ex.getMessage(), ex);
			return;
		} catch (SchemaException ex) {
			result.recordFatalError(
					"Couldn't modify task. Reason: " + ex.getMessage(), ex);
			FacesUtils.addErrorMessage(
					"Couldn't modify task. Reason: " + ex.getMessage(), ex);
			return;
		} catch (DiffException ex) {
			result.recordFatalError("Couldn't get object change for task "
					+ task.getName() + ". Reason: " + ex.getMessage(), ex);
			FacesUtils.addErrorMessage("Couldn't get object change for task "
					+ task.getName() + ". Reason: " + ex.getMessage(), ex);
			return;
		}
		editMode = false;
	}

	public void releaseTask(ActionEvent evt) {

		//TODO: check on input values
		//TODO: error handling
		OperationResult result = new OperationResult(
				TaskDetailsController.class.getName() + ".releaseTask");
		try {
			Task taskToRelease = taskManager.getTask(task.getOid(), result);
			
			taskManager.releaseTask(taskToRelease, result);
			FacesUtils.addSuccessMessage("Task released sucessfully");
		} catch (SchemaException ex) {
			FacesUtils.addErrorMessage(
					"Failed to release task. Reason: " + ex.getMessage(), ex);
			return;
		} catch (ObjectNotFoundException ex) {
			FacesUtils.addErrorMessage(
					"Failed to release task. Reason: " + ex.getMessage(), ex);
			return;
		}
	}
	
	public void claimTask(ActionEvent evt){
		
		OperationResult result = new OperationResult(
				TaskDetailsController.class.getName() + ".claimTask");
		
		try{
		Task taskToClaim = taskManager.getTask(task.getOid(), result);
		taskManager.claimTask(taskToClaim, result);
		FacesUtils.addSuccessMessage("Task claimed successfully.");
		} catch (ObjectNotFoundException ex){
			FacesUtils.addErrorMessage(
					"Failed to claim task. Reason: " + ex.getMessage(), ex);
			return;
		} catch (SchemaException ex){
			FacesUtils.addErrorMessage(
					"Failed to claim task. Reason: " + ex.getMessage(), ex);
			return;
		} catch (ConcurrencyException ex){
			FacesUtils.addErrorMessage(
					"Failed to claim task. Reason: " + ex.getMessage(), ex);
			return;
		}
	}

	public boolean isEditMode() {
		return editMode;
	}

	public void setEditMode(boolean editMode) {
		this.editMode = editMode;
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

	public List<OperationResultType> getResults() {
		return results;
	}

	public void setResults(List<OperationResultType> results) {
		this.results = results;
	}
	
	

}
