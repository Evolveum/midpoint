/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * This processor is responsible for collecting, evaluating and enforcing activity policy rules.
 */
public class ActivityPolicyRulesProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityPolicyRulesProcessor.class);

    @NotNull
    private final AbstractActivityRun<?, ?, ?> activityRun;

    public ActivityPolicyRulesProcessor(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    private ActivityPolicyRulesContext getPolicyRulesContext() {
        return activityRun.getActivityPolicyRulesContext();
    }

    public void collectRules(OperationResult result) {
        ActivityPoliciesType activityPolicy = activityRun.getActivity().getDefinition().getPoliciesDefinition().getPolicies();
        List<ActivityPolicyType> policies = activityPolicy.getPolicy();

        List<EvaluatedActivityPolicyRule> rules = policies.stream().map(EvaluatedActivityPolicyRule::new).toList();
        activityRun.getActivityPolicyRulesContext().setPolicyRules(rules);
    }

    public void evaluateAndEnforceRules(OperationResult result) {
        List<EvaluatedActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();
        if (rules.isEmpty()) {
            return;
        }

        for (EvaluatedActivityPolicyRule rule : rules) {
            evaluateRule(rule, result);
        }

        enforceRules(result);
    }

    private void evaluateRule(EvaluatedActivityPolicyRule rule, OperationResult result) {
        if (rule.isTriggered()) {
            LOGGER.trace("Policy rule {} was already triggered, skipping evaluation", rule);
            return;
        }

        ActivityPolicyConstraintsType constraints = rule.getPolicy().getPolicyConstraints();

        ActivityPolicyConstraintsEvaluator evaluator = ActivityPolicyConstraintsEvaluator.get();

        ActivityPolicyRuleEvaluationContext context = new ActivityPolicyRuleEvaluationContext(rule, activityRun);

        List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = evaluator.evaluateConstraints(constraints, context, result);
        rule.setTriggers(triggers);
    }

    private void enforceRules(OperationResult result) {
        List<EvaluatedActivityPolicyRule> rules = activityRun.getActivityPolicyRulesContext().getPolicyRules();
        if (rules.isEmpty()) {
            return;
        }

        // todo make sure we store somewhere that rule was already triggered and enforced
        //  e.g. we don't send notification after each processed item when execution time was exceeded

        // todo check also whether action condition is enabled
        for (EvaluatedActivityPolicyRule rule : rules) {
            if (!rule.isTriggered()) {
                LOGGER.trace("Policy rule {} was not triggered, skipping enforcement", rule);
                continue;
            }

            if (rule.isEnforced()) {
                LOGGER.trace("Policy rule {} was already enforced, skipping enforcement", rule);
                continue;
            }

            LOGGER.trace("Enforcing policy rule {}", rule);

            rule.enforced();

            if (rule.containsAction(NotificationPolicyActionType.class)) {
                LOGGER.debug("Sending notification because of policy violation, rule: {}", rule);
                activityRun.sendActivityPolicyRuleTriggeredEvent(rule, result);
            }

            if (rule.containsAction(SuspendTaskPolicyActionType.class)) {
                LOGGER.debug("Suspending task because of policy violation, rule: {}", rule);
// todo how to handle this exception?
//                throw new ThresholdPolicyViolationException("Policy violation, rule: " + rule);
            }
        }
    }
}
