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
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
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

        ActivityPolicyRulesContext ctx = getPolicyRulesContext();
        ctx.clearPolicyRules();
        ctx.addPolicyRules(rules);
    }

    public void evaluateAndEnforceRules(ItemProcessingResult processingResult, @NotNull OperationResult result)
            throws ThresholdPolicyViolationException, SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        LOGGER.trace("Evaluating activity policy rules for {} ({})",
                activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

        Collection<EvaluatedActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();
        if (rules.isEmpty()) {
            return;
        }

        Collection<ActivityPolicyStateType> policyStates = evaluateRules(processingResult, result);

        updateCounters(result);

        enforceRules(policyStates, result);
    }

    private Collection<ActivityPolicyStateType> evaluateRules(ItemProcessingResult processingResult, OperationResult result) {
        Collection<EvaluatedActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();

        Collection<ActivityPolicyStateType> policyStates = new ArrayList<>();
        for (EvaluatedActivityPolicyRule rule : rules) {
            ActivityPolicyStateType state = evaluateRule(rule, processingResult, result);
            if (state != null) {
                policyStates.add(state);
            }
        }

        return policyStates;
    }

    private void updateCounters(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        new ActivityPolicyRuleUpdater(activityRun)
                .updateCounters(result);
    }

    private void enforceRules(Collection<ActivityPolicyStateType> policyStates, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, ThresholdPolicyViolationException {

        try {
            LOGGER.trace("Enforcing activity policy rules for {} ({})",
                    activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

            Collection<EvaluatedActivityPolicyRule> rules = activityRun.getActivityPolicyRulesContext().getPolicyRules();

            for (EvaluatedActivityPolicyRule rule : rules) {
                if (!rule.isTriggered()) {
                    LOGGER.trace("Policy rule {} was not triggered, skipping enforcement", rule);
                    continue;
                }

                for (EvaluatedPolicyReaction reaction : rule.getApplicableReactions()) {
                    if (!rule.isEnforced()) {
                        executeOneTimeActions(reaction, result);
                    }

                    executeAlwaysActions(reaction, result);
                }

                rule.enforced();
            }
        } finally {
            Collection<EvaluatedActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();

            // update policy states after all rules were enforced (even if ThresholdPolicyViolationException was thrown)
            Map<String, EvaluatedActivityPolicyRule> ruleMap = rules.stream()
                    .collect(Collectors.toMap(r -> r.getRuleIdentifier(), r -> r));

            Map<String, ActivityPolicyStateType> updated = activityRun.updateActivityPolicyState(policyStates, result);
            updated.forEach((id, state) -> ruleMap.get(id).setCurrentState(state));
        }
    }

    private ActivityPolicyStateType evaluateRule(
            EvaluatedActivityPolicyRule rule, ItemProcessingResult processingResult, OperationResult result) {

        ActivityPolicyConstraintsType constraints = rule.getPolicy().getPolicyConstraints();

        ActivityPolicyConstraintsEvaluator evaluator = ActivityPolicyConstraintsEvaluator.get();

        ActivityPolicyRuleEvaluationContext context = new ActivityPolicyRuleEvaluationContext(rule, activityRun, processingResult);

        List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = evaluator.evaluateConstraints(constraints, context, result);
        rule.setTriggers(triggers);

        if (triggers.isEmpty()) {
            return null;
        }

        ActivityPolicyStateType newState = new ActivityPolicyStateType();
        newState.setIdentifier(rule.getRuleIdentifier());
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

    /**
     * Actions that should be triggered once. E.g. notifications for the given rule.
     */
    private void executeOneTimeActions(EvaluatedPolicyReaction reaction, OperationResult result) {
        if (reaction.containsAction(NotificationActivityPolicyActionType.class)) {
            LOGGER.debug("Sending notification because of policy violation, rule: {}", reaction);

            activityRun.sendActivityPolicyRuleTriggeredEvent(reaction.getRule(), result);
        }
    }

    /**
     * Actions that should be triggered always. E.g. suspend task.
     */
    private void executeAlwaysActions(EvaluatedPolicyReaction reaction, OperationResult result)
            throws ThresholdPolicyViolationException {

        EvaluatedActivityPolicyRule rule = reaction.getRule();

        String ruleName = rule.getName();
        String reactionName = reaction.getName();

        LocalizableMessage message = new SingleLocalizableMessage(
                "ActivityPolicyRulesProcessor.policyViolationMessage", new Object[] { ruleName, reactionName });

        String defaultMessage = "Policy violation, rule: ".formatted(ruleName, reactionName);

        TaskRunResult.TaskRunResultStatus status = null;

        RestartActivityPolicyActionType restartAction = reaction.getAction(RestartActivityPolicyActionType.class);
        if (restartAction != null) {
            LOGGER.debug("Restarting because of policy violation, rule: {}", reaction);

            throw new ActivityThresholdPolicyViolationException(
                    message,
                    defaultMessage,
                    TaskRunResult.TaskRunResultStatus.RESTART_ACTIVITY_ERROR,
                    rule.getRuleIdentifier());
        }

        if (reaction.containsAction(SkipActivityPolicyActionType.class)) {
            LOGGER.debug("Skipping activity because of policy violation, rule: {}", reaction);

            status = TaskRunResult.TaskRunResultStatus.SKIP_ACTIVITY_ERROR;
        }

        if (reaction.containsAction(SuspendTaskActivityPolicyActionType.class)) {
            LOGGER.debug("Suspending task because of policy violation, rule: {}", reaction);

            status = TaskRunResult.TaskRunResultStatus.HALTING_ERROR;
        }

        if (status == null) {
            LOGGER.debug("No action to take for policy violation, rule: {}", reaction);
            return;
        }

        throw new ActivityThresholdPolicyViolationException(message, defaultMessage, status, rule.getRuleIdentifier());
    }
}
