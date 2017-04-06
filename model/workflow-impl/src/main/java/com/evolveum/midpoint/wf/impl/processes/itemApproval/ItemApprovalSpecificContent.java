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
import com.evolveum.midpoint.wf.impl.tasks.ProcessSpecificContent;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemApprovalProcessStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaAttachedPolicyRulesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfProcessSpecificStateType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author mederly
 */
public class ItemApprovalSpecificContent implements ProcessSpecificContent {

	@NotNull private final PrismContext prismContext;
	final String taskName;
	@NotNull final ApprovalSchemaType approvalSchemaType;
	@Nullable final SchemaAttachedPolicyRulesType policyRules;

	public ItemApprovalSpecificContent(@NotNull PrismContext prismContext, String taskName,
			@NotNull ApprovalSchemaType approvalSchemaType, @Nullable SchemaAttachedPolicyRulesType policyRules) {
		this.prismContext = prismContext;
		this.taskName = taskName;
		this.approvalSchemaType = approvalSchemaType;
		this.policyRules = policyRules;
	}

	@Override public void createProcessVariables(Map<String, Object> map, PrismContext prismContext) {
		map.put(ProcessVariableNames.APPROVAL_TASK_NAME, taskName);
		map.put(ProcessVariableNames.APPROVAL_STAGES, createStages(approvalSchemaType));
	}

	private List<Integer> createStages(ApprovalSchemaType schema) {
		return IntStream.range(1, schema.getStage().size()+1).boxed().collect(Collectors.toList());
	}

	@Override
	public WfProcessSpecificStateType createProcessSpecificState() {
		ItemApprovalProcessStateType state = new ItemApprovalProcessStateType(prismContext);
		state.setApprovalSchema(approvalSchemaType);
		state.setPolicyRules(policyRules);
		return state;
	}
}
