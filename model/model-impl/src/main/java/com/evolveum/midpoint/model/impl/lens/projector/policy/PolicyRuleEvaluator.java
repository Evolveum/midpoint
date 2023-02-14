/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.F_AND;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators.CompositeConstraintEvaluator;

import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedCompositeTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RecordPolicyActionType;

/**
 * Abstract evaluator of assignment-, focus- and projection-related policy rules.
 *
 * Intentionally package-private.
 */
class PolicyRuleEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleEvaluator.class);

    private static final String OP_EVALUATE_RULE = PolicyRuleEvaluator.class.getName() + ".evaluateRule";

    @NotNull final LensContext<?> context;
    @NotNull final Task task;
    @NotNull final PolicyRulesCollector<?> collector;

    PolicyRuleEvaluator(@NotNull LensContext<?> context, @NotNull Task task) {
        this.context = context;
        this.task = task;
        this.collector = new PolicyRulesCollector<>(context, task);
    }

    /**
     * Evaluates rules (wrapped in appropriate evaluation contexts).
     *
     * Situation-related rules are to be evaluated last, because they refer to triggering of other rules.
     *
     * Note: It is the responsibility of the administrator to avoid two situation-related rules referring to each other, i.e.
     *
     *    Situation(URL1) -> Action1 [URL2]
     *    Situation(URL2) -> Action2 [URL1]
     */
    void evaluateRules(
            List<? extends PolicyRuleEvaluationContext<?>> contexts, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        for (PolicyRuleEvaluationContext<?> ctx : contexts) {
            if (!ctx.policyRule.hasSituationConstraint()) {
                evaluateRule(ctx, result);
            }
        }
        for (PolicyRuleEvaluationContext<?> ctx : contexts) {
            if (ctx.policyRule.hasSituationConstraint()) {
                evaluateRule(ctx, result);
            }
        }
    }

    /**
     * Evaluates given policy rule in a given context.
     */
    private <O extends ObjectType> void evaluateRule(
            @NotNull PolicyRuleEvaluationContext<O> ctx, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        String ruleShortString = ctx.policyRule.toShortString();
        String ruleIdentifier = ctx.policyRule.getPolicyRuleIdentifier();
        String ctxShortDescription = ctx.getShortDescription();
        OperationResult result = parentResult.subresult(OP_EVALUATE_RULE)
                .addParam(OperationResult.PARAM_POLICY_RULE, ruleShortString)
                .addParam(OperationResult.PARAM_POLICY_RULE_ID, ruleIdentifier)
                .addContext("context", ctxShortDescription)
                .build();
        try {
            LOGGER.trace("Evaluating policy rule {} ({}) in {}", ruleShortString, ruleIdentifier, ctxShortDescription);
            PolicyConstraintsType constraints = ctx.policyRule.getPolicyConstraints();
            JAXBElement<PolicyConstraintsType> conjunction = new JAXBElement<>(F_AND, PolicyConstraintsType.class, constraints);
            EvaluatedCompositeTrigger trigger = CompositeConstraintEvaluator.get().evaluate(conjunction, ctx, result);
            LOGGER.trace("Evaluated composite trigger {} for ctx {}", trigger, ctx);
            if (trigger != null && !trigger.getInnerTriggers().isEmpty()) {
                List<EvaluatedPolicyRuleTrigger<?>> triggers;
                // TODO reconsider this
                if (constraints.getName() == null && constraints.getPresentation() == null) {
                    triggers = new ArrayList<>(trigger.getInnerTriggers());
                } else {
                    triggers = Collections.singletonList(trigger);
                }
                ctx.triggerRule(triggers);
            }
            boolean triggered = ctx.policyRule.isTriggered();
            LOGGER.trace("Policy rule triggered: {}", triggered);
            result.addReturn("triggered", triggered);
            if (triggered) {
                LOGGER.trace("Start to compute actions");
                ((EvaluatedPolicyRuleImpl) ctx.policyRule)
                        .computeEnabledActions(ctx, ctx.getObject(), ctx.task, result);
                if (ctx.policyRule.containsEnabledAction(RecordPolicyActionType.class)) {
                    ctx.record(); // TODO postpone this (e.g. because of thresholds and also MID-7123)
                }
                result.addArbitraryObjectCollectionAsReturn("enabledActions", ctx.policyRule.getEnabledActions());
            }
            traceRuleEvaluationResult(ctx.policyRule, ctx);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void traceRuleEvaluationResult(EvaluatedPolicyRule rule, PolicyRuleEvaluationContext<?> ctx) {
        if (LOGGER.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\n---[ POLICY RULE ");
            if (rule.isTriggered()) {
                sb.append("# ");
            }
            sb.append(rule.toShortString());
            sb.append(" for ");
            sb.append(ctx.getShortDescription());
            sb.append(" (").append(ctx.lensContext.getState()).append(")");
            sb.append("]---------------------------");
            sb.append("\n");
            sb.append(rule.debugDump());
            LOGGER.trace("{}", sb);
        }
    }
}
