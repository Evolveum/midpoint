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

package com.evolveum.midpoint.provisioning.impl.errorhandling;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ResourceManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public abstract class ErrorHandler {
	
	private static final Trace LOGGER = TraceManager.getTrace(ErrorHandler.class);
	
	@Autowired protected ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired private ResourceManager resourceManager;
	protected PrismContext prismContext;
	
	public abstract PrismObject<ShadowType> handleGetError(ProvisioningContext ctx,
			PrismObject<ShadowType> repositoryShadow,
			GetOperationOptions rootOptions,
			Exception cause,
			Task task,
			OperationResult parentResult) 
					throws SchemaException, GenericFrameworkException, CommunicationException,
					ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
					SecurityViolationException, ExpressionEvaluationException;
			
	
	public abstract OperationResultStatus handleAddError(ProvisioningContext ctx,
			PrismObject<ShadowType> shadowToAdd,
			ProvisioningOperationOptions options,
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			Exception cause,
			OperationResult failedOperationResult,
			Task task,
			OperationResult parentResult)
				throws SchemaException, GenericFrameworkException, CommunicationException,
				ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
				SecurityViolationException, ExpressionEvaluationException;
	
	protected OperationResultStatus postponeAdd(ProvisioningContext ctx,
			PrismObject<ShadowType> shadowToAdd,
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			OperationResult failedOperationResult,
			OperationResult result) {
		LOGGER.trace("Postponing ADD operation for {}", shadowToAdd);
		opState.setExecutionStatus(PendingOperationExecutionStatusType.EXECUTING);
		AsynchronousOperationReturnValue<PrismObject<ShadowType>> asyncResult = new AsynchronousOperationReturnValue<>();
		asyncResult.setOperationResult(failedOperationResult);
		opState.setAsyncResult(asyncResult);
		if (opState.getAttemptNumber() == null) {
			opState.setAttemptNumber(1);
		}
		result.recordInProgress();
		return OperationResultStatus.IN_PROGRESS;
	}

	
	public abstract void handleModifyError(ProvisioningContext ctx,
			PrismObject<ShadowType> repoShadow,
			Collection<? extends ItemDelta> modifications,
			ProvisioningOperationOptions options,
			ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
			Exception cause,
			OperationResult failedOperationResult,
			Task task,
			OperationResult parentResult)
				throws SchemaException, GenericFrameworkException, CommunicationException,
				ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
				SecurityViolationException, ExpressionEvaluationException;

	public abstract void handleDeleteError(ProvisioningContext ctx,
			PrismObject<ShadowType> repoShadow,
			ProvisioningOperationOptions options,
			ProvisioningOperationState<AsynchronousOperationResult> opState,
			Exception cause,
			OperationResult failedOperationResult,
			Task task,
			OperationResult parentResult)
				throws SchemaException, GenericFrameworkException, CommunicationException,
				ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
				SecurityViolationException, ExpressionEvaluationException;
	
	/**
	 * Throw exception of appropriate type.
	 */
	protected abstract void throwException(Exception cause, OperationResult result)
			throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException;
		
	protected void markResourceDown(ResourceType resource, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		resourceManager.modifyResourceAvailabilityStatus(resource.asPrismObject(), 
				AvailabilityStatusType.DOWN, parentResult);
	}
	
	protected boolean isOperationRetryEnabled(ResourceType resource) {
		if (resource.getConsistency() == null) {
			return true;
		}
		
		if (resource.getConsistency().isPostpone() == null) {
			return true;
		}
		
		return resource.getConsistency().isPostpone();
	}
	
	protected boolean isCompletePostponedOperations(ProvisioningOperationOptions options) {
		return ProvisioningOperationOptions.isCompletePostponed(options);
	}
	
	protected boolean isDoDiscovery(ResourceType resource, GetOperationOptions rootOptions) {
		return !GetOperationOptions.isDoNotDiscovery(rootOptions) && isDoDiscovery(resource);
	}

	protected boolean isDoDiscovery(ResourceType resource, ProvisioningOperationOptions options) {
		return !ProvisioningOperationOptions.isDoNotDiscovery(options) && isDoDiscovery(resource);
	}
	
	protected boolean isDoDiscovery (ResourceType resource) {
		if (resource == null) {
			return true;
		}
		if (resource.getConsistency() == null) {
			return true;
		}
		
		if (resource.getConsistency().isDiscovery() == null) {
			return true;
		}
		
		return resource.getConsistency().isDiscovery();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	public abstract <T extends ShadowType> T handleError(T shadow, FailedOperation op, Exception ex, 
//			boolean doDiscovery, boolean compensate, Task task, OperationResult parentResult) 
//					throws SchemaException, GenericFrameworkException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException;

	
	protected <T extends ShadowType> Collection<ItemDelta> createAttemptModification(T shadow,
			Collection<ItemDelta> modifications) {

		if (modifications == null) {
			modifications = new ArrayList<>();
		}
		PropertyDelta attemptDelta = PropertyDelta.createReplaceDelta(shadow.asPrismObject().getDefinition(),
				ShadowType.F_ATTEMPT_NUMBER, getAttemptNumber(shadow));
		modifications.add(attemptDelta);
		return modifications;
	}

	protected Integer getAttemptNumber(ShadowType shadow) {
		Integer attemptNumber = (shadow.getAttemptNumber() == null ? 0 : shadow.getAttemptNumber()+1);
		return attemptNumber;
	}
	
	protected ResourceOperationDescription createOperationDescription(ShadowType shadowType, Exception ex, ResourceType resource, ObjectDelta delta, Task task, OperationResult result) {
		ResourceOperationDescription operationDescription = new ResourceOperationDescription();
		operationDescription.setCurrentShadow(shadowType.asPrismObject());
		if (resource != null){
			operationDescription.setResource(resource.asPrismObject());
		}
		if (task != null) {
			operationDescription.setSourceChannel(task.getChannel());
		}
		operationDescription.setObjectDelta(delta);

        // fill-in the message if necessary
        OperationResult storedResult = result != null ? result.clone() : new OperationResult("dummy");      // actually, the result shouldn't be null anyway
        storedResult.computeStatusIfUnknown();
        if (storedResult.getMessage() == null && ex != null) {
            storedResult.recordStatus(storedResult.getStatus(), ex.getMessage(), ex);
        }
        operationDescription.setResult(storedResult);

		return operationDescription;
	}
	
}
