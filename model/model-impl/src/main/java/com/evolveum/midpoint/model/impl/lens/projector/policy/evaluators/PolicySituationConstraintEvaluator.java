/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedSituationTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.projector.policy.AssignmentPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicySituationPolicyConstraintType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBElement;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PolicySituationConstraintEvaluator implements PolicyConstraintEvaluator<PolicySituationPolicyConstraintType> {

    private static final String OP_EVALUATE = PolicySituationConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY = "situation";

    @Autowired private ConstraintEvaluatorHelper evaluatorHelper;

    @Override
    public <AH extends AssignmentHolderType> EvaluatedSituationTrigger evaluate(@NotNull JAXBElement<PolicySituationPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<AH> rctx, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            // For assignments we consider only directly attached "situation" policy rules. In the future, we might configure this.
            // So, if someone wants to report (forward) triggers from a target, he must ensure that a particular
            // "situation" constraint is present directly on it.
            if (rctx instanceof AssignmentPolicyRuleEvaluationContext
                    && !((AssignmentPolicyRuleEvaluationContext) rctx).isDirect) {
                return null;
            }

            // Single pass only (for the time being)
            PolicySituationPolicyConstraintType situationConstraint = constraint.getValue();
            Collection<EvaluatedPolicyRule> sourceRules =
                    selectTriggeredRules(rctx, situationConstraint.getSituation());
            if (sourceRules.isEmpty()) {
                return null;
            }
            return new EvaluatedSituationTrigger(situationConstraint,
                    createMessage(sourceRules, constraint, rctx, result),
                    createShortMessage(sourceRules, constraint, rctx, result),
                    sourceRules);
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private LocalizableMessage createMessage(Collection<EvaluatedPolicyRule> sourceRules,
            JAXBElement<PolicySituationPolicyConstraintType>  constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        // determine if there's a single message that could be retrieved
        List<TreeNode<LocalizableMessage>> messageTrees = sourceRules.stream()
                .flatMap(r -> r.extractMessages().stream())
                .collect(Collectors.toList());
        LocalizableMessage builtInMessage;
        if (messageTrees.size() == 1) {
            builtInMessage = messageTrees.get(0).getUserObject();
        } else {
            builtInMessage = new LocalizableMessageBuilder()
                    .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY)
                    .build();
        }
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    private LocalizableMessage createShortMessage(Collection<EvaluatedPolicyRule> sourceRules,
            JAXBElement<PolicySituationPolicyConstraintType> constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        // determine if there's a single message that could be retrieved
        List<TreeNode<LocalizableMessage>> messageTrees = sourceRules.stream()
                .flatMap(r -> r.extractShortMessages().stream())
                .collect(Collectors.toList());
        LocalizableMessage builtInMessage;
        if (messageTrees.size() == 1) {
            builtInMessage = messageTrees.get(0).getUserObject();
        } else {
            builtInMessage = new LocalizableMessageBuilder()
                    .key(SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY)
                    .build();
        }
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }


    private <AH extends AssignmentHolderType> Collection<EvaluatedPolicyRule> selectTriggeredRules(
            PolicyRuleEvaluationContext<AH> rctx, List<String> situations) {
        Collection<EvaluatedPolicyRule> rules;
        if (rctx instanceof AssignmentPolicyRuleEvaluationContext) {
            EvaluatedAssignmentImpl<AH> evaluatedAssignment = ((AssignmentPolicyRuleEvaluationContext<AH>) rctx).evaluatedAssignment;
            // We consider all rules here, i.e. also those that are triggered on targets induced by this one.
            // Decision whether to trigger such rules lies on "primary" policy constraints. (E.g. approvals would
            // not trigger, whereas exclusions probably would.) Overall, our responsibility is simply to collect
            // all triggered rules.
            rules = evaluatedAssignment.getAllTargetsPolicyRules();
        } else {
            rules = rctx.focusContext.getPolicyRules();
        }
        return rules.stream()
                .filter(r -> r.isTriggered() && situations.contains(r.getPolicySituation()))
                .collect(Collectors.toList());
    }
}
