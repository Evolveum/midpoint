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
package com.evolveum.midpoint.model.impl.util.mock;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.common.mapping.Mapping;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.ClockworkInspector;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class MockLensDebugListener implements ClockworkInspector {

	private static final Trace LOGGER = TraceManager.getTrace(MockLensDebugListener.class);

	private static final String SEPARATOR = "############################################################################";

	private LensContext lastSyncContext;

	public <F extends ObjectType>  LensContext<F> getLastSyncContext() {
		return lastSyncContext;
	}

	public <F extends ObjectType> void setLastSyncContext(LensContext<F> lastSyncContext) {
		this.lastSyncContext = lastSyncContext;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.lens.LensDebugListener#beforeSync(com.evolveum.midpoint.model.lens.LensContext)
	 */
	@Override
	public <F extends ObjectType> void clockworkStart(LensContext<F> context) {
		LOGGER.trace(SEPARATOR+"\nSYNC CONTEXT BEFORE SYNC\n{}\n"+SEPARATOR, context.debugDump());
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.model.lens.LensDebugListener#afterSync(com.evolveum.midpoint.model.lens.LensContext)
	 */
	@Override
	public <F extends ObjectType> void clockworkFinish(LensContext<F> context) {
		LOGGER.trace(SEPARATOR+"\nSYNC CONTEXT AFTER SYNC\n{}\n"+SEPARATOR, context.debugDump());
		lastSyncContext = context;
	}

	@Override
	public <F extends ObjectType> void projectorStart(
			LensContext<F> context) {

	}

	@Override
	public <F extends ObjectType> void projectorFinish(
			LensContext<F> context) {

	}

	@Override
	public <F extends ObjectType> void afterMappingEvaluation(
			LensContext<F> context,
			Mapping<?,?> evaluatedMapping) {

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
