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

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
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

    public void collectRules() {
        LOGGER.trace("Collecting activity policy rules for activity {} ({})",
                activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

        ActivityPoliciesType activityPolicy = activityRun.getActivity().getDefinition().getPoliciesDefinition().getPolicies();
        List<ActivityPolicyType> policies = activityPolicy.getPolicy();

        List<EvaluatedActivityPolicyRule> rules = policies.stream()
                .map(ap -> new EvaluatedActivityPolicyRule(ap, activityRun.getActivityPath()))
                .toList();
        activityRun.getActivityPolicyRulesContext().setPolicyRules(rules);
    }

    public void evaluateAndEnforceRules(OperationResult result)
            throws ThresholdPolicyViolationException, SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        LOGGER.trace("Evaluating activity policy rules for {} ({})",
                activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

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

        ActivityPolicyConstraintsType constraints = rule.getPolicy().getPolicyConstraints();

        ActivityPolicyConstraintsEvaluator evaluator = ActivityPolicyConstraintsEvaluator.get();

        ActivityPolicyRuleEvaluationContext context = new ActivityPolicyRuleEvaluationContext(rule, activityRun);

        List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = evaluator.evaluateConstraints(constraints, context, result);
        rule.setTriggers(triggers);

        if (triggers.isEmpty()) {
            return null;
        }

        ActivityPolicyStateType newState = new ActivityPolicyStateType();
        newState.setIdentifier(rule.getRuleId());
        triggers.stream()
                .map(t -> createActivityStateTrigger(rule, t))
                .forEach(t -> newState.getTriggers().add(t));
        newState.freeze();

        return newState;
    }

    private ActivityPolicyTriggerType createActivityStateTrigger(
            EvaluatedActivityPolicyRule rule, EvaluatedActivityPolicyRuleTrigger<?> trigger) {

        ActivityPolicyTriggerType state = new ActivityPolicyTriggerType();
        state.setConstraintIdentifier(rule.getName());
        state.setMessage(LocalizationUtil.createLocalizableMessageType(trigger.getMessage()));

        return state;
    }

    private void enforceRules(OperationResult result) throws ThresholdPolicyViolationException {
        LOGGER.trace("Enforcing activity policy rules for {} ({})",
                activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

        List<EvaluatedActivityPolicyRule> rules = activityRun.getActivityPolicyRulesContext().getPolicyRules();

        for (EvaluatedActivityPolicyRule rule : rules) {
            if (!rule.isTriggered()) {
                LOGGER.trace("Policy rule {} was not triggered, skipping enforcement", rule);
                continue;
            }

            if (!rule.isEnforced()) {
                LOGGER.trace("Enforcing policy rule {}", rule);
                rule.enforced();

                executeOneTimeActions(rule, result);
            }

            executeAlwaysActions(rule, result);
        }
    }

    /**
     * Actions that should be triggered once. E.g. notifications for the given rule.
     */
    private void executeOneTimeActions(EvaluatedActivityPolicyRule rule, OperationResult result) {
        if (rule.containsAction(NotificationActivityPolicyActionType.class)) {
            LOGGER.debug("Sending notification because of policy violation, rule: {}", rule);

            activityRun.sendActivityPolicyRuleTriggeredEvent(rule, result);
        }
    }

    /**
     * Actions that should be triggered always. E.g. suspend task.
     */
    private void executeAlwaysActions(EvaluatedActivityPolicyRule rule, OperationResult result)
            throws ThresholdPolicyViolationException {

        if (rule.containsAction(SuspendTaskActivityPolicyActionType.class)) {
            LOGGER.debug("Suspending task because of policy violation, rule: {}", rule);

            throw new ThresholdPolicyViolationException("Policy violation, rule: " + rule.getName());
        }
    }
}
