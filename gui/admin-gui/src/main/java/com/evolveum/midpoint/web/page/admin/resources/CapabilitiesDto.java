package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public class CapabilitiesDto {
	
	private static final String ID_ACTIVATION = "activation";
	private static final String ID_CREDENTIALS = "credentials";
	private static final String ID_LIVE_SYNC = "liveSync";
	private static final String ID_ = "test";
	private static final String ID_SCHEMA = "schema";
	private static final String ID_CREATE = "create";
	private static final String ID_UPDATE = "update";
	
	private static final String ID_ADD_ATTRIBUE_VALUES = "addAttributeValues";
	private static final String ID_REMOVE_ATTRIBUTE_VALUES = "removeAttributeValues";
	private static final String ID_DELETE = "delete";
	private static final String ID_READ = "read";
	private static final String ID_AUXILIARY_OBJECT_CLASS = "auxiliaryObjectClass";
	private static final String ID_CONNECTOR_SCRIPT = "connectorScript";
	private static final String ID_HOST_SCRIPT = "hostScript";
	
	private boolean activation;
	
	private boolean credentials;
	
	private boolean liveSync;
	
	private boolean test;
	
	private boolean schema;
	
	private boolean create;
	
	private boolean update;
	
	private boolean addAttributeValues;
	
	private boolean removeAttributeValues;
	
	private boolean delete;
	
	private boolean read;
	
	private boolean auxiliaryObjectClass;
	
	private boolean connectorScript;
	
	private boolean hostScript;
	
	public CapabilitiesDto(ResourceType resource){
		activation = ResourceTypeUtil.hasActivationCapability(resource);
		credentials = ResourceTypeUtil.hasCredentialsCapability(resource);
		
//		liveSync = ResourceTypeUtil.has
		//test
		//schema
		
		create = ResourceTypeUtil.hasCreateCapability(resource);
		update = ResourceTypeUtil.hasUpdateCapability(resource);
//		addAttributeValues = ResourceTypeUtil.hasA
//		removeAttributeValues
		
		delete = ResourceTypeUtil.hasDeleteCapability(resource);
		read = ResourceTypeUtil.hasReadCapability(resource);
//		auxiliaryObjectClass
//		connectorScript = ResourceTypeUtil.has
//		hostScript
	}
	

	public boolean isActivation() {
		return activation;
	}

	public void setActivation(boolean activation) {
		this.activation = activation;
	}

	public boolean isCredentials() {
		return credentials;
	}

	public void setCredentials(boolean credentials) {
		this.credentials = credentials;
	}

	public boolean isLiveSync() {
		return liveSync;
	}

	public void setLiveSync(boolean liveSync) {
		this.liveSync = liveSync;
	}

	public boolean isTest() {
		return test;
	}

	public void setTest(boolean test) {
		this.test = test;
	}

	public boolean isSchema() {
		return schema;
	}

	public void setSchema(boolean schema) {
		this.schema = schema;
	}

	public boolean isCreate() {
		return create;
	}

	public void setCreate(boolean create) {
		this.create = create;
	}

	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	public boolean isAddAttributeValues() {
		return addAttributeValues;
	}

	public void setAddAttributeValues(boolean addAttributeValues) {
		this.addAttributeValues = addAttributeValues;
	}

	public boolean isRemoveAttributeValues() {
		return removeAttributeValues;
	}

	public void setRemoveAttributeValues(boolean removeAttributeValues) {
		this.removeAttributeValues = removeAttributeValues;
	}

	public boolean isDelete() {
		return delete;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public boolean isAuxiliaryObjectClass() {
		return auxiliaryObjectClass;
	}

	public void setAuxiliaryObjectClass(boolean auxiliaryObjectClass) {
		this.auxiliaryObjectClass = auxiliaryObjectClass;
	}

	public boolean isConnectorScript() {
		return connectorScript;
	}

	public void setConnectorScript(boolean connectorScript) {
		this.connectorScript = connectorScript;
	}

	public boolean isHostScript() {
		return hostScript;
	}

	public void setHostScript(boolean hostScript) {
		this.hostScript = hostScript;
	}
	
	

}
