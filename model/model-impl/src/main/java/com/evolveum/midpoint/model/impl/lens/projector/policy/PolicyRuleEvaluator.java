/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType.F_AND;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBElement;

import com.evolveum.midpoint.model.api.context.AssociatedPolicyRule;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RecordPolicyActionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedCompositeTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators.CompositeConstraintEvaluator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

/**
 * Abstract evaluator of assignment-, focus- and projection-related policy rules.
 *
 * By evaluating a rule we mean the following:
 *
 * . constraints are evaluated, i.e. determined whether they fire, and appropriate triggers are created in that process;
 * . rule exceptions are checked (currently on on assignments);
 * . triggers are added to the rule (if there is no exception recorded);
 * . enabled actions are computed;
 * . foreign policy rules (in assignments) are set.
 *
 * See {@link #evaluateRule(PolicyRuleEvaluationContext, OperationResult)}.
 *
 * Intentionally package-private.
 */
abstract class PolicyRuleEvaluator {

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
     *
     * TODO migrate to marks
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

        EvaluatedPolicyRuleImpl rule = ctx.policyRule;
        String ruleShortString = rule.toShortString();
        String ruleIdentifier = rule.getPolicyRuleIdentifier();

        // // MID-10779 disabled now, since this would cause problems with unevaluated rules in some situations: e.g. assignment exclusion policies
        // if (ctx.lensContext.hasTriggeredObjectPolicyRule(ruleIdentifier)) {
        //     LOGGER.trace("Skipping evaluation of policy rule {}, because rule has already been triggered: {}", ruleIdentifier, ruleShortString);
        //     return;
        // }

        String ctxShortDescription = ctx.getShortDescription();
        OperationResult result = parentResult.subresult(OP_EVALUATE_RULE)
                .addParam(OperationResult.PARAM_POLICY_RULE, ruleShortString)
                .addParam(OperationResult.PARAM_POLICY_RULE_ID, ruleIdentifier)
                .addContext("context", ctxShortDescription)
                .build();
        try {
            LOGGER.trace("Evaluating policy rule {} ({}) in {}", ruleShortString, ruleIdentifier, ctxShortDescription);

            PolicyConstraintsType constraints = rule.getPolicyConstraints();
            JAXBElement<PolicyConstraintsType> conjunction = new JAXBElement<>(F_AND, PolicyConstraintsType.class, constraints);

            Collection<EvaluatedCompositeTrigger> compositeTriggers =
                    CompositeConstraintEvaluator.get().evaluate(conjunction, ctx, result);
            LOGGER.trace("Evaluated composite trigger(s) {} for ctx {}", compositeTriggers, ctx);

            var compositeTrigger = MiscUtil.extractSingleton(compositeTriggers); // currently always the case
            if (compositeTrigger != null && !compositeTrigger.getInnerTriggers().isEmpty()) {
                ctx.triggerRuleIfNoExceptions(
                        getIndividualTriggers(compositeTrigger, constraints));
            }

            rule.setEvaluated();
            rule.computeEnabledActions(ctx, ctx.getObject(), ctx.task, result);

            boolean triggered = rule.isTriggered();
            LOGGER.trace("Policy rule triggered: {}", triggered);
            result.addReturn("triggered", triggered);
            if (triggered) {
                result.addArbitraryObjectCollectionAsReturn("enabledActions", rule.getEnabledActions());
                rule.registerAsForeignRuleIfNeeded();

                ctx.lensContext.addTriggeredObjectPolicyRule(rule);
            }
            traceRuleEvaluationResult(rule, ctx);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Retrieves triggers corresponding to the root-level constraints.
     *
     * We consider unnamed root-level composition as a fake one, so we look into individual sub-triggers in that case;
     * whereas if there is a name and/or presentation, we consider that composition as a single entity.
    */
    private @NotNull static List<EvaluatedPolicyRuleTrigger<?>> getIndividualTriggers(
            @NotNull EvaluatedCompositeTrigger trigger, @NotNull PolicyConstraintsType constraints) {
        // TODO reconsider this
        if (constraints.getName() == null && constraints.getPresentation() == null) {
            return List.copyOf(trigger.getInnerTriggers());
        } else {
            return List.of(trigger);
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

    abstract void record(OperationResult result) throws SchemaException;

    @NotNull <R extends AssociatedPolicyRule> List<R> selectRulesToRecord(@NotNull Collection<R> allRules) {
        return allRules.stream()
                .filter(rule -> rule.isTriggered() && rule.containsEnabledAction(RecordPolicyActionType.class))
                .collect(Collectors.toList());
    }
}
