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

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.util.DebugUtil;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
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


public class ShadowCacheReconciler extends ShadowCache {
	
	private static final Trace LOGGER = TraceManager.getTrace(ShadowCacheReconciler.class);

	@Override
	public Collection<? extends ItemDelta> beforeModifyOnResource(PrismObject<ShadowType> shadow, ProvisioningOperationOptions options, Collection<? extends ItemDelta> modifications) throws SchemaException {
		ObjectDeltaType shadowDelta = shadow.asObjectable().getObjectChange();
		
		//TODO: error handling
		if (shadowDelta != null) {
			modifications = DeltaConvertor.toModifications(
				shadowDelta.getItemDelta(), shadow.getDefinition());
		
		}
		
		// for the older versions
		ObjectDelta<? extends ObjectType> objectDelta = ObjectDelta.createModifyDelta(shadow.getOid(),
				modifications, ShadowType.class, getPrismContext());
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Storing delta to shadow:\n{}", objectDelta.debugDump(1));
		}
		
		ContainerDelta<ShadowAssociationType> associationDelta = objectDelta.findContainerDelta(ShadowType.F_ASSOCIATION);
		if (associationDelta != null) {
			normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToAdd());
			normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToReplace());
			normalizeAssociationDeltasBeforeSave(associationDelta.getValuesToDelete());
			
		}
		
		ObjectDelta mergedDelta = mergeDeltas(shadow, modifications);

		if (mergedDelta != null) {
			modifications = mergedDelta.getModifications();
		}
		
		if (modifications == null) {
			modifications = new ArrayList<>();
		}
		
		return modifications;
	}
	
}
