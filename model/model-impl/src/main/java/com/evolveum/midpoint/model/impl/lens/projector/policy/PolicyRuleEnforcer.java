/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.LocalizableMessageListBuilder;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

/**
 * Code used to enforce the policy rules that have the enforce action.
 *
 * Originally this was a regular ChangeHook. However, when invoked among other hooks, it is too late (see MID-4797).
 * So we had to convert it into regular code. Some parts still carry this history, until properly rewritten (MID-4798).
 *
 * @author semancik
 *
 */
@Component
public class PolicyRuleEnforcer {

    //private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleEnforcer.class);

    @Autowired private PrismContext prismContext;
    @Autowired private LocalizationService localizationService;

    // TODO clean this up
    private static class EvaluationContext {
        private final List<LocalizableMessage> messages = new ArrayList<>();
        private final List<EvaluatedPolicyRuleType> rules = new ArrayList<>();
    }

    public <O extends ObjectType> void execute(@NotNull ModelContext<O> context) throws PolicyViolationException {
        EvaluationContext evalCtx = executeInternal(context);
        if (context.isPreview()) {
            executePreview(context, evalCtx);
        } else {
            executeRegular(evalCtx);
        }
    }

    private void executeRegular(EvaluationContext evalCtx)
            throws PolicyViolationException {
        if (!evalCtx.messages.isEmpty()) {
            LocalizableMessage message = new LocalizableMessageListBuilder()
                    .messages(evalCtx.messages)
                    .separator(LocalizableMessageList.SEMICOLON)
                    .buildOptimized();
            throw localizationService.translate(new PolicyViolationException(message));
        }
    }

    private void executePreview(@NotNull ModelContext<? extends ObjectType> context, EvaluationContext evalCtx) {
        PolicyRuleEnforcerPreviewOutputType output = new PolicyRuleEnforcerPreviewOutputType(prismContext);
        output.getRule().addAll(evalCtx.rules);
        ((LensContext) context).setPolicyRuleEnforcerPreviewOutput(output);
    }

    @NotNull
    private <O extends ObjectType> EvaluationContext executeInternal(@NotNull ModelContext<O> context) {
        EvaluationContext evalCtx = new EvaluationContext();
        ModelElementContext<O> focusContext = context.getFocusContext();
        if (focusContext == null || !FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
            return evalCtx;
        }
        //noinspection unchecked
        ModelContext<? extends FocusType> contextCasted = (ModelContext<? extends FocusType>) context;
        evaluateFocusRules(evalCtx, contextCasted);
        evaluateAssignmentRules(evalCtx, contextCasted);

        return evalCtx;
    }

    private <F extends FocusType> void evaluateFocusRules(EvaluationContext evalCtx, ModelContext<F> context) {
        enforceTriggeredRules(evalCtx, context.getFocusContext().getPolicyRules());
    }

    private <F extends FocusType> void evaluateAssignmentRules(EvaluationContext evalCtx, ModelContext<F> context) {
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            return;
        }
        //noinspection unchecked
        evaluatedAssignmentTriple.simpleAccept(assignment -> enforceTriggeredRules(evalCtx, assignment.getAllTargetsPolicyRules()));
    }

    private void enforceTriggeredRules(EvaluationContext evalCtx, Collection<? extends EvaluatedPolicyRule> policyRules) {
        for (EvaluatedPolicyRule policyRule: policyRules) {

            Collection<EvaluatedPolicyRuleTrigger<?>> triggers = policyRule.getTriggers();
            if (triggers.isEmpty()) {
                continue;
            }

            boolean enforceAll = policyRule.containsEnabledAction(EnforcementPolicyActionType.class);
            Collection<EvaluatedPolicyRuleTrigger<?>> triggersFiltered;
            if (enforceAll) {
                triggersFiltered = triggers;
            } else {
                triggersFiltered = triggers.stream()
                        .filter(EvaluatedPolicyRuleTrigger::isEnforcementOverride)
                        .collect(Collectors.toList());
                if (triggersFiltered.isEmpty()) {
                    continue;
                }
            }

            // TODO really include assignments content?
            policyRule.addToEvaluatedPolicyRuleTypes(evalCtx.rules,
                    new PolicyRuleExternalizationOptions(FULL, true, true),
                    t -> enforceAll || t.isEnforcementOverride(), prismContext);

            List<TreeNode<LocalizableMessage>> messageTrees = EvaluatedPolicyRuleUtil.extractMessages(triggersFiltered, EvaluatedPolicyRuleUtil.MessageKind.NORMAL);
            for (TreeNode<LocalizableMessage> messageTree : messageTrees) {
                evalCtx.messages.add(messageTree.getUserObject());
            }
        }
    }
}
