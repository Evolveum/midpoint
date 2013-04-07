/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;


@Component
public class ShadowCacheReconciler extends ShadowCache{
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheReconciler.class);

	@Override
	public <T extends ShadowType> String afterAddOnResource(PrismObject<T> shadow, ResourceType resource, 
			ObjectClassComplexTypeDefinition objectClassDefinition, OperationResult parentResult)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		
		cleanShadowInRepository(shadow, parentResult);

		return shadow.getOid();
	}

	@Override
	public <T extends ShadowType> void afterModifyOnResource(PrismObject<T> shadow, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		LOGGER.trace("Modified shadow is reconciled. Start to clean up account after successfull reconciliation.");
		try{
		cleanShadowInRepository(shadow, parentResult);
		} catch (ObjectAlreadyExistsException ex){
			//should be never thrown
			throw new SystemException("While modifying object in the repository got exception: " + ex.getMessage(), ex);
		}
		LOGGER.trace("Shadow cleaned up successfully.");
		
	}
	
	private <T extends ShadowType> void cleanShadowInRepository(PrismObject<T> shadow, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException{
		PrismObject<T> oldShadow = shadow.clone();
		ShadowUtil.getAttributesContainer(oldShadow).clear();
		ShadowCacheUtil.normalizeShadow(shadow.asObjectable(), parentResult);

		// FIXME: ugly hack, need to be fixed (problem with comparing operation
		// result, because it was changed and in this call it is different as
		// one in repo, therefore the following if)
		PrismObject<ShadowType> repoShadow = getRepositoryService().getObject(ShadowType.class,
				shadow.getOid(), parentResult);
		ShadowType repoShadowType = repoShadow.asObjectable();
		if (repoShadowType.getResult() != null) {
			if (!repoShadowType.getResult().equals(oldShadow.asObjectable().getResult())) {
				oldShadow.asObjectable().setResult(repoShadowType.getResult());
			}
		}

		ObjectDelta delta = oldShadow.diff(shadow);

		LOGGER.trace("Normalizing shadow: change description: {}", delta.dump());
		// prismContext.adopt(shadow);
		try {
			getRepositoryService().modifyObject(ShadowType.class, shadow.getOid(), delta.getModifications(),
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
	public <T extends ShadowType> Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<T> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException {
		ObjectDeltaType shadowDelta = shadow.asObjectable().getObjectChange();
		
		//TODO: error handling
		if (shadowDelta != null){
		modifications = DeltaConvertor.toModifications(
				shadowDelta.getModification(), shadow.getDefinition());
		
		}
		
		if (modifications == null){
			modifications =  new ArrayList<ItemDelta>();
		}
		
		return modifications;
		
		
	}
	
	
}
