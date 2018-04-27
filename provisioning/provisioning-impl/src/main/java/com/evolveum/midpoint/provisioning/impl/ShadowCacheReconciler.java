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

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.util.DebugUtil;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;


@Component
public class ShadowCacheReconciler extends ShadowCache {
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheReconciler.class);

	@Override
	public String afterAddOnResource(ProvisioningContext ctx, 
			PrismObject<ShadowType> shadowToAdd, 
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		AsynchronousOperationReturnValue<PrismObject<ShadowType>> addResult = opState.getAsyncResult();
		if (addResult == null) {
			return opState.getExistingShadowOid();
		}
		PrismObject<ShadowType> shadow = addResult.getReturnValue();
		cleanShadowInRepository(shadow, parentResult);
		return shadow.getOid();
	}

	@Override
	public void afterModifyOnResource(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
			Collection<? extends ItemDelta> modifications,
			ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
			OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		LOGGER.trace("Modified shadow is reconciled. Start to clean up account after successful reconciliation.");
		try {
			cleanShadowInRepository(shadow, parentResult);
		} catch (ObjectAlreadyExistsException ex) {
			//should be never thrown
			throw new SystemException("While modifying object in the repository got exception: " + ex.getMessage(), ex);
		}
		LOGGER.trace("Shadow cleaned up successfully.");
	}
	
	private void cleanShadowInRepository(PrismObject<ShadowType> shadow, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException{
		PrismObject<ShadowType> repoShadowBefore = getRepositoryService().getObject(ShadowType.class, shadow.getOid(), null, parentResult);
		List<ItemDelta<?, ?>> itemDeltas =
				ProvisioningUtil.createShadowCleanupAndReconciliationDeltas(shadow, repoShadowBefore, getPrismContext());

		LOGGER.trace("Cleaning up repository shadow:\n{}\nThe current object is:\n{}\nAnd computed deltas are:\n{}",
				repoShadowBefore.debugDumpLazily(), shadow.debugDumpLazily(), DebugUtil.debugDumpLazily(itemDeltas));
		try {
			ConstraintsChecker.onShadowModifyOperation(itemDeltas);
			getRepositoryService().modifyObject(ShadowType.class, shadow.getOid(), itemDeltas, parentResult);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Couldn't clean-up shadow: schema violation: " + ex.getMessage(), ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			parentResult.recordFatalError("Couldn't clean-up shadow: shadow already exists: " + ex.getMessage(), ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Couldn't clean-up shadow: shadow not found: " + ex.getMessage(), ex);
			throw ex;
		}
	}

	@Override
	public Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException {
		ObjectDeltaType shadowDelta = shadow.asObjectable().getObjectChange();
		
		//TODO: error handling
		if (shadowDelta != null){
		modifications = DeltaConvertor.toModifications(
				shadowDelta.getItemDelta(), shadow.getDefinition());
		
		}
		
		// for the older versions
		ObjectDelta<? extends ObjectType> objectDelta = ObjectDelta.createModifyDelta(shadow.getOid(),
				modifications, ShadowType.class, getPrismContext());
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Storing delta to shadow:\n{}", objectDelta.debugDump());
		}
		
		ContainerDelta<ShadowAssociationType> associationDelta = objectDelta.findContainerDelta(ShadowType.F_ASSOCIATION);
		if (associationDelta != null) {
			normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToAdd());
			normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToReplace());
			normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToDelete());
			
		}
		
		if (modifications == null){
			modifications = new ArrayList<>();
		}
		
		return modifications;
		
		
	}
	
	
}
