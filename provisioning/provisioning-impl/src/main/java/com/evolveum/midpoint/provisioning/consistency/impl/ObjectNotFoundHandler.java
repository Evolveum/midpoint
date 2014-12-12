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

package com.evolveum.midpoint.provisioning.consistency.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.ConstraintsChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class ObjectNotFoundHandler extends ErrorHandler {

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
//	@Autowired
//	private ChangeNotificationDispatcher changeNotificationDispatcher;
	@Autowired(required = true)
	private ProvisioningService provisioningService;
	@Autowired(required = true)
	private TaskManager taskManager;

	private String oid = null;
	
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
	public <T extends ShadowType> T handleError(T shadow, FailedOperation op, Exception ex, boolean compensate,
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		if (!isDoDiscovery(shadow.getResource())){
			throw new ObjectNotFoundException(ex.getMessage(), ex);
		}
		
		OperationResult result = parentResult
				.createSubresult("Compensating object not found situation while execution operation: " + op.name());
		result.addParam("shadow", shadow);
		result.addParam("currentOperation", op);
		if (ex.getMessage() != null) {
			result.addParam("exception", ex.getMessage());
		}

		LOGGER.trace("Start compensating object not found situation while execution operation: {}", op.name());
//		Task task = taskManager.createTaskInstance();
		ObjectDelta delta = null;
		switch (op) {
		case DELETE:
			LOGGER.trace("Deleting shadow from the repository.");
			for (OperationResult subResult : parentResult.getSubresults()){
				subResult.muteError();
			}
			cacheRepositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
			parentResult.recordHandledError("Object was not found on the "
							+ ObjectTypeUtil.toShortString(shadow.getResource())
							+ ". Shadow deleted from the repository to equalize the state on the resource and in the repository.");
			LOGGER.trace("Shadow deleted from the repository. Inconsistencies are now removed.");
			result.computeStatus();
			delta = ObjectDelta.createDeleteDelta(shadow.getClass(), shadow.getOid(), prismContext);
			ResourceOperationDescription operationDescritpion = createOperationDescription(shadow, ex, shadow.getResource(), delta, task, result);
			changeNotificationDispatcher.notifySuccess(operationDescritpion, task, result);
			return shadow;
		case MODIFY:
			LOGGER.trace("Starting discovery to find out if the object should exist or not.");
			OperationResult handleErrorResult = result.createSubresult("Discovery for situation: Object not found on the " + ObjectTypeUtil.toShortString(shadow.getResource()));
			
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
//			Task task = taskManager.createTaskInstance();
//			task.setChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
			changeNotificationDispatcher.notifyChange(change, task, handleErrorResult);
			handleErrorResult.computeStatus();
			String oidVal = null;
			foundReturnedValue(handleErrorResult, oidVal);
			if (oid != null){
				LOGGER.trace("Found new oid {} as a return param from model. Probably the new shadow was created.", oid);
			}
			
			if (oid != null ) {
				LOGGER.trace("Modifying re-created object according to given changes.");
				try {
					ProvisioningOperationOptions options = new ProvisioningOperationOptions();
					options.setCompletePostponed(false);
					provisioningService.modifyObject(ShadowType.class, oid, modifications, null, options, task, 
							result);
					parentResult.recordHandledError(
							"Object was recreated and modifications were applied to newly created object.");
				} catch (ObjectNotFoundException e) {
					parentResult.recordHandledError(
							"Modifications were not applied, because shadow was deleted by discovery. Repository state were refreshed and unused shadow was deleted.");
				}
//				return shadow;
				
			} else{
				parentResult.recordHandledError(
						"Object was deleted by discovery. Modification were not applied.");
			}
		
//				LOGGER.trace("Shadow was probably unlinked from the user, so the discovery decided that the account should not exist. Deleting also unused shadow from the repo.");
				try {
					cacheRepositoryService.deleteObject(ShadowType.class, shadow.getOid(), parentResult);
				} catch (ObjectNotFoundException e) {
					// delete the old shadow that was probably deleted from
					// the
					// user, or the new one was assigned
					//TODO: log this

				}
				result.computeStatus();
				if (oid != null){
					shadowModifications.setOid(oid);
					shadow.setOid(oid);
				}
			return shadow;
		case GET:
			if (!compensate){
				result.recordFatalError(ex.getMessage(), ex);
				throw new ObjectNotFoundException(ex.getMessage(), ex);
			}
			OperationResult handleGetErrorResult = result.createSubresult("Discovery for situation: Object not found on the " + ObjectTypeUtil.toShortString(shadow.getResource()));
			
			Collection<? extends ItemDelta> deadModification = PropertyDelta.createModificationReplacePropertyCollection(ShadowType.F_DEAD, shadow.asPrismObject().getDefinition(), true);
			ConstraintsChecker.onShadowModifyOperation(deadModification);
			cacheRepositoryService.modifyObject(ShadowType.class, shadow.getOid(), deadModification, result);
			
			shadow.setDead(true);
			ResourceObjectShadowChangeDescription getChange = createResourceObjectShadowChangeDescription(shadow,
					result);

			// notify model, that the expected account doesn't exist on the
			// resource..(the change form resource is therefore deleted) and let
			// the model to decide, if the account will be revived or unlinked
			// form the user
			// TODO: task initialication
//			Task getTask = taskManager.createTaskInstance();
			if (task == null){
				task = taskManager.createTaskInstance();
			}
			changeNotificationDispatcher.notifyChange(getChange, task, handleGetErrorResult);
			// String oidVal = null;
			handleGetErrorResult.computeStatus();
			foundReturnedValue(handleGetErrorResult, null);
			
//			if (oid != null && !shadow.getOid().equals(oid)){
				try {
					cacheRepositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);

				} catch (ObjectNotFoundException e) {
					// delete the old shadow that was probably deleted from
					// the
					// user, or the new one was assigned
					//TODO: log this

				}
//			}
			
			for (OperationResult subResult : parentResult.getSubresults()){
				subResult.muteError();
			}
			if (oid != null) {
                PrismObject prismShadow;
                try {
				    prismShadow = provisioningService.getObject(shadow.getClass(), oid, null, task, result);
                } finally {
                    result.computeStatus();
                }
				shadow = (T) prismShadow.asObjectable();
				parentResult.recordHandledError("Object was re-created by the discovery.");
				return shadow;
			} else {
				parentResult.recordHandledError("Object was deleted by the discovery and the invalid link was removed from the user.");
				result.computeStatus();
				throw new ObjectNotFoundException(ex.getMessage(), ex);
			}
			
		default:
			throw new ObjectNotFoundException(ex.getMessage(), ex);
		}

	}


	private ResourceObjectShadowChangeDescription createResourceObjectShadowChangeDescription(
			ShadowType shadow, OperationResult result) {
		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

		ObjectDelta<ShadowType> objectDelta = new ObjectDelta<ShadowType>(ShadowType.class,
				ChangeType.DELETE, shadow.asPrismObject().getPrismContext());
		objectDelta.setOid(shadow.getOid());
		change.setObjectDelta(objectDelta);
		change.setResource(shadow.getResource().asPrismObject());
		ShadowType account = (ShadowType) shadow;
		change.setOldShadow(account.asPrismObject());
		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
		return change;
	}

	private void foundReturnedValue(OperationResult handleErrorResult, String oidVal) {
		if (oidVal != null) {
			oid = oidVal;
			return;
		}
		List<OperationResult> subresults = handleErrorResult.getSubresults();
		for (OperationResult subresult : subresults) {
			String oidValue = (String) subresult.getReturn("createdAccountOid");
			foundReturnedValue(subresult, oidValue);
		}
		return;
	}

}
