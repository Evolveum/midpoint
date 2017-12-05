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

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.prism.PrismObject.asPrismObject;
import static com.evolveum.midpoint.schema.util.LocalizationUtil.createLocalizableMessageType;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createDisplayInformation;
import static com.evolveum.midpoint.wf.impl.util.MiscDataUtil.getFocusObjectNewOrOld;

/**
 *
 * @author mederly
 */
@Component
public class PolicyRuleBasedAspect extends BasePrimaryChangeAspect {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleBasedAspect.class);

    public static final String USE_DEFAULT_NAME_MARKER = "#default#";

    @Autowired protected PrismContext prismContext;
	@Autowired private AssignmentPolicyAspectPart assignmentPolicyAspectPart;
	@Autowired private ObjectPolicyAspectPart objectPolicyAspectPart;
	@Autowired private ModelInteractionService modelInteractionService;

	//region ------------------------------------------------------------ Things that execute on request arrival

	@Override
	public boolean isEnabledByDefault() {
		return true;
	}

	@Override
	protected boolean isFirst() {
		return true;
	}

	@NotNull
	@Override
    public <T extends ObjectType> List<PcpChildWfTaskCreationInstruction<?>> prepareTasks(@NotNull ObjectTreeDeltas<T> objectTreeDeltas,
			ModelInvocationContext<T> ctx, @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {

		List<PcpChildWfTaskCreationInstruction<?>> instructions = new ArrayList<>();
		if (objectTreeDeltas.getFocusChange() != null) {
			PrismObject<UserType> requester = baseModelInvocationProcessingHelper.getRequester(ctx.taskFromModel, result);
			assignmentPolicyAspectPart.extractAssignmentBasedInstructions(objectTreeDeltas, requester, instructions, ctx, result);
			objectPolicyAspectPart.extractObjectBasedInstructions(objectTreeDeltas, requester, instructions, ctx, result);
		}
        return instructions;
    }

	List<EvaluatedPolicyRule> selectTriggeredApprovalActionRules(Collection<EvaluatedPolicyRule> rules) {
		return rules.stream()
					.filter(r -> r.isTriggered() && r.containsEnabledAction(ApprovalPolicyActionType.class))
					.collect(Collectors.toList());
	}

	ApprovalSchemaType getSchemaFromAction(@NotNull ApprovalPolicyActionType approvalAction) {
		// TODO approval process
		if (approvalAction.getApprovalSchema() != null) {
			return approvalAction.getApprovalSchema().clone();
		} else {
			ApprovalSchemaType rv = new ApprovalSchemaType(prismContext);
			ApprovalStageDefinitionType stageDef = new ApprovalStageDefinitionType(prismContext);
			stageDef.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(approvalAction.getApproverRef()));
			stageDef.getApproverRelation().addAll(approvalAction.getApproverRelation());
			stageDef.getApproverExpression().addAll(approvalAction.getApproverExpression());
			stageDef.setAutomaticallyApproved(approvalAction.getAutomaticallyApproved());
			// TODO maybe use name + description as well
			rv.getStage().add(stageDef);
			return rv;
		}
	}

	// evaluatedAssignment present only if relevant
	LocalizableMessage createProcessName(ApprovalSchemaBuilder.Result schemaBuilderResult,
			@Nullable EvaluatedAssignment<?> evaluatedAssignment, ModelInvocationContext<?> ctx, OperationResult result) {
		LocalizableMessage name = processNameFromApprovalActions(schemaBuilderResult, evaluatedAssignment, ctx, result);
		LOGGER.trace("Approval display name from approval actions: {}", name);
		if (name != null) {
			return name;
		}
		name = processNameFromTriggers(schemaBuilderResult);
		LOGGER.trace("Approval display name from triggers: {}", name);
		return name;
	}

	// corresponds with ConstraintEvaluationHelper.createExpressionVariables
	private LocalizableMessage processNameFromApprovalActions(ApprovalSchemaBuilder.Result schemaBuilderResult,
			@Nullable EvaluatedAssignment<?> evaluatedAssignment, ModelInvocationContext<?> ctx, OperationResult result) {
		if (schemaBuilderResult.approvalDisplayName == null) {
			return null;
		}
		Map<QName, Object> variables = new HashMap<>();
		variables.put(ExpressionConstants.VAR_OBJECT, getFocusObjectNewOrOld(ctx.modelContext));
		variables.put(ExpressionConstants.VAR_OBJECT_DISPLAY_INFORMATION, createLocalizableMessageType(createDisplayInformation(asPrismObject(getFocusObjectNewOrOld(ctx.modelContext)), false)));
		if (evaluatedAssignment != null) {
			variables.put(ExpressionConstants.VAR_TARGET, evaluatedAssignment.getTarget());
			variables.put(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION, createLocalizableMessageType(createDisplayInformation(evaluatedAssignment.getTarget(), false)));
			variables.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, evaluatedAssignment);
			variables.put(ExpressionConstants.VAR_ASSIGNMENT, evaluatedAssignment.getAssignmentType());
		} else {
			variables.put(ExpressionConstants.VAR_TARGET, null);
			variables.put(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION, null);
			variables.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, null);
			variables.put(ExpressionConstants.VAR_ASSIGNMENT, null);
		}
		LocalizableMessageType localizableMessageType;
		try {
			localizableMessageType = modelInteractionService
					.createLocalizableMessageType(schemaBuilderResult.approvalDisplayName, variables, ctx.taskFromModel, result);
		} catch (CommonException|RuntimeException e) {
			throw new SystemException("Couldn't create localizable message for approval display name: " + e.getMessage(), e);
		}
		return LocalizationUtil.toLocalizableMessage(localizableMessageType);
	}

	@Nullable
	private LocalizableMessage processNameFromTriggers(ApprovalSchemaBuilder.Result schemaBuilderResult) {
		List<EvaluatedPolicyRuleTriggerType> triggers = new ArrayList<>();

		// Let's analyze process specification - collect rules mentioned there.
		// Unlike in attachedRules, these are ordered in such a way that process-specific
		// are present first. (Not ordered according to composition rules.)
		if (schemaBuilderResult.processSpecification != null) {
			ProcessSpecifications.ProcessSpecification ps = schemaBuilderResult.processSpecification;
			// TODO take name from process specification itself (if present)
			for (Pair<ApprovalPolicyActionType, EvaluatedPolicyRule> actionWithRule : ps.actionsWithRules) {
				if (actionWithRule.getRight() != null) {
					for (EvaluatedPolicyRuleTrigger<?> trigger : actionWithRule.getRight().getAllTriggers()) {
						// we don't care about options; these converted triggers will be thrown away
						triggers.add(trigger.toEvaluatedPolicyRuleTriggerType(new PolicyRuleExternalizationOptions()));
					}
				}
			}
		} else {
			// For assignments we do not set processSpecification yet.
			// The triggers can be collected also from attached rules.
			for (SchemaAttachedPolicyRuleType entry : schemaBuilderResult.attachedRules.getEntry()) {
				triggers.addAll(entry.getRule().getTrigger());
			}
		}

		// now get the first message
		List<TreeNode<EvaluatedPolicyRuleTriggerType>> trees = EvaluatedPolicyRuleUtil.arrangeForPresentationExt(triggers);
		if (!trees.isEmpty() && trees.get(0).getUserObject().getShortMessage() != null) {
			return LocalizationUtil.toLocalizableMessage(trees.get(0).getUserObject().getShortMessage());
		} else {
			return null;
		}
	}

	protected boolean useDefaultProcessName(LocalizableMessage processName) {
		return LocalizableMessage.isEmpty(processName) ||
				processName instanceof SingleLocalizableMessage &&
						USE_DEFAULT_NAME_MARKER.equals(((SingleLocalizableMessage) processName).getKey());
	}
	//endregion
}