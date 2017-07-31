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

package com.evolveum.midpoint.provisioning.impl;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CachingStategyType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

@Component
public class ShadowCacheProvisioner extends ShadowCache {
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheProvisioner.class);
	
	@Override
	public String afterAddOnResource(ProvisioningContext ctx, String existingShadowOid, AsynchronousOperationReturnValue<PrismObject<ShadowType>> addResult, OperationResult parentResult)
					throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
	
		return shadowManager.addNewActiveRepositoryShadow(ctx, existingShadowOid, addResult, parentResult);
	}

	@Override
	public void afterModifyOnResource(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications, 
			OperationResult resourceOperationResult, OperationResult parentResult) throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
		shadowManager.modifyShadow(ctx, shadow, modifications, resourceOperationResult, parentResult);
	}
	
	@Override
	public Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException {
		
		// TODO: error handling
		//do not merge deltas when complete postponed operation is set to false, because it can cause some unexpected behavior..
		if (!ProvisioningOperationOptions.isCompletePostponed(options)){
			return modifications;
		}
		
		ObjectDelta mergedDelta = mergeDeltas(shadow, modifications);

		if (mergedDelta != null) {
			modifications = mergedDelta.getModifications();
		}
		
		return modifications;
		
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private ObjectDelta mergeDeltas(PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications)
			throws SchemaException {
		ShadowType shadowType = shadow.asObjectable();
		if (shadowType.getObjectChange() != null) {

			ObjectDeltaType deltaType = shadowType.getObjectChange();
			Collection<? extends ItemDelta> pendingModifications = DeltaConvertor.toModifications(
					deltaType.getItemDelta(), shadow.getDefinition());

            // pendingModifications must come before modifications, otherwise REPLACE of value X (pending),
            // followed by ADD of value Y (current) would become "REPLACE X", which is obviously wrong.
            // See e.g. MID-1709.
			return ObjectDelta.summarize(
                    ObjectDelta.createModifyDelta(shadow.getOid(), pendingModifications,
                            ShadowType.class, getPrismContext()),
                    ObjectDelta.createModifyDelta(shadow.getOid(), modifications,
                            ShadowType.class, getPrismContext()));
		}
		return null;
	}

}
