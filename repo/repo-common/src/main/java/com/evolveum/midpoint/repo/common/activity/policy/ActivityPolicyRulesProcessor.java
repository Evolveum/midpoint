/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus;
import com.evolveum.midpoint.repo.common.activity.ActivityThresholdPolicyViolationException;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ActivityCompositeConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
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

    /**
     * Collects all activity policy rules from the activity and its parent activities.
     * Collects also preexisting (initial) values for individual constraints.
     *
     * TODO Currently returns "doubled" policies when activity has embedded child activities (e.g. reconciliation).
     *  Reason is that embedded activities do inherit definition from parent activity (if there's no tailoring in place).
     */
    public void collectRulesAndPreexistingValues(OperationResult result) throws SchemaException, ObjectNotFoundException {
        List<EvaluatedActivityPolicyRule> rules = collectRulesFromActivity(activityRun.getActivity());
        getPolicyRulesContext().setPolicyRules(rules);

        LOGGER.trace("Found {} activity policy rules for activity hierarchy, activity: '{}'",
                rules.size(), activityRun.getActivityPath());

        PreexistingValues preexistingValues = PreexistingValues.determine(activityRun, rules, result);
        getPolicyRulesContext().setPreexistingValues(preexistingValues);

        LOGGER.trace("Determined preexisting values for activity policy rules:\n{}", preexistingValues.debugDumpLazily(1));
    }

    /**
     * Collects all policy rules from the given activity and its parent activities recursively.
     *
     * Rules from parent activities are included because otherwise they would only be validated
     * in-between child activities, which might be too infrequent (e.g., for execution time policies).
     * By collecting rules from the entire activity hierarchy, we ensure that parent rules are
     * enforced as often as necessary.
     *
     * @param activity The activity from which to start collecting policy rules (null to stop).
     * @return List of evaluated activity policy rules, ordered by their defined order.
     */
    private List<EvaluatedActivityPolicyRule> collectRulesFromActivity(@Nullable Activity<?, ?> activity) {
        if (activity == null) {
            return List.of();
        }

        var rules = new ArrayList<>(collectRulesFromActivity(activity.getParent()));

        ActivityPath activityPath = activity.getPath();
        ActivityPoliciesType activityPoliciesBean = activity.getDefinition().getPoliciesDefinition().getPolicies();
        List<ActivityPolicyType> policyBeans = activityPoliciesBean.getPolicy();

        policyBeans.stream()
                .map(policyBean -> new EvaluatedActivityPolicyRule(policyBean, activityPath, getDataNeeds(policyBean)))
                .sorted(
                        Comparator.comparing(
                                EvaluatedActivityPolicyRule::getOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(r -> rules.add(r));

        LOGGER.trace("Found {} activity policy rules for activity '{}' (including ancestors)", rules.size(), activityPath);

        return rules;
    }

    private static Set<DataNeed> getDataNeeds(ActivityPolicyType policyBean) {
        return ActivityCompositeConstraintEvaluator.get().getDataNeeds(createRootConstraintElement(policyBean));
    }

    public void evaluateAndExecuteRules(ItemProcessingResult processingResult, @NotNull OperationResult result)
            throws ThresholdPolicyViolationException, SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        LOGGER.trace("Evaluating/executing activity policy rules for activity '{}'", activityRun.getActivityPath());

        Collection<EvaluatedActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();
        if (rules.isEmpty()) {
            return;
        }

        rules.forEach(rule -> rule.clearEvaluation());

        evaluateRules(processingResult, result);

        updateCounters(result);

        executeAndStoreState(result);
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

    private ActivityPolicyStateType createPolicyState(EvaluatedActivityPolicyRule rule) {
        ActivityPolicyStateType state = new ActivityPolicyStateType();
        state.setIdentifier(rule.getRuleIdentifier().toString());
        state.setName(rule.getName());

        rule.getTriggers().stream()
                .map(t -> t.toPolicyTriggerType())
                .forEach(t -> state.getTrigger().add(t));

        return state;
    }

    private void executeAndStoreState(OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, ThresholdPolicyViolationException {

        // These will be written to activity state at the end
        Collection<ActivityPolicyStateType> policyStates = new ArrayList<>();

        try {
            LOGGER.trace("Enforcing activity policy rules for {} ({})",
                    activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

            Collection<EvaluatedActivityPolicyRule> rules = activityRun.getActivityPolicyRulesContext().getPolicyRules();

            for (EvaluatedActivityPolicyRule rule : rules) {
                if (!rule.isTriggered()) {
                    LOGGER.trace("Policy rule {} was not triggered, not executing reactions", rule);
                    continue;
                }

                if (rule.hasThreshold() && !rule.isOverThreshold()) {
                    LOGGER.trace("Policy rule {} was triggered, still not yet over threshold, therefore not executing actions", rule);
                    continue;
                }

                policyStates.add(createPolicyState(rule));

                executeActions(rule, result);
            }
        } finally {
            Collection<EvaluatedActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();

            // update policy states after all rules were enforced (even if ThresholdPolicyViolationException was thrown)
            Map<String, EvaluatedActivityPolicyRule> ruleMap = rules.stream()
                    .collect(Collectors.toMap(r -> r.getRuleIdentifier().toString(), r -> r));

            Map<String, ActivityPolicyStateType> updated = activityRun.updateActivityPolicyState(policyStates, result);
            updated.forEach((id, state) -> ruleMap.get(id).setCurrentState(state));
        }
    }

    private void evaluateRule(
            EvaluatedActivityPolicyRule rule, ItemProcessingResult processingResult, OperationResult result) {

        LOGGER.trace("Starting evaluation of rule {}, name: {}", rule.getRuleIdentifier(), rule.getName());

        JAXBElement<ActivityPolicyConstraintsType> element = createRootConstraintElement(rule.getPolicy());

        ActivityPolicyRuleEvaluationContext context = new ActivityPolicyRuleEvaluationContext(rule, activityRun, processingResult);

        ActivityCompositeConstraintEvaluator evaluator = ActivityCompositeConstraintEvaluator.get();

        List<ActivityCompositeTrigger> compositeTriggers = evaluator.evaluate(element, context, result);
        ActivityCompositeTrigger compositeTrigger = MiscUtil.extractSingleton(compositeTriggers);
        if (compositeTrigger != null && !compositeTrigger.getInnerTriggers().isEmpty()) {
            rule.setTriggers(List.copyOf(compositeTrigger.getInnerTriggers()));
        }

        LOGGER.trace("Completed evaluation of rule {}: triggered={}, triggers={}",
                rule.getRuleIdentifier(), rule.isTriggered(), rule.getTriggers());
    }

    private static JAXBElement<ActivityPolicyConstraintsType> createRootConstraintElement(ActivityPolicyType policyBean) {
        return new JAXBElement<>(
                ActivityPolicyConstraintsType.F_AND,
                ActivityPolicyConstraintsType.class,
                policyBean.getPolicyConstraints());
    }

    private void executeActions(EvaluatedActivityPolicyRule rule, OperationResult result)
            throws ActivityThresholdPolicyViolationException {

        for (ActivityPolicyActionType action : rule.getActions()) {
            if (action instanceof NotificationActivityPolicyActionType na) {
                // todo fix single execution check + state has to be loaded from somewhere
//                if (BooleanUtils.isTrue(na.isSingle()) && reaction.isEnforced()) {
//                    LOGGER.debug("Skipping notification action (single) because it was already enforced, rule: {}", rule);
//                    continue;
//                }

                LOGGER.debug("Sending notification because of policy violation, rule: {}", rule);

                activityRun.sendActivityPolicyRuleTriggeredEvent(rule, result);
                continue;
            }

            ActivityRunResultStatus runResultStatus;
            if (action instanceof RestartActivityPolicyActionType) {
                LOGGER.debug("Restarting activity because of policy violation, rule: {}", rule);
                runResultStatus = ActivityRunResultStatus.RESTART_ACTIVITY_ERROR;
            } else if (action instanceof SkipActivityPolicyActionType) {
                LOGGER.debug("Skipping activity because of policy violation, rule: {}", rule);
                runResultStatus = ActivityRunResultStatus.SKIP_ACTIVITY_ERROR;
            } else if (action instanceof SuspendTaskActivityPolicyActionType) {
                LOGGER.debug("Suspending task because of policy violation, rule: {}", rule);
                runResultStatus = ActivityRunResultStatus.HALTING_ERROR;
            } else {
                LOGGER.debug("No action to take for policy violation, rule: {}", rule);
                continue;
            }

            throw buildException(rule, action, runResultStatus);
        }
    }

    private ActivityThresholdPolicyViolationException buildException(
            EvaluatedActivityPolicyRule rule, ActivityPolicyActionType action, ActivityRunResultStatus resultStatus) {

        String ruleName = rule.getName();
        String reactionName = rule.getName();

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
                PolicyViolationContextBuilder.from(rule, action, executionAttempt));
    }
}
