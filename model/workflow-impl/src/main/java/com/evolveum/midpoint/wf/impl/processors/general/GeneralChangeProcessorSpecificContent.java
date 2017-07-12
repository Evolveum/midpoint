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

package com.evolveum.midpoint.wf.impl.processors.general;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.tasks.ProcessorSpecificContent;
import com.evolveum.midpoint.wf.impl.util.JaxbValueContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessorSpecificStateType;

import java.util.Map;

/**
 * @author mederly
 */
public class GeneralChangeProcessorSpecificContent implements ProcessorSpecificContent {

	private String scenarioBeanName;
	private LensContext<?> modelContext;

	public GeneralChangeProcessorSpecificContent(LensContext<?> context) {
		this.modelContext = context;
	}

	public void setScenarioBeanName(String scenarioBeanName) {
		this.scenarioBeanName = scenarioBeanName;
	}

	@Override public WfProcessorSpecificStateType createProcessorSpecificState() {
		return null;
	}

	@Override
	public void createProcessVariables(Map<String, Object> map, PrismContext prismContext) throws SchemaException {
		if (scenarioBeanName != null) {
			map.put(GcpProcessVariableNames.VARIABLE_MIDPOINT_SCENARIO_BEAN_NAME, scenarioBeanName);
		}
		if (modelContext != null) {
			map.put(GcpProcessVariableNames.VARIABLE_MODEL_CONTEXT, new JaxbValueContainer<>(modelContext.toLensContextType(), prismContext));
		}
	}
}
