/*
 * Copyright (c) 2010-2013 Evolveum
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

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.impl.importer.ImportAccountsFromResourceTaskHandler;
import com.evolveum.midpoint.model.impl.util.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Iterative search result handler for account synchronization. Works both for
 * reconciliation and import from resource.
 * 
 * This class is called back from the searchObjectsIterative() operation of the
 * provisioning service. It does most of the work of the "import" and resource
 * reconciliation operations.
 * 
 * @see ImportAccountsFromResourceTaskHandler
 * @see ReconciliationTaskHandler
 * 
 * @author Radovan Semancik
 * 
 */
public class SynchronizeAccountResultHandler extends AbstractSearchIterativeResultHandler<ShadowType> {

	private static final Trace LOGGER = TraceManager.getTrace(SynchronizeAccountResultHandler.class);
	
	private ResourceObjectChangeListener objectChangeListener;
	private ResourceType resource;
	private ObjectClassComplexTypeDefinition objectClass;
	private QName sourceChannel;
	private boolean forceAdd;
    private Task task;

	public SynchronizeAccountResultHandler(ResourceType resource, ObjectClassComplexTypeDefinition objectClass,
			String processShortName, Task task, ResourceObjectChangeListener objectChangeListener) {
		super(task, SynchronizeAccountResultHandler.class.getName(), processShortName, "from "+resource);
		this.objectChangeListener = objectChangeListener;
		this.resource = resource;
		this.objectClass = objectClass;
        this.task = task;
		forceAdd = false;
	}

	public boolean isForceAdd() {
		return forceAdd;
	}

	public void setForceAdd(boolean forceAdd) {
		this.forceAdd = forceAdd;
	}
	
	public QName getSourceChannel() {
		return sourceChannel;
	}

	public void setSourceChannel(QName sourceChannel) {
		this.sourceChannel = sourceChannel;
	}
	
	public ResourceType getResource() {
		return resource;
	}

	public ObjectClassComplexTypeDefinition getObjectClass() {
		return objectClass;
	}

	/*
	 * This methods will be called for each search result. It means it will be
	 * called for each account on a resource. We will pretend that the account
	 * was created and invoke notification interface.
	 * 
	 * @see
	 * com.evolveum.midpoint.provisioning.api.ResultHandler#handle(com.evolveum
	 * .midpoint.xml.ns._public.common.common_1.ObjectType)
	 */
	@Override
	protected boolean handleObject(PrismObject<ShadowType> accountShadow, OperationResult result) {
				
		ShadowType newShadowType = accountShadow.asObjectable();
		if (newShadowType.isProtectedObject() != null && newShadowType.isProtectedObject()) {
			LOGGER.trace("{} skipping {} because it is protected",new Object[] {
					getProcessShortNameCapitalized(), accountShadow});
			result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipped because it is protected");
			return true;
		}
		
		if (objectClass != null && (objectClass instanceof RefinedObjectClassDefinition) 
				&& !((RefinedObjectClassDefinition)objectClass).matches(newShadowType)) {
			LOGGER.trace("{} skipping {} because it does not match objectClass/kind/intent",new Object[] {
					getProcessShortNameCapitalized(), accountShadow});
			result.recordStatus(OperationResultStatus.NOT_APPLICABLE, "Skipped because it does not match objectClass/kind/intent");
			return true;
		}
		
		if (objectChangeListener == null) {
			LOGGER.warn("No object change listener set for {} task, ending the task", getProcessShortName());
			result.recordFatalError("No object change listener set for "+getProcessShortName()+" task, ending the task");
			return false;
		}

		// We are going to pretend that all of the objects were just created.
		// That will efficiently import them to the IDM repository
			
		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
		change.setSourceChannel(QNameUtil.qNameToUri(sourceChannel));
		change.setResource(resource.asPrismObject());
		
		if (forceAdd) {
			// We should provide shadow in the state before the change. But we are
			// pretending that it has
			// not existed before, so we will not provide it.
			ObjectDelta<ShadowType> shadowDelta = new ObjectDelta<ShadowType>(
					ShadowType.class, ChangeType.ADD, accountShadow.getPrismContext());
			//PrismObject<AccountShadowType> shadowToAdd = refinedAccountDefinition.getObjectDefinition().parseObjectType(newShadowType);
			PrismObject<ShadowType> shadowToAdd = newShadowType.asPrismObject();
			shadowDelta.setObjectToAdd(shadowToAdd);
			shadowDelta.setOid(newShadowType.getOid());
			change.setObjectDelta(shadowDelta);
			// Need to also set current shadow. This will get reflected in "old" object in lens context
			change.setCurrentShadow(accountShadow);
			
		} else {
			// No change, therefore the delta stays null. But we will set the current
			change.setCurrentShadow(accountShadow);
		}

		try {
			change.checkConsistence();
		} catch (RuntimeException ex) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Check consistence failed: {}\nChange:\n{}",ex,change.debugDump());
			}
			throw ex;
		}
		
		// Invoke the change notification
        Utils.clearRequestee(getTask());
        objectChangeListener.notifyChange(change, getTask(), result);
        
        // No exception thrown here. The error is indicated in the result. Will be processed by superclass.
        
		return task.canRun();
	}
}
