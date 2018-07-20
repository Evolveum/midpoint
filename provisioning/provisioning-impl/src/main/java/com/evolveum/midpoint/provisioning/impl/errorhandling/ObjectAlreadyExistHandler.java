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
import java.util.List;

import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Component
public class ObjectAlreadyExistHandler extends HardErrorHandler {
	
	private static final String OP_DISCOVERY = ObjectAlreadyExistHandler.class + ".discovery";
	
	private static final Trace LOGGER = TraceManager.getTrace(ObjectAlreadyExistHandler.class);
	
	@Autowired ProvisioningService provisioningService;
	
	@Override
	public OperationResultStatus handleAddError(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
			ProvisioningOperationOptions options,
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
			throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
			SecurityViolationException, ExpressionEvaluationException {
		
		if (isDoDiscovery(ctx.getResource(), options)) {
			discoverConflictingShadow(ctx, shadowToAdd, options, opState, cause, failedOperationResult, task, parentResult);
		}
		
		throwException(cause, opState, parentResult);
		return OperationResultStatus.FATAL_ERROR; // not reached
	}
	
	private void discoverConflictingShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
			ProvisioningOperationOptions options,
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult) 
					throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {
		
		// TODO: this probably should NOT be a subresult of parentResult. We probably want new result (and maybe also task) here.
		OperationResult result = parentResult.createSubresult(OP_DISCOVERY);
		
		ObjectQuery query = createQueryBySecondaryIdentifier(shadowToAdd.asObjectable());
		final List<PrismObject<ShadowType>> foundAccounts = getExistingAccount(query, task, result);
		
		PrismObject<ShadowType> conflictingShadow = null;
		if (!foundAccounts.isEmpty() && foundAccounts.size() == 1) {
			conflictingShadow = foundAccounts.get(0);
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Processing \"already exists\" error for shadow:\n{}\nConflicting shadow:\n{}", shadowToAdd.debugDump(1), conflictingShadow==null?"  null":conflictingShadow.debugDump(1));
		}
		
		try{
			if (conflictingShadow != null) {
				// Original object and found object share the same object class, therefore they must
				// also share a kind. We can use this short-cut.
				conflictingShadow.asObjectable().setKind(shadowToAdd.asObjectable().getKind());
				
				ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
				change.setResource(ctx.getResource().asPrismObject());
				change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
				change.setCurrentShadow(conflictingShadow);
				// TODO: task initialization
//				Task task = taskManager.createTaskInstance();
				changeNotificationDispatcher.notifyChange(change, task, result);
			}
			} finally {
				result.computeStatus();
			}
	}
	
	private ObjectQuery createQueryBySecondaryIdentifier(ShadowType shadow) throws SchemaException {
		// TODO: error handling TODO TODO TODO set matching rule instead of null in equlas filter
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
		S_AtomicFilterEntry q = QueryBuilder.queryFor(ShadowType.class, prismContext);
		q = q.block();
		if (secondaryIdentifiers.isEmpty()) {
			for (ResourceAttribute<?> primaryIdentifier: ShadowUtil.getPrimaryIdentifiers(shadow)) {
				q = q.itemAs(primaryIdentifier).or();
			}
		} else {
			// secondary identifiers connected by 'or' clause
			for (ResourceAttribute<?> secondaryIdentifier : secondaryIdentifiers) {
				q = q.itemAs(secondaryIdentifier).or();
			}
		}
		q = q.none().endBlock().and();
		// resource + object class
		q = q.item(ShadowType.F_RESOURCE_REF).ref(shadow.getResourceRef().getOid()).and();
		return q.item(ShadowType.F_OBJECT_CLASS).eq(shadow.getObjectClass()).build();
	}
	
	private List<PrismObject<ShadowType>> getExistingAccount(ObjectQuery query, Task task, OperationResult parentResult)
		throws ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException,
		SecurityViolationException, ExpressionEvaluationException {
		final List<PrismObject<ShadowType>> foundAccount = new ArrayList<>();
		ResultHandler<ShadowType> handler = new ResultHandler() {
		
			@Override
			public boolean handle(PrismObject object, OperationResult parentResult) {
				return foundAccount.add(object);
			}
		
		};
		
		provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, task, parentResult);
		
		return foundAccount;
	}
	
	@Override
	protected void throwException(Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result)
			throws ObjectAlreadyExistsException {
		recordCompletionError(cause, opState, result);
		if (cause instanceof ObjectAlreadyExistsException) {
			throw (ObjectAlreadyExistsException)cause;
		} else {
			throw new ObjectAlreadyExistsException(cause.getMessage(), cause);
		}
	}

}
