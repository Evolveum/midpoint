package com.evolveum.midpoint.web.controller.server;

import java.io.Serializable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.bean.TaskItemExclusivityStatus;
import com.evolveum.midpoint.web.bean.TaskItemExecutionStatus;
import com.evolveum.midpoint.web.bean.TaskItemRecurrenceStatus;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

@Controller("taskDetails")
@Scope("session")
public class TaskDetailsController implements Serializable {

	private static final long serialVersionUID = -5990159771865483929L;
	public static final String PAGE_NAVIGATION = "/server/taskDetails?faces-redirect=true";

	@Autowired(required = true)
	private transient TaskManager taskManager;
	@Autowired(required = true)
	private TaskItemController itemController;

	private boolean editMode = false;

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

}
