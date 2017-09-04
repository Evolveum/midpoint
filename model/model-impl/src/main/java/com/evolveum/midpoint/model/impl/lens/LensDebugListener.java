/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.common.mapping.Mapping;
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
public interface LensDebugListener {

	public <F extends ObjectType> void beforeSync(LensContext<F> context);

	public <F extends ObjectType> void afterSync(LensContext<F> context);

	public <F extends ObjectType> void beforeProjection(LensContext<F> context);

	public <F extends ObjectType> void afterProjection(LensContext<F> context);

	/**
	 * May be used to gather profiling data, etc.
	 */
	public <F extends ObjectType> void afterMappingEvaluation(LensContext<F> context, Mapping<?,?> evaluatedMapping);

//	/**
//	 * For all scripts expect for mappings.
//	 * May be used to gather profiling data, etc.
//	 */
//	public <F extends ObjectType, P extends ObjectType> void afterScriptEvaluation(LensContext<F,P> context, ScriptExpression scriptExpression);
}
