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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.consistency.api.ErrorHandler;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
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

@Component
public class ObjectAlreadyExistHandler extends ErrorHandler {

	@Autowired(required = true)
	private ProvisioningService provisioningService;
	@Autowired(required = true)
	private PrismContext prismContext;
	
	private static final Trace LOGGER = TraceManager.getTrace(ObjectAlreadyExistHandler.class);

	@Override
	public <T extends ShadowType> T handleError(T shadow, FailedOperation op, Exception ex, boolean compensate, 
			Task task, OperationResult parentResult) throws SchemaException, GenericFrameworkException, CommunicationException,
			ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException, SecurityViolationException {

		if (!isDoDiscovery(shadow.getResource())){
			throw new ObjectAlreadyExistsException();
		}
		
		LOGGER.trace("Start to hanlde ObjectAlreadyExitsException.");
		
		OperationResult operationResult = parentResult
				.createSubresult("Discovery for object already exists situation. Operation: " + op.name());
		operationResult.addParam("shadow", shadow);
		operationResult.addParam("currentOperation", op);
		operationResult.addParam("exception", ex.getMessage());

		ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();

		change.setResource(shadow.getResource().asPrismObject());
		change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));

		ObjectQuery query = createQueryByIcfName(shadow);
		final List<PrismObject<ShadowType>> foundAccount = getExistingAccount(query, task, operationResult);

		PrismObject<ShadowType> resourceAccount = null;
		if (!foundAccount.isEmpty() && foundAccount.size() == 1) {
			resourceAccount = foundAccount.get(0);
		}
		
		LOGGER.trace("Found conflicting resource object: {}", resourceAccount);

		try{
		if (resourceAccount != null) {
			// Original object and found object share the same object class, therefore they must
			// also share a kind. We can use this short-cut.
			resourceAccount.asObjectable().setKind(shadow.getKind());
			change.setCurrentShadow(resourceAccount);
			// TODO: task initialization
//			Task task = taskManager.createTaskInstance();
			changeNotificationDispatcher.notifyChange(change, task, operationResult);
		}
		} finally {
			operationResult.computeStatus();
		}
		if (operationResult.isSuccess()) {
			parentResult.recordSuccess();
			parentResult.muteLastSubresultError();
		}
		
		if (compensate){
		throw new ObjectAlreadyExistsException(ex.getMessage(), ex);
		}
	
		return shadow;
	}

	private ObjectQuery createQueryByIcfName(ShadowType shadow) throws SchemaException {
		// TODO: error handling TODO TODO TODO set matching rule instead of null in equlas filter
		Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
		
		List<EqualFilter> secondaryIdentifierFilters = new ArrayList<EqualFilter>();
		
		for (ResourceAttribute<?> secondaryIdentifier : secondaryIdentifiers){
			EqualFilter equal = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, secondaryIdentifier.getElementName()), secondaryIdentifier);
			secondaryIdentifierFilters.add(equal);
		}
		OrFilter orSecondary = OrFilter.createOr((List)secondaryIdentifierFilters);
		RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class,
				prismContext, shadow.getResourceRef().getOid());
		EqualFilter objectClassFilter = EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext,
				null, shadow.getObjectClass());

		ObjectQuery query = ObjectQuery.createObjectQuery(AndFilter.createAnd(orSecondary, resourceRefFilter,
				objectClassFilter));

		return query;
	}

	private List<PrismObject<ShadowType>> getExistingAccount(ObjectQuery query, Task task, OperationResult parentResult)
			throws ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException,
			SecurityViolationException {
		final List<PrismObject<ShadowType>> foundAccount = new ArrayList<PrismObject<ShadowType>>();
		ResultHandler<ShadowType> handler = new ResultHandler() {

			@Override
			public boolean handle(PrismObject object, OperationResult parentResult) {
				return foundAccount.add(object);
			}

		};

		provisioningService.searchObjectsIterative(ShadowType.class, query, null, handler, parentResult);

		return foundAccount;
	}

}
