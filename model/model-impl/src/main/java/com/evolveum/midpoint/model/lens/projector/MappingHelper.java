/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.lens.projector;

import java.util.Collection;
import java.util.HashSet;

import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.mapping.Mapping;
import com.evolveum.midpoint.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingStrengthType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author Radovan Semancik
 *
 */
@Component
public class MappingHelper {
	
	private static final Trace LOGGER = TraceManager.getTrace(MappingHelper.class);
	
	@Autowired(required = true)
    private PrismContext prismContext;

    @Autowired(required = true)
    private MappingFactory valueConstructionFactory;
	
	public <V extends PrismValue> PrismValueDeltaSetTriple<V> evaluateMappingSetProjection(Collection<MappingType> mappingTypes, String mappingDesc,
			XMLGregorianCalendar now, MappingClosures<V> closures, ItemDelta<V> aPrioriDelta,
			LensContext<UserType,ShadowType> context, LensProjectionContext<ShadowType> accCtx, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		
		PrismValueDeltaSetTriple<V> outputTriple = null;
		Collection<XMLGregorianCalendar> recomputeTimes = new HashSet<XMLGregorianCalendar>();
		
		for (MappingType mappingType: mappingTypes) {
			
			Mapping<V> mapping = valueConstructionFactory.createMapping(mappingType, mappingDesc);
			
			if (!mapping.isApplicableToChannel(context.getChannel())) {
	        	continue;
	        }
			
			// TODO: WEAK and aPrioriValue
			
			if (mapping.getStrength() != MappingStrengthType.STRONG) {
	        	if (aPrioriDelta != null && !aPrioriDelta.isEmpty()) {
	        		continue;
	        	}
	        }
			
			mapping.setNow(now);
			
			// Initialize mapping (using Inversion of Control)
			closures.initialize(mapping);
			
			if (!closures.willEvaluate(mapping)) {
				continue;
			}
			
			mapping.evaluate(result);
			
			PrismValueDeltaSetTriple<V> mappingOutputTriple = mapping.getOutputTriple();
			if (mappingOutputTriple != null) {
				if (outputTriple == null) {
					outputTriple = mappingOutputTriple;
				} else {
					outputTriple.merge(mappingOutputTriple);
				}
			}
			
			XMLGregorianCalendar nextRecomputeTime = mapping.getNextRecomputeTime();
			if (nextRecomputeTime != null) {
				recomputeTimes.add(nextRecomputeTime);
			}
		}
		
		if (!recomputeTimes.isEmpty()) {
			for (XMLGregorianCalendar recomputeTime: recomputeTimes) {
				PrismObjectDefinition<ShadowType> objectDefinition = accCtx.getObjectDefinition();
				PrismContainerDefinition<TriggerType> triggerContDef = objectDefinition.findContainerDefinition(ObjectType.F_TRIGGER);
				ContainerDelta<TriggerType> triggerDelta = triggerContDef.createEmptyDelta(new ItemPath(ObjectType.F_TRIGGER));
				PrismContainerValue<TriggerType> triggerCVal = triggerContDef.createValue();
				triggerDelta.addValueToAdd(triggerCVal);
				TriggerType triggerType = triggerCVal.asContainerable();
				triggerType.setTimestamp(recomputeTime);
				triggerType.setHandlerUri(RecomputeTriggerHandler.HANDLER_URI);
				
				// TODO: check for existing triggers
	
				accCtx.addToSecondaryDelta(triggerDelta);
			}
			
		}		
		
		return outputTriple;
	}

}
