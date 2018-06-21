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

import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class ShadowCacheProvisioner extends ShadowCache {
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheProvisioner.class);

	@Override
	public String afterAddOnResource(
			ProvisioningContext ctx, 
			PrismObject<ShadowType> shadowToAdd, 
			ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
			OperationResult parentResult)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException, EncryptionException {
	
		return shadowManager.addNewActiveRepositoryShadow(ctx, shadowToAdd, opState, parentResult);
	}

	@Override
	public void afterModifyOnResource(
			ProvisioningContext ctx,
			PrismObject<ShadowType> shadow,
			Collection<? extends ItemDelta> modifications,
			ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
			XMLGregorianCalendar now,
			OperationResult parentResult)
					throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException, EncryptionException {
		shadowManager.modifyShadow(ctx, shadow, modifications, opState, now, parentResult);
	}
	
	@Override
	public Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException {
		
		// TODO: error handling
		//do not merge deltas when complete postponed operation is set to false, because it can cause some unexpected behavior..
		if (!ProvisioningOperationOptions.isCompletePostponed(options)) {
			return modifications;
		}
		
		ObjectDelta mergedDelta = mergeDeltas(shadow, modifications);

		if (mergedDelta != null) {
			modifications = mergedDelta.getModifications();
		}
		
		return modifications;
		
	}

}
