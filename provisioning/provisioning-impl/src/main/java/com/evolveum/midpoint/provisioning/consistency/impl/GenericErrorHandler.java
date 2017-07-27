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

import com.evolveum.midpoint.provisioning.impl.ConstraintsChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class GenericErrorHandler extends ErrorHandler{

	private static final String COMPENSATE_OPERATION = GenericErrorHandler.class.getName()+".compensate";
	private static final Trace LOGGER = TraceManager.getTrace(GenericErrorHandler.class);

	@Autowired(required = true)
	private ProvisioningService provisioningService;

//	@Autowired(required = true)
//	private ShadowCacheReconciler shadowCacheFinisher;
	
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	
//	@Autowired
//	private OperationFinisher operationFinisher;
	
	
	@Override
	public <T extends ShadowType> T handleError(T shadow, FailedOperation op, Exception ex, 
			boolean doDiscovery, boolean compensate, 
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		
		if (!doDiscovery) {
			parentResult.recordFatalError(ex);
			if (ex instanceof GenericFrameworkException) {
				throw (GenericFrameworkException)ex;
			} else {
				throw new GenericFrameworkException(ex.getMessage(), ex);
			}
		}
		
//		OperationResult result = OperationResult.createOperationResult(shadow.getResult());
		String operation = (shadow.getFailedOperationType() == null ? "null" : shadow.getFailedOperationType().name());
		
		OperationResult result = parentResult.createSubresult(COMPENSATE_OPERATION);
		result.addContext("compensatedOperation", operation);
		result.addContext("operationType",op.name());
		result.addParam("shadow", shadow);
		result.addArbitraryObjectAsParam("currentOperation", op);
		result.addParam("reconciled", true);
		
		switch (op) {
		case GET:
				if (ShadowUtil.isDead(shadow) || ResourceTypeUtil.isDown(shadow.getResource()) || !compensate) {
					result.recordStatus(OperationResultStatus.PARTIAL_ERROR,
							"Unable to get object from the resource. Probably it has not been created yet because of previous unavailability of the resource.");
					result.computeStatus();
					shadow.setFetchResult(parentResult.createOperationResultType());
					return shadow;
				}

				if (shadow.getFailedOperationType() == null) {
					String message = "Generic error in the connector. Can't process shadow "
							+ ObjectTypeUtil.toShortString(shadow) + ": " + ex.getMessage();
					result.recordFatalError(message, ex);
					throw new GenericFrameworkException(message, ex);
				}
				try {
					//ProvisioningOperationOptions.createCompletePostponed(false);
					provisioningService.refreshShadow(shadow.asPrismObject(), null, task, result);
					result.computeStatus();
					if (result.isSuccess()) {
						LOGGER.trace("Postponed operation was finished successfully while getting shadow. Getting new object.");
						PrismObject prismShadow = provisioningService.getObject(shadow.getClass(),
								shadow.getOid(), null, task, result);
						if (!prismShadow.hasCompleteDefinition()){
							LOGGER.trace("applying definitions to shadow");
							provisioningService.applyDefinition(prismShadow, task, result);
						}
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Got {} after finishing postponed operation.", prismShadow.debugDump());
					}
						shadow = (T) prismShadow.asObjectable();
					}
//				} catch(Exception e){
//					result.recordFatalError("Could not finish operation " + operation + ". Reason: " + e.getMessage()));
//					// just throw the origin exception
//					throw new GenericFrameworkException(ex);
				} finally {
					result.computeStatus();
				}
				return shadow;

		case MODIFY:
			if (shadow.getFailedOperationType() == null) {
				String message = "Generic error in the connector. Can't process shadow " + ObjectTypeUtil.toShortString(shadow) + ". ";
				result.recordFatalError(message, ex);
				throw new GenericFrameworkException(message, ex);
			}
			// get the modifications from the shadow before the account
			// is created, because after successful creation of account,
			// the modification will be lost
			Collection<? extends ItemDelta> modifications = null;
			if (shadow.getObjectChange() != null) {
				ObjectDeltaType deltaType = shadow.getObjectChange();
				modifications = DeltaConvertor.toModifications(deltaType.getItemDelta(), shadow
							.asPrismObject().getDefinition());
			}
			PropertyDelta.applyTo(modifications, shadow.asPrismObject());
			provisioningService.refreshShadow(shadow.asPrismObject(), null, task, result);						
			result.computeStatus();

			if (!result.isSuccess()) {
				// account wasn't created, probably resource is
				// still down, or there is other reason.just save the
				// pending modifications to the shadow in the
				// repository..next time by processing this shadow, we can try again
				// TODO: probably there is a need to union current changes with previous
				ConstraintsChecker.onShadowModifyOperation(modifications);
				cacheRepositoryService.modifyObject(ShadowType.class, shadow.getOid(), modifications,
					result);
				result.recordHandledError("Modifications not applied to the object, because resource is unreachable. They are stored to the shadow and will be applied when the resource goes online.");
			}
			return shadow;
			
		case DELETE:
			cacheRepositoryService.deleteObject(shadow.getClass(), shadow.getOid(), result);
			result.recordStatus(OperationResultStatus.HANDLED_ERROR, "Object has been not created on the resource yet. Shadow deleted from the repository");
			return shadow;
		default:
			result.recordFatalError("Can't process "
					+ ObjectTypeUtil.toShortString(shadow) + ": "+ex.getMessage(), ex);
			if (shadow.getOid() == null){
				throw new GenericFrameworkException("Can't process "
						+ ObjectTypeUtil.toShortString(shadow) + ": "+ex.getMessage(), ex);
			}
			
			Collection<ItemDelta> modification = createAttemptModification(shadow, null);
			ConstraintsChecker.onShadowModifyOperation(modification);
			cacheRepositoryService.modifyObject(shadow.asPrismObject().getCompileTimeClass(), shadow.getOid(), modification, parentResult);
			
			String message = "Can't process " + ObjectTypeUtil.toShortString(shadow) + ". ";
			result.recordFatalError(message, ex);
			throw new GenericFrameworkException(message, ex);
		}		
	}

}
