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

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.impl.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class ObjectNotFoundHandler extends HardErrorHandler {
	
	private static final String OP_DISCOVERY = ObjectNotFoundHandler.class + ".discovery";
	
	private static final Trace LOGGER = TraceManager.getTrace(ObjectNotFoundHandler.class);
	
	@Autowired private ShadowManager shadowManager;
	
	@Override
	public PrismObject<ShadowType> handleGetError(ProvisioningContext ctx,
			PrismObject<ShadowType> repositoryShadow, GetOperationOptions rootOptions, Exception cause,
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException,
			CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException,
			ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		
		if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), rootOptions)) {
			discoverDeletedShadow(ctx, repositoryShadow, cause, task, parentResult);
		}
		
		return super.handleGetError(ctx, repositoryShadow, rootOptions, cause, task, parentResult);
	}
	
	@Override
	public OperationResultStatus handleModifyError(ProvisioningContext ctx, PrismObject<ShadowType> repoShadow,
			Collection<? extends ItemDelta> modifications, ProvisioningOperationOptions options,
			ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
			Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
			throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {

		if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) {
			discoverDeletedShadow(ctx, repoShadow, cause, task, parentResult);
		}

		return super.handleModifyError(ctx, repoShadow, modifications, options, opState, cause, failedOperationResult, task, parentResult);
	}
	
	@Override
	public OperationResultStatus handleDeleteError(ProvisioningContext ctx,
			PrismObject<ShadowType> repoShadow, ProvisioningOperationOptions options,
			ProvisioningOperationState<AsynchronousOperationResult> opState, Exception cause,
			OperationResult failedOperationResult, Task task, OperationResult parentResult)
			throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {
		
		if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) {
			discoverDeletedShadow(ctx, repoShadow, cause, task, parentResult);
		}
		
		return super.handleDeleteError(ctx, repoShadow, options, opState, cause, failedOperationResult, task,
				parentResult);
	}

	private void discoverDeletedShadow(ProvisioningContext ctx, PrismObject<ShadowType> repositoryShadow,
			Exception cause, Task task, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		
		ShadowType repositoryShadowType = repositoryShadow.asObjectable();
		if (ShadowUtil.isTombstone(repositoryShadowType)) {
			// Do NOT do discovery of shadow that is already dead. This is no discovery.
			// We already know that it is dead ergo it is not present on resource.
			LOGGER.trace("Skipping discovery of shadow {} becasue it is just a tombstone.", repositoryShadow);
			return;
		}
		if (ShadowUtil.isSchroedinger(repositoryShadowType)) {
			// Box is open, quantum state collapses. Now we know that the cat is dead.
			repositoryShadow = shadowManager.markShadowTombstone(repositoryShadow, parentResult);
			// However, this is not a big surprise. No need to do discovery.
			LOGGER.trace("Skipping discovery of shadow {} becasue it is just Schroedinger's shadow turned into a tombstone.", repositoryShadow);
			return;
		}
		
		// TODO: this probably should NOT be a subresult of parentResult. We probably want new result (and maybe also task) here.
		OperationResult result = parentResult.createSubresult(OP_DISCOVERY);
		
		LOGGER.debug("DISCOVERY: discovered deleted shadow {}", repositoryShadow);		
		
		repositoryShadow = shadowManager.markShadowTombstone(repositoryShadow, result);
		
		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
		change.setResource(ctx.getResource().asPrismObject());
		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
		change.setObjectDelta(repositoryShadow.createDeleteDelta());
		change.setOldShadow(repositoryShadow);
		change.setCurrentShadow(null);
		// TODO: task initialization
//		Task task = taskManager.createTaskInstance();
		changeNotificationDispatcher.notifyChange(change, task, result);
		
		result.computeStatus();
	}

	@Override
	protected void throwException(Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result)
			throws ObjectNotFoundException {
		recordCompletionError(cause, opState, result);
		if (cause instanceof ObjectNotFoundException) {
			throw (ObjectNotFoundException)cause;
		} else {
			throw new ObjectNotFoundException(cause.getMessage(), cause);
		}
	}

}
