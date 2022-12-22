/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.context.*;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.result.OperationResult;
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

import static com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil.MessageKind.NORMAL;
import static com.evolveum.midpoint.model.api.util.EvaluatedPolicyRuleUtil.extractMessages;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

/**
 * Code used to enforce the policy rules that have the enforce action.
 *
 * Originally this was a regular {@link ChangeHook}. However, when invoked among other hooks, it is too late (see MID-4797).
 * So we had to convert it into regular code and run it right after the first {@link Projector} run.
 *
 * @author semancik
 */
@Component
public class PolicyRuleEnforcer {

    //private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleEnforcer.class);

    private static final String OP_EXECUTE = PolicyRuleEnforcer.class.getName() + ".execute";

    @Autowired private PrismContext prismContext;
    @Autowired private LocalizationService localizationService;

    private static class EvaluationContext {
        private final List<LocalizableMessage> messages = new ArrayList<>();
        private final List<EvaluatedPolicyRuleType> rules = new ArrayList<>();
    }

    public <O extends ObjectType> void execute(@NotNull LensContext<O> context, OperationResult parentResult)
            throws PolicyViolationException {
        OperationResult result = parentResult.createMinorSubresult(OP_EXECUTE);
        try {
            EvaluationContext evalCtx = executeInternal(context);
            if (evalCtx == null) {
                result.recordNotApplicable();
                return;
            }

            if (context.isPreview()) {
                executePreview(context, evalCtx);
            } else {
                executeRegular(evalCtx);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
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

    private void executePreview(@NotNull LensContext<? extends ObjectType> context, EvaluationContext evalCtx) {
        PolicyRuleEnforcerPreviewOutputType output = new PolicyRuleEnforcerPreviewOutputType(prismContext);
        output.getRule().addAll(evalCtx.rules);
        context.setPolicyRuleEnforcerPreviewOutput(output);
    }

    private <O extends ObjectType> EvaluationContext executeInternal(@NotNull LensContext<O> context) {
        if (!context.hasFocusOfType(FocusType.class)) { // TODO or AssignmentHolderType?
            return null;
        }
        EvaluationContext evalCtx = new EvaluationContext();
        //noinspection unchecked
        LensContext<? extends FocusType> contextCasted = (LensContext<? extends FocusType>) context;
        evaluateFocusRules(evalCtx, contextCasted);
        evaluateAssignmentRules(evalCtx, contextCasted);

        return evalCtx;
    }

    private <F extends FocusType> void evaluateFocusRules(EvaluationContext evalCtx, LensContext<F> context) {
        enforceTriggeredRules(evalCtx, context.getFocusContext().getObjectPolicyRules());
    }

    private <F extends FocusType> void evaluateAssignmentRules(EvaluationContext evalCtx, LensContext<F> context) {
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple == null) {
            return;
        }
        evaluatedAssignmentTriple.simpleAccept(assignment -> enforceTriggeredRules(evalCtx, assignment.getAllTargetsPolicyRules()));
    }

    private void enforceTriggeredRules(EvaluationContext evalCtx, Collection<? extends EvaluatedPolicyRule> policyRules) {
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
            policyRule.addToEvaluatedPolicyRuleBeans(evalCtx.rules,
                    new PolicyRuleExternalizationOptions(FULL, true, true),
                    t -> enforceAll || t.isEnforcementOverride());

            List<TreeNode<LocalizableMessage>> messageTrees = extractMessages(enforcingTriggers, NORMAL);
            for (TreeNode<LocalizableMessage> messageTree : messageTrees) {
                evalCtx.messages.add(messageTree.getUserObject());
            }
        }
    }
}
