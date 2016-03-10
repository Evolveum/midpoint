/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.wf.impl.jobs.ProcessInstruction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemApprovalProcessStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessSpecificStateType;

import java.util.Map;

/**
 * @author mederly
 */
public class ItemApprovalInstruction implements ProcessInstruction {

	private String taskName;
	private ApprovalSchema approvalSchema;

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskName() {
		return taskName;
	}

	@Override public void createProcessVariables(Map<String, Object> map, PrismContext prismContext) {
		map.put(ProcessVariableNames.APPROVAL_TASK_NAME, taskName);
		if (approvalSchema != null) {
			map.put(ProcessVariableNames.APPROVAL_SCHEMA, approvalSchema);
		}
	}

	public void setApprovalSchema(ApprovalSchema approvalSchema) {
		this.approvalSchema = approvalSchema;
	}

	public ApprovalSchema getApprovalSchema() {
		return approvalSchema;
	}

	@Override
	public WfProcessSpecificStateType createProcessSpecificState() {
		ItemApprovalProcessStateType state = new ItemApprovalProcessStateType();
		state.asPrismContainerValue().setConcreteType(ItemApprovalProcessStateType.COMPLEX_TYPE);
		state.setApprovalSchema(approvalSchema != null ? approvalSchema.toApprovalSchemaType() : null);
		return state;
	}
}
