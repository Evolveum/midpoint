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

package com.evolveum.midpoint.model.intest.util;

import java.util.Collection;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.ClockworkInspector;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.lang3.ObjectUtils;

public class ProfilingClockworkInspector implements ClockworkInspector {

	protected static final Trace LOGGER = TraceManager.getTrace(ProfilingClockworkInspector.class);

	private long projectorStartTime = 0;
	private long projectorEndTime = 0;
	private long mappingTotalMillis = 0;
	private long projectorMappingTotalMillis = 0;
	private long projectorMappingTotalCount = 0;
	private LensContext lastLensContext;

	@Override
	public <F extends ObjectType> void clockworkStart(LensContext<F> context) {
		// TODO Auto-generated method stub

	}

	@Override
	public <F extends ObjectType> void clockworkFinish(LensContext<F> context) {
		// TODO Auto-generated method stub

	}

	@Override
	public <F extends ObjectType> void projectorStart(LensContext<F> context) {
		projectorStartTime = System.currentTimeMillis();
		projectorMappingTotalMillis = 0;
		projectorMappingTotalCount = 0;
	}

	@Override
	public <F extends ObjectType> void projectorFinish(LensContext<F> context) {
		projectorEndTime = System.currentTimeMillis();
		String desc = null;
		if (context.getFocusContext() != null) {
			PrismObject<F> focusObject = context.getFocusContext().getObjectNew();
			if (focusObject == null) {
				context.getFocusContext().getObjectOld();
			}
			if (focusObject != null) {
				desc = focusObject.toString();
			}
		} else {
			for (LensProjectionContext projectionContext: context.getProjectionContexts()) {
				PrismObject<ShadowType> projObj = projectionContext.getObjectNew();
				if (projObj == null) {
					projObj = projectionContext.getObjectOld();
				}
				if (projObj != null) {
					desc = projObj.toString();
					break;
				}
			}
		}
		int changes = 0;
		Collection<ObjectDelta<? extends ObjectType>> allDeltas = null;
		try {
			allDeltas = context.getAllChanges();
		} catch (SchemaException e) {
			changes = -1;
		}
		if (allDeltas != null) {
			changes = allDeltas.size();
		}
		long projectorEtime = projectorEndTime - projectorStartTime;
		LOGGER.trace("Projector finished ({}), {} changes, etime: {} ms ({} mapping evaluated, {} ms total)",
				new Object[]{desc, changes, projectorEtime, projectorMappingTotalCount, projectorMappingTotalMillis});

		lastLensContext = context;
	}

	public <F extends ObjectType> LensContext<F> getLastLensContext() {
		return lastLensContext;
	}

	@Override
	public <F extends ObjectType> void afterMappingEvaluation(LensContext<F> context, Mapping<?,?> evaluatedMapping) {
		mappingTotalMillis += ObjectUtils.defaultIfNull(evaluatedMapping.getEtime(), 0L);
		projectorMappingTotalMillis += ObjectUtils.defaultIfNull(evaluatedMapping.getEtime(), 0L);
		projectorMappingTotalCount++;
	}

	@Override
	public <F extends ObjectType> void clockworkStateSwitch(LensContext<F> contextBefore,
			ModelState newState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void projectorComponentSkip(String componentName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void projectorComponentStart(String componentName) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void projectorComponentFinish(String componentName) {
		// TODO Auto-generated method stub
		
	}

}
