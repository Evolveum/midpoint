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
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.WfExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.tasks.ProcessSpecificContent;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author mederly
 */
public class ItemApprovalSpecificContent implements ProcessSpecificContent {

	private static final transient Trace LOGGER = TraceManager.getTrace(ItemApprovalSpecificContent.class);

	@NotNull private final PrismContext prismContext;
	private final String taskName;
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

	// skippability because of no approvers was already tested; see ApprovalSchemaHelper.shouldBeSkipped
	@Override
	public boolean checkEmpty(PcpChildWfTaskCreationInstruction instruction,
			WfExpressionEvaluationHelper evaluationHelper, ModelInvocationContext ctx, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		List<ApprovalStageDefinitionType> stages = WfContextUtil.getStages(approvalSchemaType);
		// first pass: if there is any stage that is obviously not skippable, let's return false without checking the expressions
		for (ApprovalStageDefinitionType stage : stages) {
			if (stage.getAutomaticallyCompleted() == null) {
				return false;
			}
		}
		// second pass: check the conditions
		for (ApprovalStageDefinitionType stage : stages) {
			if (!SchemaConstants.MODEL_APPROVAL_OUTCOME_SKIP.equals(
					evaluateAutoCompleteExpression(stage, instruction, evaluationHelper, ctx, result))) {
				return false;
			}
		}
		return true;
	}

	private String evaluateAutoCompleteExpression(ApprovalStageDefinitionType stageDef, PcpChildWfTaskCreationInstruction instruction,
			WfExpressionEvaluationHelper evaluationHelper, ModelInvocationContext ctx, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {
		ExpressionType expression = stageDef.getAutomaticallyCompleted();
		ExpressionVariables variables = evaluationHelper.getDefaultVariables(instruction.getWfContext(), result);
		List<String> outcomes = evaluationHelper.evaluateExpression(expression, variables,
				"automatic completion expression", String.class,
				DOMUtil.XSD_STRING, WfExpressionEvaluationHelper.createOutcomeConvertor(), ctx.taskFromModel, result);
		LOGGER.trace("Pre-completed = {} for stage {}", outcomes, stageDef);
		Set<String> distinctOutcomes = new HashSet<>(outcomes);
		if (distinctOutcomes.isEmpty()) {
			return null;
		} else if (distinctOutcomes.size() == 1) {
			return distinctOutcomes.iterator().next();
		} else {
			throw new IllegalStateException("Ambiguous result from 'automatically completed' expression: " + distinctOutcomes);
		}
	}
}
