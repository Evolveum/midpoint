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
package com.evolveum.midpoint.model.api.util;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Interface used to intercept the SyncContext as it passes through the computation.
 *
 * It is mostly used in tests.
 *
 * EXPERIMENTAL
 *
 * @author Radovan Semancik
 *
 */
public interface ClockworkInspector {

	<F extends ObjectType> void clockworkStart(ModelContext<F> context);
	
	<F extends ObjectType> void clockworkStateSwitch(ModelContext<F> contextBefore, ModelState newState);

	<F extends ObjectType> void clockworkFinish(ModelContext<F> context);

	<F extends ObjectType> void projectorStart(ModelContext<F> context);
	
	void projectorComponentSkip(String componentName);
	
	void projectorComponentStart(String componentName);
	
	void projectorComponentFinish(String componentName);

	<F extends ObjectType> void projectorFinish(ModelContext<F> context);

	/**
	 * May be used to gather profiling data, etc.
	 */
	public <F extends ObjectType> void afterMappingEvaluation(ModelContext<F> context, Mapping<?,?> evaluatedMapping);

//	/**
//	 * For all scripts expect for mappings.
//	 * May be used to gather profiling data, etc.
//	 */
//	public <F extends ObjectType, P extends ObjectType> void afterScriptEvaluation(LensContext<F,P> context, ScriptExpression scriptExpression);
}
