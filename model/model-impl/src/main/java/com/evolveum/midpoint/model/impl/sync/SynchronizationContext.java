package com.evolveum.midpoint.model.impl.sync;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class SynchronizationContext<F extends FocusType> {

	private PrismObject<ShadowType> applicableShadow;
	private PrismObject<ShadowType> currentShadow;
	private PrismObject<ResourceType> resource;
	private PrismObject<SystemConfigurationType> systemConfiguration;
	private String chanel;
	
	private Task task;
	private OperationResult result;
	
	private ObjectSynchronizationType objectSynchronization;
	private Class<F> focusClass;
	private F currentOwner;
	private F correlatedOwner;
	private SynchronizationSituationType situation;
	
	private SynchronizationReactionType reaction;
	
	public SynchronizationContext(PrismObject<ShadowType> applicableShadow, PrismObject<ShadowType> currentShadow, PrismObject<ResourceType> resource, String chanel, Task task, OperationResult result) {
		this.applicableShadow = applicableShadow;
		this.currentShadow = currentShadow;
		this.resource = resource;
		this.chanel = chanel;
		this.task = task;
		this.result = result;
	}
	
	public PrismObject<ShadowType> getApplicableShadow() {
		return applicableShadow;
	}
	public PrismObject<ShadowType> getCurrentShadow() {
		return currentShadow;
	}
	public PrismObject<ResourceType> getResource() {
		return resource;
	}
	public ObjectSynchronizationType getObjectSynchronization() {
		return objectSynchronization;
	}

	public Class<F> getFocusClass() throws ConfigurationException {
		
		if (focusClass != null) {
			return focusClass;
		}
		
		if (objectSynchronization == null) {
			throw new IllegalStateException("synchronizationPolicy is null");
		}
		
		QName focusTypeQName = objectSynchronization.getFocusType();
		if (focusTypeQName == null) {
			this.focusClass = (Class<F>) UserType.class;
			return focusClass;
		}
		ObjectTypes objectType = ObjectTypes.getObjectTypeFromTypeQName(focusTypeQName);
		if (objectType == null) {
			throw new ConfigurationException(
					"Unknown focus type " + focusTypeQName + " in synchronization policy in " + resource);
		}
		this.focusClass = (Class<F>) objectType.getClassDefinition();
		return focusClass;
	}

	public F getCurrentOwner() {
		return currentOwner;
	}

	public F getCorrelatedOwner() {
		return correlatedOwner;
	}

	public SynchronizationSituationType getSituation() {
		return situation;
	}

	public void setObjectSynchronization(ObjectSynchronizationType objectSynchronization) {
		this.objectSynchronization = objectSynchronization;
	}

	public void setFocusClass(Class<F> focusClass) {
		this.focusClass = focusClass;
	}

	public void setCurrentOwner(F owner) {
		this.currentOwner = owner;
	}

	public void setCorrelatedOwner(F correlatedFocus) {
		this.correlatedOwner = correlatedFocus;
	}

	public void setSituation(SynchronizationSituationType situation) {
		this.situation = situation;
	}

	public PrismObject<SystemConfigurationType> getSystemConfiguration() {
		return systemConfiguration;
	}
	public String getChanel() {
		return chanel;
	}
	public void setApplicableShadow(PrismObject<ShadowType> applicableShadow) {
		this.applicableShadow = applicableShadow;
	}
	public void setCurrentShadow(PrismObject<ShadowType> currentShadow) {
		this.currentShadow = currentShadow;
	}
	public void setResource(PrismObject<ResourceType> resource) {
		this.resource = resource;
	}
	public void setSystemConfiguration(PrismObject<SystemConfigurationType> systemConfiguration) {
		this.systemConfiguration = systemConfiguration;
	}
	public void setChanel(String chanel) {
		this.chanel = chanel;
	}
	
	public SynchronizationReactionType getReaction() {
		return reaction;
	}
	
	public void setReaction(SynchronizationReactionType reaction) {
		this.reaction = reaction;
	}
	
	public Task getTask() {
		return task;
	}
	
	public void setTask(Task task) {
		this.task = task;
	}
	
	public OperationResult getResult() {
		return result;
	}
	
	public void setResult(OperationResult result) {
		this.result = result;
	}

	
}
