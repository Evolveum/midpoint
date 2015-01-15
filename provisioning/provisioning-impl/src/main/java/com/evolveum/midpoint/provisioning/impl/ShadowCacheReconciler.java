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

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;


@Component
public class ShadowCacheReconciler extends ShadowCache{
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheReconciler.class);

	@Override
	public String afterAddOnResource(PrismObject<ShadowType> shadow, ResourceType resource, 
			RefinedObjectClassDefinition objectClassDefinition, OperationResult parentResult)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		
		cleanShadowInRepository(shadow, parentResult);

		return shadow.getOid();
	}

	@Override
	public void afterModifyOnResource(PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		LOGGER.trace("Modified shadow is reconciled. Start to clean up account after successfull reconciliation.");
		try{
		cleanShadowInRepository(shadow, parentResult);
		} catch (ObjectAlreadyExistsException ex){
			//should be never thrown
			throw new SystemException("While modifying object in the repository got exception: " + ex.getMessage(), ex);
		}
		LOGGER.trace("Shadow cleaned up successfully.");
		
	}
	
	private void cleanShadowInRepository(PrismObject<ShadowType> shadow, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException{
		PrismObject<ShadowType> normalizedShadow = shadow.clone();
		
		ProvisioningUtil.normalizeShadow(normalizedShadow.asObjectable(), parentResult);

		LOGGER.trace("normalized shadow {}", normalizedShadow.debugDump());
		// FIXME: ugly hack, need to be fixed (problem with comparing operation
		// result, because it was changed and in this call it is different as
		// one in repo, therefore the following if)
		PrismObject<ShadowType> oldShadow = shadow.clone();
		ShadowUtil.getAttributesContainer(oldShadow).clear();
		PrismObject<ShadowType> repoShadow = getRepositoryService().getObject(ShadowType.class,
				shadow.getOid(), null, parentResult);
		ShadowType repoShadowType = repoShadow.asObjectable();
		if (repoShadowType.getResult() != null) {
			if (!repoShadowType.getResult().equals(oldShadow.asObjectable().getResult())) {
				oldShadow.asObjectable().setResult(repoShadowType.getResult());
			}
		}
//		ShadowUtil.getAttributesContainer(repoShadow).clear();
		
		LOGGER.trace("origin shadow with failure description {}", oldShadow.debugDump());
		
		ObjectDelta delta = oldShadow.diff(normalizedShadow);

		LOGGER.trace("Normalizing shadow: change description: {}", delta.debugDump());
		// prismContext.adopt(shadow);
		try {
			ConstraintsChecker.onShadowModifyOperation(delta.getModifications());
			getRepositoryService().modifyObject(ShadowType.class, oldShadow.getOid(), delta.getModifications(),
					parentResult);
		} catch (SchemaException ex) {
			parentResult.recordFatalError("Couldn't modify shadow: schema violation: " + ex.getMessage(), ex);
			throw ex;
		} catch (ObjectAlreadyExistsException ex) {
			parentResult.recordFatalError("Couldn't modify shadow: shadow already exists: " + ex.getMessage(), ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			parentResult.recordFatalError("Couldn't modify shadow: shadow not found: " + ex.getMessage(), ex);
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
		
		if (modifications == null){
			modifications =  new ArrayList<ItemDelta>();
		}
		
		return modifications;
		
		
	}
	
	
}
