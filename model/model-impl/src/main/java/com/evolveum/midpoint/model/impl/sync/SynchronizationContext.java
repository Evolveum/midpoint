/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl.sync;

import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;

import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.util.PrismMonitor;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationReactionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class SynchronizationContext<F extends FocusType> {
	
	private static final Trace LOGGER = TraceManager.getTrace(SynchronizationContext.class);

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
	
	private boolean unrelatedChange = false;
	
	private boolean shadowExistsInRepo = true;
	private boolean forceIntentChange;
	
	public SynchronizationContext(PrismObject<ShadowType> applicableShadow, PrismObject<ShadowType> currentShadow, PrismObject<ResourceType> resource, String chanel, Task task, OperationResult result) {
		this.applicableShadow = applicableShadow;
		this.currentShadow = currentShadow;
		this.resource = resource;
		this.chanel = chanel;
		this.task = task;
		this.result = result;
	}
	
	public boolean isSynchronizationEnabled() {
		if (objectSynchronization == null) {
			return false;
		}
		return BooleanUtils.isNotFalse(objectSynchronization.isEnabled());
	}
	
	public boolean isProtected() {
		if (applicableShadow == null) {
			return false;
		}

		ShadowType currentShadowType = applicableShadow.asObjectable();
		return BooleanUtils.isTrue(currentShadowType.isProtectedObject());
	}
	
	public boolean isSatisfyTaskConstraints() throws SchemaException {
		
		ShadowKindType kind = getTaskPropertyValue(SchemaConstants.MODEL_EXTENSION_KIND);
		String intent = getTaskPropertyValue(SchemaConstants.MODEL_EXTENSION_INTENT);
		QName objectClass = getTaskPropertyValue(SchemaConstants.MODEL_EXTENSION_OBJECTCLASS);
		
		LOGGER.trace("checking task constraints: {}", task);
		
		boolean isApplicable = SynchronizationUtils.isPolicyApplicable(objectClass, kind, intent, objectSynchronization, resource, true);
		//this mean that kind/intent are null in the task..but this can be a case, so check if at least the objectClass is the same
		if (!isApplicable && objectClass != null) {
			return QNameUtil.matchAny(objectClass, objectSynchronization.getObjectClass());
		}
		
		return isApplicable;
	}
	
	//TODO multi-threded tasks?
	private <T> T getTaskPropertyValue(QName propertyName) {
		PrismProperty<T> prop = task.getExtensionProperty(propertyName);
		if (prop == null || prop.isEmpty()) {
			return null;
		}
		
		return prop.getRealValue();
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
	
	public boolean isUnrelatedChange() {
		return unrelatedChange;
	}
	
	public void setUnrelatedChange(boolean unrelatedChange) {
		this.unrelatedChange = unrelatedChange;
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

	public boolean isShadowExistsInRepo() {
		return shadowExistsInRepo;
	}

	public void setShadowExistsInRepo(boolean shadowExistsInRepo) {
		this.shadowExistsInRepo = shadowExistsInRepo;
	}
	
	public boolean isForceIntentChange() {
		return forceIntentChange;
	}
	
	public void setForceIntentChange(boolean forceIntentChange) {
		this.forceIntentChange = forceIntentChange;
	}
	
}
