/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.policy;

import static com.evolveum.midpoint.prism.PrismObject.asPrismObject;
import static com.evolveum.midpoint.schema.util.LocalizationUtil.createLocalizableMessageType;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createDisplayInformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.AssociatedPolicyRule;
import com.evolveum.midpoint.model.api.context.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpStartInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.processors.primary.policy.ProcessSpecifications.ApprovalActionWithRule;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class PolicyRuleBasedAspect extends BasePrimaryChangeAspect {

    @SuppressWarnings("unused")
    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleBasedAspect.class);

    private static final String USE_DEFAULT_NAME_MARKER = "#default#";

    private static final String OP_GET_START_INSTRUCTIONS = PolicyRuleBasedAspect.class.getName() + ".getStartInstructions";

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
    public <T extends ObjectType> List<PcpStartInstruction> getStartInstructions(
            @NotNull ObjectTreeDeltas<T> objectTreeDeltas,
            @NotNull ModelInvocationContext<T> ctx,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {
        OperationResult result = parentResult.subresult(OP_GET_START_INSTRUCTIONS)
                .setMinor()
                .build();
        try {
            List<PcpStartInstruction> instructions = new ArrayList<>();
            if (objectTreeDeltas.getFocusChange() != null) {
                PrismObject<? extends FocusType> requester = ctx.getRequestor(result);
                assignmentPolicyAspectPart.extractAssignmentBasedInstructions(
                        objectTreeDeltas, requester, instructions, ctx, result);
                objectPolicyAspectPart.extractObjectBasedInstructions(objectTreeDeltas, requester, instructions, ctx, result);
            }
            result.addParam("instructionsCount", instructions.size());
            return instructions;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    List<AssociatedPolicyRule> selectTriggeredApprovalActionRules(Collection<? extends AssociatedPolicyRule> rules) {
        return rules.stream()
                .filter(r -> r.isTriggered() && r.containsEnabledAction(ApprovalPolicyActionType.class))
                .collect(Collectors.toList());
    }

    ApprovalSchemaType getSchemaFromAction(@NotNull ApprovalPolicyActionType approvalAction) {
        // TODO approval process
        if (approvalAction.getApprovalSchema() != null) {
            return approvalAction.getApprovalSchema().clone();
        } else {
            ApprovalSchemaType rv = new ApprovalSchemaType();
            ApprovalStageDefinitionType stageDef = new ApprovalStageDefinitionType();
            stageDef.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(approvalAction.getApproverRef()));
            stageDef.getApproverRelation().addAll(approvalAction.getApproverRelation());
            stageDef.getApproverExpression().addAll(approvalAction.getApproverExpression());
            // TODO maybe use name + description as well
            rv.getStage().add(stageDef);
            return rv;
        }
    }

    // evaluatedAssignment present only if relevant
    LocalizableMessage createProcessName(ApprovalSchemaBuilder.Result schemaBuilderResult,
            @Nullable EvaluatedAssignment evaluatedAssignment, ModelInvocationContext<?> ctx, OperationResult result) {
        LocalizableMessage name = processNameFromApprovalActions(schemaBuilderResult, evaluatedAssignment, ctx, result);
        LOGGER.trace("Approval display name from approval actions: {}", name);
        if (name != null) {
            return name;
        }
        name = caseNameFromTriggers(schemaBuilderResult);
        LOGGER.trace("Approval display name from triggers: {}", name);
        return name;
    }

    // corresponds with ConstraintEvaluationHelper.createVariablesMap
    private LocalizableMessage processNameFromApprovalActions(ApprovalSchemaBuilder.Result schemaBuilderResult,
            @Nullable EvaluatedAssignment evaluatedAssignment, ModelInvocationContext<?> ctx, OperationResult result) {
        if (schemaBuilderResult.approvalDisplayName == null) {
            return null;
        }
        VariablesMap variables = new VariablesMap();
        ObjectType focusType = ctx.getFocusObjectNewOrOld();
        variables.put(ExpressionConstants.VAR_OBJECT, focusType, focusType.asPrismObject().getDefinition());
        variables.put(ExpressionConstants.VAR_OBJECT_DISPLAY_INFORMATION,
                createLocalizableMessageType(createDisplayInformation(asPrismObject(focusType), false)),
                LocalizableMessageType.class);
        if (evaluatedAssignment != null) {
            variables.put(ExpressionConstants.VAR_TARGET, evaluatedAssignment.getTarget(), evaluatedAssignment.getTarget().getDefinition());
            variables.put(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION,
                    createLocalizableMessageType(createDisplayInformation(evaluatedAssignment.getTarget(), false)),
                    LocalizableMessageType.class);
            variables.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, evaluatedAssignment, EvaluatedAssignment.class);
            // Wrong ... but this will get reworked in 4.0 anyway.
            variables.put(ExpressionConstants.VAR_ASSIGNMENT, evaluatedAssignment.getAssignment(), AssignmentType.class);
        } else {
            // Wrong ... but this will get reworked in 4.0 anyway.
            variables.put(ExpressionConstants.VAR_TARGET, null, ObjectType.class);
            variables.put(ExpressionConstants.VAR_TARGET_DISPLAY_INFORMATION, null, LocalizableMessageType.class);
            variables.put(ExpressionConstants.VAR_EVALUATED_ASSIGNMENT, null, EvaluatedAssignment.class);
            // Wrong ... but this will get reworked in 4.0 anyway.
            variables.put(ExpressionConstants.VAR_ASSIGNMENT, null, AssignmentType.class);
        }
        LocalizableMessageType localizableMessageType;
        try {
            localizableMessageType = modelInteractionService
                    .createLocalizableMessageType(schemaBuilderResult.approvalDisplayName, variables, ctx.task, result);
        } catch (CommonException | RuntimeException e) {
            throw new SystemException("Couldn't create localizable message for approval display name: " + e.getMessage(), e);
        }
        return LocalizationUtil.toLocalizableMessage(localizableMessageType);
    }

    @Nullable
    private LocalizableMessage caseNameFromTriggers(ApprovalSchemaBuilder.Result schemaBuilderResult) {
        List<EvaluatedPolicyRuleTriggerType> triggers = new ArrayList<>();

        // Let's analyze process specification - collect rules mentioned there.
        // Unlike in attachedRules, these are ordered in such a way that process-specific
        // are present first. (Not ordered according to composition rules.)
        ProcessSpecifications.ProcessSpecification ps = schemaBuilderResult.processSpecification;
        if (ps != null) {
            // TODO take name from process specification itself (if present)
            for (ApprovalActionWithRule actionWithRule : ps.actionsWithRules) {
                AssociatedPolicyRule rule = actionWithRule.policyRule();
                // We can take all (i.e. also irrelevant) triggers here, as the conversion to bean will select the relevant ones.
                for (EvaluatedPolicyRuleTrigger<?> trigger : rule.getEvaluatedPolicyRule().getAllTriggers()) {
                    // we don't care about options; these converted triggers will be thrown away - only messages are collected
                    triggers.add(
                            trigger.toEvaluatedPolicyRuleTriggerBean(
                                    new PolicyRuleExternalizationOptions(), rule.getNewOwner()));
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

    boolean useDefaultProcessName(LocalizableMessage processName) {
        return LocalizableMessage.isEmpty(processName) ||
                processName instanceof SingleLocalizableMessage &&
                        USE_DEFAULT_NAME_MARKER.equals(((SingleLocalizableMessage) processName).getKey());
    }
    //endregion
}
