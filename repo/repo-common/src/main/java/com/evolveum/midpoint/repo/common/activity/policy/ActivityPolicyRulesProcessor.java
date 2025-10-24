/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ActivityCompositeConstraintEvaluator;

import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.repo.common.activity.ActivityThresholdPolicyViolationException;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.MiscUtil;
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
        List<EvaluatedActivityPolicyRule> rules = collectRulesFromActivity(activityRun.getActivity());
        getPolicyRulesContext().setPolicyRules(rules);

        LOGGER.trace("Found {} activity policy rules for activity hierarchy {} ({})",
                rules.size(), activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());
    }

    /**
     * Collects all policy rules from the given activity and its parent activities recursively.
     *
     * Rules from parent activities are included because otherwise they would only be validated
     * in-between child activities, which might be too infrequent (e.g., for execution time policies).
     * By collecting rules from the entire activity hierarchy, we ensure that parent rules are
     * enforced as often as necessary.
     *
     * @param activity The activity from which to start collecting policy rules.
     * @return List of evaluated activity policy rules, ordered by their defined order.
     */
    private List<EvaluatedActivityPolicyRule> collectRulesFromActivity(@NotNull Activity<?, ?> activity) {
        String identifier = activity.getIdentifier();
        ActivityPath activityPath = activity.getPath();

        LOGGER.trace("Collecting activity policy rules for activity {} ({})", identifier, activityPath);

        ActivityPoliciesType activityPolicy = activityRun.getActivity().getDefinition().getPoliciesDefinition().getPolicies();
        List<ActivityPolicyType> policies = activityPolicy.getPolicy();

        List<EvaluatedActivityPolicyRule> rules = policies.stream()
                .map(ap -> new EvaluatedActivityPolicyRule(ap, activityPath))
                .sorted(
                        Comparator.comparing(
                                EvaluatedActivityPolicyRule::getOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        LOGGER.trace("Found {} activity policy rules for activity {} ({})", rules.size(), identifier, activityPath);

        // Recursively collect rules from parent activities, not needed for now as
        // we only support iterative activities (not composite activity)
        // todo what about reconciliation? [viliam]
        // rules.addAll(collectRulesFromActivity(activity.getParent()));

        return rules;
    }

    public void evaluateAndEnforceRules(ItemProcessingResult processingResult, @NotNull OperationResult result)
            throws ThresholdPolicyViolationException, SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        LOGGER.trace("Evaluating activity policy rules for {} ({})",
                activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

        Collection<EvaluatedActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();
        if (rules.isEmpty()) {
            return;
        }

        evaluateRules(processingResult, result);

        updateCounters(result);

        enforceRules(result);
    }

    private void evaluateRules(ItemProcessingResult processingResult, OperationResult result) {
        for (EvaluatedActivityPolicyRule rule : getPolicyRulesContext().getPolicyRules()) {
            evaluateRule(rule, processingResult, result);
        }
    }

    private void updateCounters(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        new ActivityPolicyRuleUpdater(activityRun)
                .updateCounters(result);
    }

    private ActivityPolicyStateType createPolicyState(
            EvaluatedActivityPolicyRule rule, List<EvaluatedPolicyReaction> applicableReactions) {

        ActivityPolicyStateType state = new ActivityPolicyStateType();
        state.setIdentifier(rule.getRuleIdentifier());
        state.setName(rule.getName());

        rule.getTriggers().stream()
                .map(t -> t.toPolicyTriggerType())
                .forEach(t -> state.getTrigger().add(t));

        List<EvaluatedActivityPolicyReactionType> reactionTypes = applicableReactions.stream()
                .map(r -> r.toPolicyReactionBean())
                .toList();
        state.getReaction().addAll(reactionTypes);

        return state;
    }

    private void enforceRules(OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, ThresholdPolicyViolationException {

        Collection<ActivityPolicyStateType> policyStates = new ArrayList<>();

        try {
            LOGGER.trace("Enforcing activity policy rules for {} ({})",
                    activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

            Collection<EvaluatedActivityPolicyRule> rules = activityRun.getActivityPolicyRulesContext().getPolicyRules();

            for (EvaluatedActivityPolicyRule rule : rules) {
                if (!rule.isTriggered()) {
                    LOGGER.trace("Policy rule {} was not triggered, skipping enforcement", rule);
                    continue;
                }

                List<EvaluatedPolicyReaction> reactions = rule.getApplicableReactions();
                if (reactions.isEmpty()) {
                    LOGGER.trace("Policy rule {} has no applicable reactions, skipping enforcement", rule);
                    continue;
                }

                policyStates.add(createPolicyState(rule, reactions));

                for (EvaluatedPolicyReaction reaction : reactions) {
                    executeActions(reaction, result);
                }
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

    private void evaluateRule(
            EvaluatedActivityPolicyRule rule, ItemProcessingResult processingResult, OperationResult result) {

        ActivityPolicyConstraintsType constraints = rule.getPolicy().getPolicyConstraints();
        JAXBElement<ActivityPolicyConstraintsType> element =
                new JAXBElement<>(ActivityPolicyConstraintsType.F_AND, ActivityPolicyConstraintsType.class, constraints);

        ActivityPolicyRuleEvaluationContext context = new ActivityPolicyRuleEvaluationContext(rule, activityRun, processingResult);

        ActivityCompositeConstraintEvaluator evaluator = ActivityCompositeConstraintEvaluator.get();

        List<ActivityCompositeTrigger> compositeTriggers = evaluator.evaluate(element, context, result);
        ActivityCompositeTrigger compositeTrigger = MiscUtil.extractSingleton(compositeTriggers);
        if (compositeTrigger != null && !compositeTrigger.getInnerTriggers().isEmpty()) {
            rule.setTriggers(List.copyOf(compositeTrigger.getInnerTriggers()));
        }
    }

    private void executeActions(EvaluatedPolicyReaction reaction, OperationResult result)
            throws ActivityThresholdPolicyViolationException {

        reaction.enforced();

        for (ActivityPolicyActionType action : reaction.getActions()) {
            if (action instanceof NotificationActivityPolicyActionType na) {
                if (BooleanUtils.isTrue(na.isSingle()) && reaction.isEnforced()) {
                    LOGGER.debug("Skipping notification action (single) because it was already enforced, rule: {}", reaction);
                    continue;
                }

                LOGGER.debug("Sending notification because of policy violation, rule: {}", reaction);

                activityRun.sendActivityPolicyRuleTriggeredEvent(reaction.getRule(), result);
                continue;
            }

            ActivityRunResultStatus runResultStatus;
            if (action instanceof RestartActivityPolicyActionType) {
                LOGGER.debug("Restarting activity because of policy violation, rule: {}", reaction);
                runResultStatus = ActivityRunResultStatus.RESTART_ACTIVITY_ERROR;
            } else if (action instanceof SkipActivityPolicyActionType) {
                LOGGER.debug("Skipping activity because of policy violation, rule: {}", reaction);
                runResultStatus = ActivityRunResultStatus.SKIP_ACTIVITY_ERROR;
            } else if (action instanceof SuspendTaskActivityPolicyActionType) {
                LOGGER.debug("Suspending task because of policy violation, rule: {}", reaction);
                runResultStatus = ActivityRunResultStatus.HALTING_ERROR;
            } else {
                LOGGER.debug("No action to take for policy violation, rule: {}", reaction);
                continue;
            }

            throw buildException(reaction, action, runResultStatus);
        }
    }

    private ActivityThresholdPolicyViolationException buildException(
            EvaluatedPolicyReaction reaction, ActivityPolicyActionType action, ActivityRunResultStatus resultStatus) {

        EvaluatedActivityPolicyRule rule = reaction.getRule();

        String ruleName = rule.getName();
        String reactionName = reaction.getName();

        LocalizableMessage message = new SingleLocalizableMessage(
                "ActivityPolicyRulesProcessor.policyViolationMessage", new Object[] { ruleName, reactionName });

        String defaultMessage =
                "Policy violation, rule: "
                        + ruleName
                        + (reactionName != null ? "/" + reactionName : "");

        Integer executionAttempt = activityRun.getActivityState().getExecutionAttempt();

        return new ActivityThresholdPolicyViolationException(
                message,
                defaultMessage,
                resultStatus,
                PolicyViolationContextBuilder.from(reaction, action, executionAttempt));
    }
}
