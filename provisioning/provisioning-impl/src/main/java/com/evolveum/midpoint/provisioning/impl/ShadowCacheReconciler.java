package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.util.ShadowCacheUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;


@Component
public class ShadowCacheReconciler extends ShadowCache{
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheReconciler.class);

	@Override
	public String afterAddOnResource(ResourceObjectShadowType shadowType, ResourceType resource, OperationResult parentResult)
			throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
		
		cleanShadowInRepository(shadowType, parentResult);

		return shadowType.getOid();
	}

	@Override
	public void afterModifyOnResource(ResourceObjectShadowType shadowType, Collection<? extends ItemDelta> modifications, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {
		LOGGER.trace("Modified shadow is reconciled. Start to clean up account after successfull reconciliation.");
		try{
		cleanShadowInRepository(shadowType, parentResult);
		} catch (ObjectAlreadyExistsException ex){
			//should be never thrown
			throw new SystemException("While modifying object in the repository got exception: " + ex.getMessage(), ex);
		}
		LOGGER.trace("Shadow cleaned up successfully.");
		
	}
	
	private void cleanShadowInRepository(ResourceObjectShadowType shadowType, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException{
	PrismObject oldShadow = shadowType.asPrismObject().clone();
	ResourceObjectShadowUtil.getAttributesContainer(oldShadow).clear();
	ShadowCacheUtil.normalizeShadow(shadowType, parentResult);

	ObjectDelta delta = oldShadow.diff(shadowType.asPrismObject());
	LOGGER.trace("Normalizing shadow: change description: {}", delta.dump());
//	prismContext.adopt(shadow);
	try {
		getRepositoryService().modifyObject(AccountShadowType.class, shadowType.getOid(), delta.getModifications(),
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
	public Collection<? extends ItemDelta> beforeModifyOnResource(ResourceObjectShadowType shadowType,
			ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException{
		ObjectDeltaType shadowDelta = shadowType.getObjectChange();
		
		//TODO: error handling
		if (shadowDelta != null){
		modifications = DeltaConvertor.toModifications(
				shadowDelta.getModification(), shadowType.asPrismObject().getDefinition());
		
		}
		
		if (modifications == null){
			modifications =  new ArrayList<ItemDelta>();
		}
		
		return modifications;
		
		
	}
	
	
}
