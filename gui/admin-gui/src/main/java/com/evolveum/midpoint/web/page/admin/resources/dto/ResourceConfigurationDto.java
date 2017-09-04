package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class ResourceConfigurationDto implements Serializable{

	private ResourceObjectTypeDefinitionType objectTypeDefinition;

	private boolean sync;
	private List<TaskType> definedTasks;

	public ResourceConfigurationDto(ResourceObjectTypeDefinitionType obejctTypeDefinition, boolean sync, List<TaskType> definedTasks) {
		this.objectTypeDefinition = obejctTypeDefinition;
		this.sync = sync;
		this.definedTasks = definedTasks;
	}

	public List<TaskType> getDefinedTasks() {
		return definedTasks;
	}

	public ResourceObjectTypeDefinitionType getObjectTypeDefinition() {

		return objectTypeDefinition;
	}

	public boolean isSync() {
		return sync;
	}

	public void setSync(boolean sync) {
		this.sync = sync;
	}

	public void setDefinedTasks(List<TaskType> definedTasks) {
		this.definedTasks = definedTasks;
	}

	public void setObjectTypeDefinition(ResourceObjectTypeDefinitionType objectTypeDefinition) {
		this.objectTypeDefinition = objectTypeDefinition;
	}



}
