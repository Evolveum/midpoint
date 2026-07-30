/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import static com.evolveum.midpoint.model.api.ModelExecuteOptions.isPreviewPolicyRulesEnforcement;
import static com.evolveum.midpoint.repo.common.policy.TriggerPresentationUtil.MessageKind.NORMAL;
import static com.evolveum.midpoint.repo.common.policy.TriggerPresentationUtil.extractMessages;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.notifications.api.PolicyRuleNotificationPublisher;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.DirectlyEvaluatedClockworkPolicyRule;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.repo.common.activity.ActivityPolicyBasedAbortException;
import com.evolveum.midpoint.repo.common.activity.ActivityPolicyBasedHaltException;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.repo.common.policy.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.schema.config.PolicyActionConfigItem;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Code used to enforce the policy rules that have the `enforce` and `suspendTask` actions.
 *
 * An interesting difference is the execution in the preview mode: the former are only simulated, whereas the latter
 * do really throw an exception. This behavior may change in the future.
 *
 * Originally this was a regular {@link ChangeHook}. However, when invoked among other hooks, it is too late (see MID-4797).
 * So we had to convert it into regular code and run it right after the first {@link Projector} run.
 *
 * Intentionally package-private.
 *
 * @author semancik
 */
class PolicyRuleEnforcer<O extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleEnforcer.class);

    @NotNull private final LensContext<O> context;
    /**
     * Execution support might be null for policies enforced outsize real task execution (transient/lightweight tasks)
     */
    private final ExecutionSupport executionSupport;
    @NotNull private final List<LocalizableMessage> messages = new ArrayList<>();
    @NotNull private final List<EvaluatedPolicyRuleType> rulesBeans = new ArrayList<>();

    PolicyRuleEnforcer(@NotNull LensContext<O> context, ExecutionSupport executionSupport) {
        this.context = context;
        this.executionSupport = executionSupport;
    }

    public void enforce(Task task, OperationResult result) throws PolicyViolationException, ConfigurationException {
        enforceRulesWithoutThresholds(result);
        enforceThresholds(task, result);
    }

    private void enforceRulesWithoutThresholds(OperationResult result)
            throws PolicyViolationException {
        if (!context.hasFocusOfType(AssignmentHolderType.class)) { // temporary - what about projection-only rules?
            result.recordNotApplicable();
            return;
        }

        computeEnforcementForFocusRules();
        computeEnforcementForAssignmentRules();
        // TODO projection rules

        if (isEnforcementPreviewMode()) {
            enforceInPreviewMode();
        } else {
            enforceInRegularMode();
        }
    }

    private boolean isEnforcementPreviewMode() {
        return isPreviewPolicyRulesEnforcement(context.getOptions());
    }

    private void enforceInPreviewMode() {
        PolicyRuleEnforcerPreviewOutputType output = new PolicyRuleEnforcerPreviewOutputType();
        output.getRule().addAll(rulesBeans);
        context.setPolicyRuleEnforcerPreviewOutput(output);
    }

    private void enforceInRegularMode()
            throws PolicyViolationException {
        if (!messages.isEmpty()) {
            LocalizableMessage message = new LocalizableMessageListBuilder()
                    .messages(messages)
                    .separator(LocalizableMessageList.SEMICOLON)
                    .buildOptimized();
            throw ModelCommonBeans.get().localizationService.translate(
                    new PolicyViolationException(message));
        }
    }

    private void computeEnforcementForFocusRules() {
        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext != null) {
            computeEnforcementForTriggeredRules(focusContext.getObjectPolicyRules());
        }
    }

    private void computeEnforcementForAssignmentRules() {
        DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = context.getEvaluatedAssignmentTriple();
        if (evaluatedAssignmentTriple != null) {
            // We do not need to consider foreign policy rules here, as they are enforced on their respective primary hosts.
            evaluatedAssignmentTriple.simpleAccept(
                    assignment -> computeEnforcementForTriggeredRules(assignment.getAllTargetsPolicyRules()));
        }
    }

    private void computeEnforcementForTriggeredRules(Collection<? extends DirectlyEvaluatedClockworkPolicyRule> policyRules) {
        for (DirectlyEvaluatedClockworkPolicyRule policyRule : policyRules) {

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

            rulesBeans.addAll(
                    // TODO really include assignments content?
                    policyRule.toEvaluatedPolicyRuleBeans(
                            new PolicyRuleExternalizationOptions(
                                    FULL,
                                    true,
                                    policyRule.getRelevantTriggersFilter()),
                            t -> enforceAll || t.isEnforcementOverride()));

            List<TreeNode<LocalizableMessage>> messageTrees = extractMessages(enforcingTriggers, NORMAL);
            for (TreeNode<LocalizableMessage> messageTree : messageTrees) {
                messages.add(messageTree.getUserObject());
            }
        }
    }

    private ActivityPath getActivityPath(DirectlyEvaluatedClockworkPolicyRule rule) {
        ActivityPath path = rule.getActivityPath();
        if (path != null) {
            return path;
        }

        if (executionSupport != null) {
            return executionSupport.getActivityPath();
        }

        return ActivityPath.empty();
    }

    private void enforceThresholds(Task task, OperationResult result)
            throws PolicyViolationException {
        if (isEnforcementPreviewMode()) {
            return; // preview should be already recorded
        }
        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext != null) {
            for (DirectlyEvaluatedClockworkPolicyRule policyRule : focusContext.getObjectPolicyRules()) {
                if (!policyRule.isOverThreshold()) {
                    continue;
                }

                LocalizableMessage message = createViolationMessage(policyRule);
                String defaultMessage = ModelCommonBeans.get().localizationService
                        .translate(message, Locale.getDefault());

                for (PolicyActionConfigItem<?> actionCI : policyRule.getEnabledActions()) {
                    PolicyActionType action = actionCI.value();

                    if (action instanceof SuspendTaskPolicyActionType) {
                        enforceNotificationAction(policyRule, "suspend task", task, result);

                        LOGGER.debug("Going to suspend the task because of policy violation, rule: {}", policyRule);
                        var cause = new ActivityPolicyBasedHaltException(message, defaultMessage);

                        throw new ThresholdPolicyViolationException(message, defaultMessage, cause);
                    } else if (action instanceof RestartActivityPolicyActionType || action instanceof SkipActivityPolicyActionType) {
                        enforceNotificationAction(policyRule, "skip/restart activity", task, result);

                        LOGGER.debug("Going to abort the activity because of policy violation, rule: {}", policyRule);

                        var activityPath = getActivityPath(policyRule);

                        var abortInfo = new ActivityAbortingInformationType()
                                .activityPath(activityPath != null ? activityPath.toBean() : null)
                                .policyAction(action.clone());
                        var cause = new ActivityPolicyBasedAbortException(message, defaultMessage, abortInfo);

                        throw new ThresholdPolicyViolationException(message, defaultMessage, cause);
                    }
                }
            }
        }
    }

    /**
     * Builds a human-readable, localizable violation message. The trigger messages are collected the same way the
     * non-threshold enforcement path does (see {@link #computeEnforcementForTriggeredRules}), which avoids dumping
     * the whole {@link PolicyRuleType} bean into the operation execution.
     *
     * The result is "Policy rule '{name}' violation: {triggers}", or - if no trigger carries a message - just
     * "Policy rule '{name}' violation" (a separate localization key, as there is no trigger argument to render).
     */
    private LocalizableMessage createViolationMessage(DirectlyEvaluatedClockworkPolicyRule policyRule) {
        String ruleName = Objects.requireNonNullElse(policyRule.getName(), "Unnamed policy rule");
        List<LocalizableMessage> triggerMessages = extractMessages(policyRule.getTriggers(), NORMAL).stream()
                .map(TreeNode::getUserObject)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (triggerMessages.isEmpty()) {
            return new SingleLocalizableMessage(
                    "PolicyRuleEnforces.policyViolationMessageWithoutTriggers", new Object[] { ruleName });
        }

        LocalizableMessage triggers = new LocalizableMessageListBuilder()
                .messages(triggerMessages)
                .separator(LocalizableMessageList.SEMICOLON)
                .buildOptimized();

        return new SingleLocalizableMessage(
                "PolicyRuleEnforces.policyViolationMessage", new Object[] { ruleName, triggers });
    }

    private void enforceNotificationAction(
            DirectlyEvaluatedClockworkPolicyRule policyRule, String realAction, Task task, OperationResult result) {

        boolean shouldSendNotification = policyRule.containsEnabledAction(NotificationPolicyActionType.class);
        if (!shouldSendNotification) {
            return;
        }

        // such notification would not be emitted via NotificationHook

        PolicyRuleNotificationPublisher notificationPublisher = context.getModelBeans().policyRuleNotificationPublisher;
        if (notificationPublisher != null) {
            LOGGER.debug("Forcing notifications action for policy rule {} before enforcing {} action.", policyRule, realAction);
            notificationPublisher.emitPolicyRulesEvents(context, task, result);
        } else {
            LOGGER.debug("Cannot force notifications action for policy rule {} before enforcing {} action, "
                    + "because notification publisher is not available.", policyRule, realAction);
        }
    }
}
