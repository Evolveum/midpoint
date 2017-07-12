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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceOperationDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.impl.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class ConfigurationExceptionHandler extends ErrorHandler {
	
	private static final Trace LOGGER = TraceManager.getTrace(ConfigurationExceptionHandler.class);

	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService cacheRepositoryService;
	
	@Override
	public <T extends ShadowType> T handleError(T shadow, FailedOperation op, Exception ex, 
			boolean doDiscovery, boolean compensate,
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException {

		if (!doDiscovery) {
			parentResult.recordFatalError(ex);
			if (ex instanceof ConfigurationException) {
				throw (ConfigurationException)ex;
			} else {
				throw new ConfigurationException(ex.getMessage(), ex);
			}
		}
		
        ObjectDelta delta = null;
		switch (op) {
            case ADD:
                delta = ObjectDelta.createAddDelta(shadow.asPrismObject());
                break;
            case DELETE:
                delta = ObjectDelta.createDeleteDelta(shadow.getClass(), shadow.getOid(), prismContext);
                break;
            case MODIFY:
                Collection<? extends ItemDelta> modifications = null;
                if (shadow.getObjectChange() != null) {
                    ObjectDeltaType deltaType = shadow.getObjectChange();

                    modifications = DeltaConvertor.toModifications(deltaType.getItemDelta(), shadow
                            .asPrismObject().getDefinition());
                }
                delta = ObjectDelta.createModifyDelta(shadow.getOid(), modifications, shadow.getClass(), prismContext);
                break;
            case GET:
                OperationResult operationResult = parentResult.createSubresult("com.evolveum.midpoint.provisioning.consistency.impl.ConfigurationExceptionHandler.handleError." + op.name());
                operationResult.addParam("shadow", shadow);
                operationResult.addParam("currentOperation", op);
                operationResult.addParam("exception", ex.getMessage());
                for (OperationResult subRes : parentResult.getSubresults()) {
                    subRes.muteError();
                }
                operationResult.recordPartialError("Could not get " + ObjectTypeUtil.toShortString(shadow) + " from the resource "
                        + ObjectTypeUtil.toShortString(shadow.getResource())
                        + ", because of configuration error. Returning shadow from the repository");
                shadow.setFetchResult(operationResult.createOperationResultType());
                return shadow;
        }

        if (op != FailedOperation.GET) {
	//		Task task = taskManager.createTaskInstance();
			ResourceOperationDescription operationDescription = createOperationDescription(shadow, ex, shadow.getResource(),
					delta, task, parentResult);
			changeNotificationDispatcher.notifyFailure(operationDescription, task, parentResult);
		}
		
		if (shadow.getOid() == null) {
			throw new ConfigurationException("Configuration error: "+ex.getMessage(), ex);
		}
		
		Collection<ItemDelta> modification = createAttemptModification(shadow, null);
		try {
			ConstraintsChecker.onShadowModifyOperation(modification);
			cacheRepositoryService.modifyObject(shadow.asPrismObject().getCompileTimeClass(), shadow.getOid(),
					modification, parentResult);
		} catch (Exception e) {
			//this should not happen. But if it happens, we should return original exception
			LOGGER.error("Unexpected error while modifying shadow {}: {}", shadow, e.getMessage(), e);
			if (ex instanceof SchemaException) {
				throw ((SchemaException)ex);
			} else if (ex instanceof GenericFrameworkException) {
				throw ((GenericFrameworkException)ex);
			} else if (ex instanceof CommunicationException) {
				throw ((CommunicationException)ex);
			} else if (ex instanceof ObjectNotFoundException) {
				throw ((ObjectNotFoundException)ex);
			} else if (ex instanceof ObjectAlreadyExistsException) {
				throw ((ObjectAlreadyExistsException)ex);
			} else if (ex instanceof ConfigurationException) {
				throw ((ConfigurationException)ex);
			} 
		}
		
		parentResult.recordFatalError("Configuration error: " + ex.getMessage(), ex);
		throw new ConfigurationException("Configuration error: "+ex.getMessage(), ex);
	}

}
