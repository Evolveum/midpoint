/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
public class ObjectNotFoundHandler extends ErrorHandler {

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;

	@Autowired(required = true)
	private ProvisioningService provisioningService;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	private static final Trace LOGGER = TraceManager.getTrace(ObjectNotFoundHandler.class);

	/**
	 * Get the value of repositoryService.
	 * 
	 * @return the value of repositoryService
	 */
	public RepositoryService getCacheRepositoryService() {
		return cacheRepositoryService;
	}

	/**
	 * Set the value of repositoryService
	 * 
	 * Expected to be injected.
	 * 
	 * @param repositoryService
	 *            new value of repositoryService
	 */
	public void setCacheRepositoryService(RepositoryService repositoryService) {
		this.cacheRepositoryService = repositoryService;
	}

	@Override
	public <T extends ShadowType> T handleError(T shadow, FailedOperation op, Exception ex, 
			boolean doDiscovery, boolean compensate,
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

		if (!doDiscovery) {
			parentResult.recordFatalError(ex);
			if (ex instanceof ObjectNotFoundException) {
				throw (ObjectNotFoundException)ex;
			} else {
				throw new ObjectNotFoundException(ex.getMessage(), ex);
			}
		}
		
		OperationResult result = parentResult
				.createSubresult("com.evolveum.midpoint.provisioning.consistency.impl.ObjectNotFoundHandler.handleError." + op.name());
		result.addParam("shadow", shadow);
		result.addArbitraryObjectAsParam("currentOperation", op);
		if (ex.getMessage() != null) {
			result.addParam("exception", ex.getMessage());
		}

		LOGGER.trace("Start compensating object not found situation while execution operation: {}", op.name(), ex);
//		Task task = taskManager.createTaskInstance();
		ObjectDelta delta = null;
		switch (op) {
		case DELETE:
			LOGGER.debug("DISCOVERY: cannot find object {}. The operation in progress is DELETE, therefore just deleting the shadow", shadow);
			LOGGER.trace("Deleting shadow from the repository.");
			for (OperationResult subResult : parentResult.getSubresults()){
				subResult.muteError();
			}
			try {
				cacheRepositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
			} catch (ObjectNotFoundException e) {
				LOGGER.debug("Cannot delete {} in consistency compensation (discovery): {} - this is probably harmless", shadow, e.getMessage());
			}
			String message = "Object was not found on the "
					+ ObjectTypeUtil.toShortString(shadow.getResource())
					+ ". Shadow deleted from the repository to equalize the state on the resource and in the repository.";
			parentResult.recordHandledError(message);
			LOGGER.trace("Shadow deleted from the repository. Inconsistencies are now removed.");
			result.computeStatus();
			result.setStatus(OperationResultStatus.HANDLED_ERROR);		// parentResult status can be recomputed by the caller method
			delta = ObjectDelta.createDeleteDelta(shadow.getClass(), shadow.getOid(), prismContext);
			ResourceOperationDescription operationDescritpion = createOperationDescription(shadow, ex, shadow.getResource(), delta, task, result);
			changeNotificationDispatcher.notifySuccess(operationDescritpion, task, result);
			LOGGER.debug("DISCOVERY: cannot find object {}: DELETE operation handler done", shadow);
			return shadow;
		case MODIFY:
			LOGGER.debug("DISCOVERY: cannot find object {}. The operation in progress is MODIFY, therefore initiating synchronization", shadow);
			LOGGER.trace("Starting discovery to find out if the object should exist or not.");
			OperationResult handleErrorResult = result.createSubresult("com.evolveum.midpoint.provisioning.consistency.impl.ObjectNotFoundHandler.handleError.discovery");
			handleErrorResult.addContext(OperationResult.CONTEXT_RESOURCE, shadow.getResource());
			
			ObjectDeltaType shadowModifications = shadow.getObjectChange();
			Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(
					shadowModifications.getItemDelta(), shadow.asPrismObject().getDefinition());
			
			shadow.setDead(true);
			
			Collection<? extends ItemDelta> deadDeltas = PropertyDelta.createModificationReplacePropertyCollection(ShadowType.F_DEAD, shadow.asPrismObject().getDefinition(), true);
			ConstraintsChecker.onShadowModifyOperation(deadDeltas);
			cacheRepositoryService.modifyObject(ShadowType.class, shadow.getOid(), deadDeltas, result);
			
			ResourceObjectShadowChangeDescription change = createResourceObjectShadowChangeDescription(shadow,
					result);

			// notify model, that the expected account doesn't exist on the
			// resource..(the change form resource is therefore deleted) and let
			// the model to decide, if the account will be revived or unlinked
			// form the user
			// TODO: task initialication
			
			try {
				
				changeNotificationDispatcher.notifyChange(change, task, handleErrorResult);
				
			} catch (RuntimeException e) {
				handleErrorResult.recordFatalError(e);
				result.computeStatus();
				throw e;
			}
			handleErrorResult.computeStatus();
			String oidVal = null;
			String oid = findCreatedAccountOid(handleErrorResult, oidVal);
			if (oid != null){
				LOGGER.trace("Found new oid {} as a return param from model. Probably the new shadow was created.", oid);
				LOGGER.debug("DISCOVERY: object {} re-created, applying pending changes", shadow);
				LOGGER.trace("Modifying re-created object by applying pending changes:\n{}", DebugUtil.debugDump(modifications));
				try {
					ProvisioningOperationOptions options = new ProvisioningOperationOptions();
					options.setCompletePostponed(false);
					options.setDoNotDiscovery(true);
					provisioningService.modifyObject(ShadowType.class, oid, modifications, null, options, task, 
							result);
					parentResult.recordHandledError(
							"Object was recreated and modifications were applied to newly created object.");
				} catch (ObjectNotFoundException e) {
					parentResult.recordHandledError(
							"Modifications were not applied, because shadow was deleted by discovery. Repository state were refreshed and unused shadow was deleted.");
				}
				
			} else{
				LOGGER.debug("DISCOVERY: object {} deleted, application of pending changes skipped", shadow);
				parentResult.recordHandledError(
						"Object was deleted by discovery. Modification were not applied.");
			}

			// We do not need the old shadow any more. Even if the object was re-created it has a new shadow now.
			try {
				cacheRepositoryService.deleteObject(ShadowType.class, shadow.getOid(), parentResult);
			} catch (ObjectNotFoundException e) {
				// delete the old shadow that was probably deleted from the
				// user, or the new one was assigned
				LOGGER.debug("Cannot delete {} in consistency compensation (discovery): {} - this is probably harmless", shadow, e.getMessage());

			}
			result.computeStatus();
			if (parentResult.isHandledError()) {
				// Ugly hack. We shouldn't set parentResult status in the first place, as it can be overriden by computeStatus/recomputeStatus called in the parent.
				result.setStatus(OperationResultStatus.HANDLED_ERROR);
			}
			if (oid != null){
				shadowModifications.setOid(oid);
				shadow.setOid(oid);
			}
			LOGGER.debug("DISCOVERY: cannot find object {}: MODIFY operation handler done", shadow);
			return shadow;
		case GET:
			if (!compensate){
				LOGGER.trace("DISCOVERY: cannot find object {}, GET operation: handling skipped because discovery is disabled", shadow);
				result.recordFatalError(ex.getMessage(), ex);
				throw new ObjectNotFoundException(ex.getMessage(), ex);
			}
			LOGGER.debug("DISCOVERY: cannot find object {}. The operation in progress is GET, therefore initiating synchronization", shadow);
			OperationResult handleGetErrorResult = result.createSubresult("com.evolveum.midpoint.provisioning.consistency.impl.ObjectNotFoundHandler.handleError.discovery");
			handleGetErrorResult.addContext(OperationResult.CONTEXT_RESOURCE, shadow.getResource());
			
			Collection<? extends ItemDelta> deadModification = PropertyDelta.createModificationReplacePropertyCollection(ShadowType.F_DEAD, shadow.asPrismObject().getDefinition(), true);
			ConstraintsChecker.onShadowModifyOperation(deadModification);
			try {
				cacheRepositoryService.modifyObject(ShadowType.class, shadow.getOid(), deadModification, result);
			} catch (ObjectNotFoundException e) {
				// The shadow is not there. So we cannot mark it as dead.
				LOGGER.debug("Cannot modify shadow {} in consistency compensation (discovery): {} - this is probably harmless", shadow, e.getMessage());

			}
			
			shadow.setDead(true);
			ResourceObjectShadowChangeDescription getChange = createResourceObjectShadowChangeDescription(shadow,
					result);

			// notify model, that the expected account doesn't exist on the
			// resource..(the change form resource is therefore deleted) and let
			// the model to decide, if the account will be revived or unlinked
			// form the user
			// TODO: task initialication
			
			if (task == null){
				task = taskManager.createTaskInstance();
			}
			try {
				
				changeNotificationDispatcher.notifyChange(getChange, task, handleGetErrorResult);
				
			} catch (RuntimeException e) {
				LOGGER.trace("DISCOVERY: synchronization invoked for  {} ended with an error {}", shadow, e);
				handleGetErrorResult.recordFatalError(e);
				result.computeStatus();
				throw e;
			}
			
			// String oidVal = null;
			handleGetErrorResult.computeStatus();
			
			LOGGER.trace("DISCOVERY: synchronization invoked for  {} finished with status {}", shadow, handleGetErrorResult.getStatus());
			
			oid = findCreatedAccountOid(handleGetErrorResult, null);
			
			try {
				LOGGER.trace("DISCOVERY: deleting  {}", shadow);
				cacheRepositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
			} catch (ObjectNotFoundException e) {
				// delete the old shadow that was probably deleted from the
				// user, or the new one was assigned
				LOGGER.debug("Cannot delete {} in consistency compensation (discovery): {} - this is probably harmless", shadow, e.getMessage());
			}
			
			for (OperationResult subResult : parentResult.getSubresults()){
				subResult.muteError();
			}
			if (oid != null) {
                PrismObject newShadow;
                try {
                	LOGGER.trace("DISCOVERY: retrieving shadow  {}", oid);
				    Collection<SelectorOptions<GetOperationOptions>> options = 
				    		SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery());
					newShadow = provisioningService.getObject(shadow.getClass(), oid, options, task, result);
				    LOGGER.trace("DISCOVERY: retrieved {}", newShadow);
                } catch (ObjectNotFoundException e) {
                	String msg = "Strange thing did happen: new shadow ("+oid
                			+") was supposedly created for old shadow "+shadow+", however the new shadow was not found: "
                			+e.getMessage();
                	LOGGER.error(msg);
                	result.recordFatalError(msg, e);
                	parentResult.recordFatalError(msg);
                	throw new ObjectNotFoundException(msg, ex);
                } finally {
                    result.computeStatus();
                }
                LOGGER.debug("DISCOVERY: object {} re-created as {}. GET operation handler done: returning new shadow", shadow, newShadow);
				shadow = (T) newShadow.asObjectable();
				parentResult.recordHandledError("Object was re-created by the discovery.");
				result.setStatus(OperationResultStatus.HANDLED_ERROR);		// parentResult status can be recomputed by the caller method
				return shadow;
			} else {
				parentResult.recordHandledError("Object was deleted by the discovery and the invalid link was removed from the user.");
				result.computeStatus();
				result.setStatus(OperationResultStatus.HANDLED_ERROR);		// parentResult status can be recomputed by the caller method
				LOGGER.debug("DISCOVERY: object {} was deleted. GET operation handler done: throwing ObjectNotFoundException", shadow);
				throw new ObjectNotFoundException(ex.getMessage(), ex);
			}
			
		default:
			throw new ObjectNotFoundException(ex.getMessage(), ex);
		}

	}


	private ResourceObjectShadowChangeDescription createResourceObjectShadowChangeDescription(
			ShadowType shadow, OperationResult result) {
		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

		ObjectDelta<ShadowType> objectDelta = new ObjectDelta<>(ShadowType.class,
            ChangeType.DELETE, shadow.asPrismObject().getPrismContext());
		objectDelta.setOid(shadow.getOid());
		change.setObjectDelta(objectDelta);
		change.setResource(shadow.getResource().asPrismObject());
		ShadowType account = (ShadowType) shadow;
		change.setOldShadow(account.asPrismObject());
		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
		return change;
	}

	private String findCreatedAccountOid(OperationResult handleErrorResult, String oidVal) {
		if (oidVal != null) {
			return oidVal;
		}
		List<OperationResult> subresults = handleErrorResult.getSubresults();
		for (OperationResult subresult : subresults) {
			String oidValue = subresult.getReturnSingle("createdAccountOid");
			String oid = findCreatedAccountOid(subresult, oidValue);
			if (oid != null) {
				return oid;
			}
		}
		return null;
	}

}
