/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.isPreviewPolicyRulesEnforcement;
import static com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil.MessageKind.NORMAL;
import static com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil.extractMessages;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Code used to enforce the policy rules that have the `enforce` and `suspendTask` actions.
 *
 * An interesting difference is the execution in the preview mode: the former are only simulated, whereas the latter
 * do really throw an exception. This behavior may change in the future.
 *
 * Originally this was a regular {@link ChangeHook}. However, when invoked among other hooks, it is too late (see MID-4797).
 * So we had to convert it into regular code and run it right after the first {@link Projector} run.
 *
 * Intentionally package-private.
 *
 * @author semancik
 */
class PolicyRuleEnforcer<O extends ObjectType> {

    @NotNull private final LensContext<O> context;
    @NotNull private final List<LocalizableMessage> messages = new ArrayList<>();
    @NotNull private final List<EvaluatedPolicyRuleType> rulesBeans = new ArrayList<>();

    PolicyRuleEnforcer(@NotNull LensContext<O> context) {
        this.context = context;
    }

    public void enforce(OperationResult result) throws PolicyViolationException, ConfigurationException {
        enforceRulesWithoutThresholds(result);
        enforceThresholds();
    }

    private void enforceRulesWithoutThresholds(OperationResult result)
            throws PolicyViolationException {
        if (!context.hasFocusOfType(AssignmentHolderType.class)) { // temporary - what about projection-only rules?
            result.recordNotApplicable();
            return;
        }

        computeEnforcementForFocusRules();
        computeEnforcementForAssignmentRules();
        // TODO projection rules

        if (isEnforcementPreviewMode()) {
            enforceInPreviewMode();
        } else {
            enforceInRegularMode();
        }
    }

    private boolean isEnforcementPreviewMode() {
        return isPreviewPolicyRulesEnforcement(context.getOptions());
    }

    private void enforceInPreviewMode() {
        PolicyRuleEnforcerPreviewOutputType output = new PolicyRuleEnforcerPreviewOutputType();
        output.getRule().addAll(rulesBeans);
        context.setPolicyRuleEnforcerPreviewOutput(output);
    }

    private void enforceInRegularMode()
            throws PolicyViolationException {
        if (!messages.isEmpty()) {
            LocalizableMessage message = new LocalizableMessageListBuilder()
                    .messages(messages)
                    .separator(LocalizableMessageList.SEMICOLON)
                    .buildOptimized();
            throw ModelCommonBeans.get().localizationService.translate(
                    new PolicyViolationException(message));
        }
    }

    private void computeEnforcementForFocusRules() {
        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext != null) {
            computeEnforcementForTriggeredRules(focusContext.getObjectPolicyRules());
        }
    }

    private void computeEnforcementForAssignmentRules() {
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple != null) {
            // We do not need to consider foreign policy rules here, as they are enforced on their respective primary hosts.
            evaluatedAssignmentTriple.simpleAccept(
                    assignment -> computeEnforcementForTriggeredRules(assignment.getAllTargetsPolicyRules()));
        }
    }

    private void computeEnforcementForTriggeredRules(Collection<? extends EvaluatedPolicyRule> policyRules) {
        for (EvaluatedPolicyRule policyRule: policyRules) {

            Collection<EvaluatedPolicyRuleTrigger<?>> triggers = policyRule.getTriggers();
            if (triggers.isEmpty()) {
                continue;
            }

            boolean enforceAll = policyRule.containsEnabledAction(EnforcementPolicyActionType.class);
            Collection<EvaluatedPolicyRuleTrigger<?>> enforcingTriggers;
            if (enforceAll) {
                enforcingTriggers = triggers;
            } else {
                enforcingTriggers = triggers.stream()
                        .filter(EvaluatedPolicyRuleTrigger::isEnforcementOverride)
                        .collect(Collectors.toList());
                if (enforcingTriggers.isEmpty()) {
                    continue;
                }
            }

            // TODO really include assignments content?
            policyRule.addToEvaluatedPolicyRuleBeans(
                    rulesBeans,
                    new PolicyRuleExternalizationOptions(FULL, true),
                    t -> enforceAll || t.isEnforcementOverride(),
                    policyRule.getNewOwner());

            List<TreeNode<LocalizableMessage>> messageTrees = extractMessages(enforcingTriggers, NORMAL);
            for (TreeNode<LocalizableMessage> messageTree : messageTrees) {
                messages.add(messageTree.getUserObject());
            }
        }
    }

    private void enforceThresholds()
            throws ThresholdPolicyViolationException, ConfigurationException {
        if (isEnforcementPreviewMode()) {
            return; // preview should be already recorded
        }
        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext != null) {
            for (EvaluatedPolicyRule policyRule : focusContext.getObjectPolicyRules()) {
                // In theory we could count events also for other kinds of actions (not only SuspendTask)
                if (policyRule.containsEnabledAction(SuspendTaskPolicyActionType.class)) {
                    if (policyRule.isOverThreshold()) {
                        throw new ThresholdPolicyViolationException(
                                new SingleLocalizableMessage("PolicyRuleEnforces.policyViolationMessage", new Object[] { policyRule.getPolicyRule() }),
                                "Policy rule violation: " + policyRule.getPolicyRule());
                    }
                }
            }
        }
    }
}
