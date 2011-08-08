package com.evolveum.midpoint.web.controller.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.SelectItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.web.bean.TaskItemExclusivityStatus;
import com.evolveum.midpoint.web.bean.TaskItemExecutionStatus;
import com.evolveum.midpoint.web.bean.TaskItemRecurrenceStatus;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.util.FacesUtils;

@Controller("itemController")
@Scope("session")
public class TaskItemController implements Serializable{
	
	private static final long serialVersionUID = -320314540772984007L;

	@Autowired(required = true)
	private transient ObjectTypeCatalog objectTypeCatalog;

	private List<SelectItem> resourceRefList;
	private TaskItemExclusivityStatus[] exclusivityStatus;
	private TaskItemExecutionStatus[] executionStatus;
	private String selectedResurceRef;
	private TaskItemRecurrenceStatus[] recurrenceStatus;
	
	public TaskItemController(){
		
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

		FacesUtils.addErrorMessage("Resource with the name " + name
				+ "not found.");
		throw new IllegalArgumentException("Resource with the name " + name
				+ "not found.");
	}
	
	// the same method in the user details controller
	public List<ResourceDto> listResources() {
		ResourceManager resManager = ControllerUtil
				.getResourceManager(objectTypeCatalog);

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

	// the same method in the user details controller
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

	
	public TaskItemExclusivityStatus[] getExclusivityStatus() {
		return exclusivityStatus;
	}

	public void setExclusivityStatus(TaskItemExclusivityStatus[] exclusivityStatus) {
		this.exclusivityStatus = exclusivityStatus;
	}

	public TaskItemExecutionStatus[] getExecutionStatus() {
		return executionStatus;
	}

	public void setExecutionStatus(TaskItemExecutionStatus[] executionStatus) {
		this.executionStatus = executionStatus;
	}

	public TaskItemRecurrenceStatus[] getRecurrenceStatus() {
		return recurrenceStatus;
	}

	public void setRecurrenceStatus(TaskItemRecurrenceStatus[] recurrenceStatus) {
		this.recurrenceStatus = recurrenceStatus;
	}

	public ObjectTypeCatalog getObjectTypeCatalog() {
		return objectTypeCatalog;
	}

	public void setObjectTypeCatalog(ObjectTypeCatalog objectTypeCatalog) {
		this.objectTypeCatalog = objectTypeCatalog;
	}
	
}
