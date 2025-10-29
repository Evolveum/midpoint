/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.util.DebugUtil.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyType;

/**
 * A policy rule that is being evaluated in a context of given activity.
 *
 * - These objects are created when an activity run ({@link AbstractActivityRun}) starts - in
 * {@link ActivityPolicyRulesProcessor#collectRulesAndPreexistingValues(OperationResult)} method.
 *
 * - They are evaluated periodically during the activity run - currently in
 * {@link ActivityPolicyRulesProcessor#evaluateAndExecuteRules(ItemProcessingResult, OperationResult)} method.
 *
 * == Thresholds
 *
 * A rule may produce a single value (integer or duration) that is used to compare against thresholds defined for individual
 * reactions. This usually happens when the specific constraint has no boundaries defined by itself.
 *
 * TODO implement a check that makes sure that only one constraint without boundaries is defined for a rule.
 * TODO implement a check that makes sure that when thresholds are defined, at least one constraint provides the value.
 */
public class EvaluatedActivityPolicyRule implements EvaluatedPolicyRule, DebugDumpable {

    private final @NotNull ActivityPolicyType policy;

    private final @NotNull ActivityPath path;

    private final List<EvaluatedActivityPolicyRuleTrigger<?>> triggers = new ArrayList<>();

    /** The bean recorded to the activity state (if any). */
    private ActivityPolicyStateType currentState;

    /** Type of the threshold value produced and checked by this rule (if any). */
    private ThresholdValueType thresholdValueType;

    /**
     * The total value (to be checked against the threshold): existing before + computed for the current activity.
     */
    private Object totalValue;

    /**
     * The value (to be checked against the threshold) that was computed for the current activity only (if any).
     *
     * @see EvaluatedPolicyRule#getLocalValue()
     * @see EvaluatedPolicyRule#getTotalValue()
     */
    private Object localValue;

    private final @NotNull Set<DataNeed> dataNeeds;

    public EvaluatedActivityPolicyRule(
            @NotNull ActivityPolicyType policy, @NotNull ActivityPath path, @NotNull Set<DataNeed> dataNeeds) {
        this.policy = policy;
        this.path = path;
        this.dataNeeds = dataNeeds;
    }

    public @NotNull ActivityPath getPath() {
        return path;
    }

    @Override
    public @NotNull ActivityPolicyRuleIdentifier getRuleIdentifier() {
        return ActivityPolicyRuleIdentifier.of(policy, path);
    }

    @Override
    public String getName() {
        return policy.getName();
    }

    @Override
    public Integer getOrder() {
        return policy.getOrder();
    }

    @Override
    public Object getLocalValue() {
        return localValue;
    }

    @Override
    public Object getTotalValue() {
        return totalValue;
    }

    @Override
    public @NotNull ThresholdValueType getThresholdValueType() {
        return thresholdValueType != null ? thresholdValueType : ThresholdValueType.COUNTER;
    }

    @Override
    public void setThresholdTypeAndValues(
            @NotNull ThresholdValueType thresholdValueType, Object localValue, Object totalValue) {
        if (this.thresholdValueType != null && this.thresholdValueType != thresholdValueType) {
            throw new IllegalStateException(
                    "Cannot change threshold value type from " + this.thresholdValueType + " to " + thresholdValueType);
        }

        this.thresholdValueType = thresholdValueType;
        this.localValue = localValue;
        this.totalValue = totalValue;
    }

    @NotNull
    public ActivityPolicyType getPolicy() {
        return policy;
    }

    @Override
    public boolean hasThreshold() {
        return policy.getPolicyReaction().stream()
                .anyMatch(r -> r.getThreshold() != null);
    }

    List<EvaluatedPolicyReaction> getApplicableReactions() {
        return policy.getPolicyReaction().stream()
                .map(r -> new EvaluatedPolicyReaction(this, r))
                .filter(r -> !r.hasThreshold() || r.isWithinThreshold())
                .toList();
    }

    @NotNull
    public List<EvaluatedActivityPolicyRuleTrigger<?>> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<EvaluatedActivityPolicyRuleTrigger<?>> triggers) {
        this.triggers.clear();

        if (triggers != null) {
            this.triggers.addAll(triggers);
        }
    }

    public void setCurrentState(ActivityPolicyStateType currentState) {
        this.currentState = currentState;
    }

    @Override
    public boolean isTriggered() {
        return !triggers.isEmpty() || (currentState != null && !currentState.getTrigger().isEmpty());
    }

    boolean isReactionEnforced(String reactionIdentifier) {
        return currentState != null
                && currentState.getReaction().stream()
                        .anyMatch(r -> reactionIdentifier.equals(r.getRef()) && r.isEnforced());
    }

    /** Does this policy rule need execution time to be evaluated? */
    boolean doesNeedExecutionTime() {
        return dataNeeds.contains(DataNeed.EXECUTION_TIME);
    }

    /** Does this policy rule need execution attempt number to be evaluated? */
    boolean doesNeedExecutionAttemptNumber() {
        return dataNeeds.contains(DataNeed.EXECUTION_ATTEMPTS);
    }

    /** Does this policy rule need counters to be evaluated? Currently not used, we take all counters from the whole tree. */
    public boolean doesUseCounters() {
        return dataNeeds.contains(DataNeed.COUNTERS);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDumpLabelLn(sb, "EvaluatedActivityPolicyRule " + (getName() != null ? getName() + " " : "") + "(triggers: " + triggers.size() + ")", indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, policy, ActivityPolicyType.COMPLEX_TYPE, PrismContext.LANG_XML);
        sb.append('\n');
        debugDumpWithLabelLn(sb, "triggers", triggers, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedActivityPolicyRule{" +
                "policy=" + policy.getName() +
                ", triggers=" + triggers.size() +
                '}';
    }
}
