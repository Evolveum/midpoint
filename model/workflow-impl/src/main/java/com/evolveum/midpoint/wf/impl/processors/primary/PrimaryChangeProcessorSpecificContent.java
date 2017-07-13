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

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.tasks.ProcessorSpecificContent;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfPrimaryChangeProcessorStateType;

import java.util.Map;

/**
 * @author mederly
 */
public class PrimaryChangeProcessorSpecificContent implements ProcessorSpecificContent {

	private final WfPrimaryChangeProcessorStateType processorState;
	private boolean executeApprovedChangeImmediately;     // should the child job execute approved change immediately (i.e. executeModelOperationHandler must be set as well!)

	public PrimaryChangeProcessorSpecificContent(PrismContext prismContext) {
		processorState = new WfPrimaryChangeProcessorStateType(prismContext);
	}

	public WfPrimaryChangeProcessorStateType createProcessorSpecificState() {
		return processorState;
	}

	public boolean isExecuteApprovedChangeImmediately() {
		return executeApprovedChangeImmediately;
	}

	public void setExecuteApprovedChangeImmediately(boolean executeApprovedChangeImmediately) {
		this.executeApprovedChangeImmediately = executeApprovedChangeImmediately;
	}

	@Override public void createProcessVariables(Map<String, Object> map, PrismContext prismContext) throws SchemaException {
		map.put(PcpProcessVariableNames.VARIABLE_CHANGE_ASPECT, processorState.getChangeAspect());
	}
}
