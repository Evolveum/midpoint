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

import com.evolveum.midpoint.common.diff.CalculateXmlDiff;
import com.evolveum.midpoint.common.diff.DiffException;
import com.evolveum.midpoint.schema.exception.ConcurrencyException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.bean.TaskItem;
import com.evolveum.midpoint.web.bean.TaskItemExclusivityStatus;
import com.evolveum.midpoint.web.bean.TaskItemExecutionStatus;
import com.evolveum.midpoint.web.bean.TaskItemRecurrenceStatus;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

@Controller("taskDetails")
@Scope("session")
public class TaskDetailsController implements Serializable {

	private static final long serialVersionUID = -5990159771865483929L;
	public static final String PAGE_NAVIGATION = "/admin/server/taskDetails?faces-redirect=true";

	@Autowired(required = true)
	private transient TaskManager taskManager;
	@Autowired(required = true)
	private transient TaskItemController itemController;
	@Autowired(required = true)
	private transient ObjectTypeCatalog objectTypeCatalog;

	private List<SelectItem> resourceRefList;
	private String selectedResurceRef;
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

	public String backPerformed() {
		editMode = false;
		task = null;
		return TaskListController.PAGE_NAVIGATION;
	}

	public void editPerformed() {
		setResourceRefList(createResourceList());
		editMode = true;
	}

	public void savePerformed() {
		OperationResult result = new OperationResult(TaskAddController.class.getName() + ".modifyTask");
		try {
			task.setObjectRef(getRefFromName(getSelectedResurceRef()));
			TaskType oldObject = null;

			oldObject = taskManager.getTask(task.getOid(), result).getTaskTypeObject();

			ObjectModificationType modification = CalculateXmlDiff.calculateChanges(oldObject,
					task.toTaskType());
			taskManager.modifyTask(modification, result);
			FacesUtils.addSuccessMessage("Task modified successfully");
			result.recordSuccess();
		} catch (ObjectNotFoundException ex) {
			result.recordFatalError(
					"Couldn't get task, oid: " + task.getOid() + ". Reason: " + ex.getMessage(), ex);
			FacesUtils.addErrorMessage(
					"Couldn't get task, oid: " + task.getOid() + ". Reason: " + ex.getMessage(), ex);
			return;
		} catch (SchemaException ex) {
			result.recordFatalError("Couldn't modify task. Reason: " + ex.getMessage(), ex);
			FacesUtils.addErrorMessage("Couldn't modify task. Reason: " + ex.getMessage(), ex);
			return;
		} catch (DiffException ex) {
			result.recordFatalError("Couldn't get object change for task " + task.getName() + ". Reason: "
					+ ex.getMessage(), ex);
			FacesUtils.addErrorMessage("Couldn't get object change for task " + task.getName() + ". Reason: "
					+ ex.getMessage(), ex);
			return;
		}
		editMode = false;
	}

	public void releaseTask(ActionEvent evt) {

		// TODO: check on input values
		// TODO: error handling
		OperationResult result = new OperationResult(TaskDetailsController.class.getName() + ".releaseTask");
		try {
			Task taskToRelease = taskManager.getTask(task.getOid(), result);

			taskManager.releaseTask(taskToRelease, result);
			FacesUtils.addSuccessMessage("Task released sucessfully");
		} catch (SchemaException ex) {
			FacesUtils.addErrorMessage("Failed to release task. Reason: " + ex.getMessage(), ex);
			return;
		} catch (ObjectNotFoundException ex) {
			FacesUtils.addErrorMessage("Failed to release task. Reason: " + ex.getMessage(), ex);
			return;
		}
	}

	public void claimTask(ActionEvent evt) {

		OperationResult result = new OperationResult(TaskDetailsController.class.getName() + ".claimTask");

		try {
			Task taskToClaim = taskManager.getTask(task.getOid(), result);
			taskManager.claimTask(taskToClaim, result);
			FacesUtils.addSuccessMessage("Task claimed successfully.");
		} catch (ObjectNotFoundException ex) {
			FacesUtils.addErrorMessage("Failed to claim task. Reason: " + ex.getMessage(), ex);
			return;
		} catch (SchemaException ex) {
			FacesUtils.addErrorMessage("Failed to claim task. Reason: " + ex.getMessage(), ex);
			return;
		} catch (ConcurrencyException ex) {
			FacesUtils.addErrorMessage("Failed to claim task. Reason: " + ex.getMessage(), ex);
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

		// TODO: fix result
		OperationResult result = new OperationResult("Create task instance");

		try {
			taskManager.createTaskInstance(task.toTaskType(), result);
			FacesUtils.addSuccessMessage("Task instance created sucessfully");

		} catch (SchemaException ex) {
			FacesUtils.addErrorMessage("Failed to create task. Reason: " + ex.getMessage(), ex);
		}

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

	public String getRefFromName(String name) {

		if (name == null) {
			FacesUtils.addErrorMessage("Resource name must not be null");
			throw new IllegalArgumentException("Resource name must not be null");
		}

		for (ResourceDto resource : listResources()) {
			if (name.equals(resource.getName())) {
				return resource.getOid();
			}
		}

		FacesUtils.addErrorMessage("Resource with the name " + name + "not found.");
		throw new IllegalArgumentException("Resource with the name " + name + "not found.");
	}

	// the same method in the user details controller
	public List<ResourceDto> listResources() {
		ResourceManager resManager = ControllerUtil.getResourceManager(objectTypeCatalog);

		List<ResourceDto> resources = new ArrayList<ResourceDto>();
		try {
			Collection<GuiResourceDto> list = resManager.list();
			if (list != null) {
				resources.addAll(list);
			}
		} catch (Exception ex) {
			FacesUtils.addErrorMessage("Couldn't list resources.", ex);
		}
		return resources;
	}

	public List<SelectItem> createResourceList() {
		List<SelectItem> list = new ArrayList<SelectItem>();

		List<ResourceDto> resources = listResources();
		for (ResourceDto resourceDto : resources) {
			SelectItem si = new SelectItem((GuiResourceDto) resourceDto);
			list.add(si);
		}
		return list;
	}

	public List<SelectItem> getResourceRefList() {
		return resourceRefList;
	}

	public void setResourceRefList(List<SelectItem> resourceRefList) {
		this.resourceRefList = resourceRefList;
	}

	public String getSelectedResurceRef() {
		return selectedResurceRef;
	}

	public void setSelectedResurceRef(String selectedResurceRef) {
		this.selectedResurceRef = selectedResurceRef;
	}

	public ObjectTypeCatalog getObjectTypeCatalog() {
		return objectTypeCatalog;
	}

	public void setObjectTypeCatalog(ObjectTypeCatalog objectTypeCatalog) {
		this.objectTypeCatalog = objectTypeCatalog;
	}

	public TaskItemExclusivityStatus[] getExclusivityStatus() {
		return TaskItemExclusivityStatus.values();
	}

	public TaskItemExecutionStatus[] getExecutionStatus() {
		return TaskItemExecutionStatus.values();
	}

	public TaskItemRecurrenceStatus[] getRecurrenceStatus() {
		return TaskItemRecurrenceStatus.values();
	}

}
