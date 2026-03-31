/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.ABORTED;
import static com.evolveum.midpoint.repo.common.activity.ActivityRunResultStatus.HALTING_ERROR;
import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityPolicyBasedAbortException;
import com.evolveum.midpoint.repo.common.activity.ActivityPolicyBasedHaltException;
import com.evolveum.midpoint.repo.common.activity.policy.evaluator.ActivityCompositeConstraintEvaluator;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunPolicyException;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * This processor is responsible for collecting, evaluating and executing activity policy rules.
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

    public void evaluateAndExecuteRules(ItemProcessingResult processingResult, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, ActivityRunPolicyException {

        LOGGER.trace("Evaluating/executing activity policy rules for activity '{}'", activityRun.getActivityPath());

        Collection<ActivityPolicyRule> rules = getPolicyRulesContext().getPolicyRules();
        if (rules.isEmpty()) {
            return;
        }

        Collection<EvaluatedActivityPolicyRule> evaluatedRules = rules.stream()
                .map(EvaluatedActivityPolicyRule::new)
                .collect(Collectors.toList());

        evaluateRules(evaluatedRules, processingResult, result);

        updateCounters(evaluatedRules, result);

        executeAndStoreState(evaluatedRules, result);
    }

    private void evaluateRules(
            Collection<EvaluatedActivityPolicyRule> rules, ItemProcessingResult processingResult, OperationResult result) {

        for (EvaluatedActivityPolicyRule rule : rules) {
            evaluateRule(rule, processingResult, result);
        }
    }

    private void updateCounters(Collection<EvaluatedActivityPolicyRule> evaluatedRules, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        new ActivityPolicyRuleUpdater(activityRun, evaluatedRules)
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

    private void executeAndStoreState(Collection<EvaluatedActivityPolicyRule> evaluatedRules, OperationResult result)
            throws ObjectNotFoundException, ObjectAlreadyExistsException, SchemaException, ActivityRunPolicyException {

        // These will be written to activity state at the end
        Collection<ActivityPolicyStateType> policyStates = new ArrayList<>();

        try {
            LOGGER.trace("Executing activity policy rules for {} ({})",
                    activityRun.getActivity().getIdentifier(), activityRun.getActivityPath());

            for (EvaluatedActivityPolicyRule rule : evaluatedRules) {
                if (!rule.isTriggered()) {
                    LOGGER.trace("Policy rule {} was not triggered, not executing actions", rule);
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
            // update policy states after all rules were enforced (even if ThresholdPolicyViolationException was thrown)
            Map<String, EvaluatedActivityPolicyRule> ruleMap = evaluatedRules.stream()
                    .collect(Collectors.toMap(r -> r.getRuleIdentifier().toString(), r -> r));

            Map<String, ActivityPolicyStateType> updated = activityRun.updateActivityPolicyState(policyStates, result);
            updated.forEach((id, state) -> ruleMap.get(id).setCurrentState(state));
        }
    }

    private void evaluateRule(
            EvaluatedActivityPolicyRule rule, ItemProcessingResult processingResult, OperationResult result) {

        LOGGER.trace("Starting evaluation of rule {}, name: {}", rule.getRuleIdentifier(), rule.getName());

        JAXBElement<PolicyConstraintsType> element = createRootConstraintElement(rule.getPolicy());

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

    private static JAXBElement<PolicyConstraintsType> createRootConstraintElement(PolicyRuleType policyBean) {
        return new JAXBElement<>(
                PolicyConstraintsType.F_AND,
                PolicyConstraintsType.class,
                policyBean.getPolicyConstraints());
    }

    private void executeActions(EvaluatedActivityPolicyRule rule, OperationResult result) throws ActivityRunPolicyException {

        for (PolicyActionType action : rule.getActions()) {
            if (action instanceof NotificationPolicyActionType) {
                LOGGER.debug("Sending notification because of policy violation, rule: {}", rule);

                activityRun.sendActivityPolicyRuleTriggeredEvent(rule, result);
                continue;
            }

            String ruleName = rule.getName();

            String defaultMessage = "Policy violation, rule: " + ruleName;

            LocalizableMessage message = new SingleLocalizableMessage(
                    "ActivityPolicyRulesProcessor.policyViolationMessage", new Object[] { ruleName }, defaultMessage);

            if (action instanceof RestartActivityPolicyActionType || action instanceof SkipActivityPolicyActionType) {
                LOGGER.debug("Aborting activity because of policy violation, rule: {}", rule);
                var abortInfo = new ActivityAbortingInformationType()
                        .activityPath(rule.getPath().toBean())
                        .policyAction(action.clone());
                var cause = new ActivityPolicyBasedAbortException(message, defaultMessage, abortInfo);
                throw new ActivityRunPolicyException(defaultMessage, FATAL_ERROR, ABORTED, cause);
            } else if (action instanceof SuspendTaskPolicyActionType) {
                LOGGER.debug("Suspending task because of policy violation, rule: {}", rule);
                var cause = new ActivityPolicyBasedHaltException(message, defaultMessage);
                throw new ActivityRunPolicyException(defaultMessage, FATAL_ERROR, HALTING_ERROR, cause);
            } else {
                LOGGER.debug("No action to take for policy violation, rule: {}", rule);
            }
        }
    }
}
