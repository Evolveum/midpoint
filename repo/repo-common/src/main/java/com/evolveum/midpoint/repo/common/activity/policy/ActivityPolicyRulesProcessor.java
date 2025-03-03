/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

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

        String taskOid = activityRun.getRunningTask().getOid();

        List<EvaluatedActivityPolicyRule> rules = policies.stream()
                .map(ap -> new EvaluatedActivityPolicyRule(ap, taskOid))
                .toList();
        activityRun.getActivityPolicyRulesContext().setPolicyRules(rules);
    }

    public void evaluateAndEnforceRules(OperationResult result)
            throws ThresholdPolicyViolationException, SchemaException, ObjectNotFoundException {

        List<EvaluatedActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();
        if (rules.isEmpty()) {
            return;
        }

        Collection<ActivityPolicyStateType> policyStates = new ArrayList<>();
        for (EvaluatedActivityPolicyRule rule : rules) {
            ActivityPolicyStateType state = evaluateRule(rule, result);
            if (state != null) {
                policyStates.add(state);
            }
        }

        try {
            // we'll enforce all triggered rules at once
            enforceRules(result);
        } finally {
            // update policy states after all rules were enforced (even if ThresholdPolicyViolationException was thrown)
            Map<String, EvaluatedActivityPolicyRule> ruleMap = rules.stream()
                    .collect(Collectors.toMap(r -> r.getRuleId(), r -> r));

            Map<String, ActivityPolicyStateType> updated = activityRun.updateActivityPolicyState(policyStates, result);
            updated.forEach((id, state) -> ruleMap.get(id).setCurrentState(state));
        }
    }

    private ActivityPolicyStateType evaluateRule(EvaluatedActivityPolicyRule rule, OperationResult result) {
        if (rule.isTriggered()) {
            LOGGER.trace("Policy rule {} was already triggered, skipping evaluation", rule);
            return null;
        }

        // todo check activity state whether rule was not already triggered
        ActivityPolicyStateType state = rule.getCurrentState();
        if (state != null && !state.getTriggers().isEmpty()) {
            LOGGER.trace("Policy rule {} was already triggered, triggers stored in policy state, skipping evaluation", rule);
            return null; // no state change
        }

        ActivityPolicyConstraintsType constraints = rule.getPolicy().getPolicyConstraints();

        ActivityPolicyConstraintsEvaluator evaluator = ActivityPolicyConstraintsEvaluator.get();

        ActivityPolicyRuleEvaluationContext context = new ActivityPolicyRuleEvaluationContext(rule, activityRun);

        List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = evaluator.evaluateConstraints(constraints, context, result);
        rule.setTriggers(triggers);

        if (triggers.isEmpty()) {
            return null;
        }

        ActivityPolicyStateType newState = new ActivityPolicyStateType();
        triggers.stream()
                .map(t -> createActivityStateTrigger(rule, t))
                .forEach(t -> newState.getTriggers().add(t));

        return newState;
    }

    private ActivityPolicyTriggerType createActivityStateTrigger(
            EvaluatedActivityPolicyRule rule, EvaluatedActivityPolicyRuleTrigger trigger) {

        ActivityPolicyTriggerType state = new ActivityPolicyTriggerType();
        state.setConstraintIdentifier(rule.getName());
        state.setMessage(LocalizationUtil.createLocalizableMessageType(trigger.getMessage()));

        return state;
    }

    private void enforceRules(OperationResult result) throws ThresholdPolicyViolationException {
        List<EvaluatedActivityPolicyRule> rules = activityRun.getActivityPolicyRulesContext().getPolicyRules();
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
                throw new ThresholdPolicyViolationException("Policy violation, rule: " + rule);
            }
        }
    }
}
