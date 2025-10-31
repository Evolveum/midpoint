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

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyActionsType;
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
 */
public class ActivityPolicyRule implements DebugDumpable {

    private final @NotNull ActivityPolicyType policy;

    private final @NotNull ActivityPath path;

    /** The bean recorded to the activity state (if any). */
    private ActivityPolicyStateType currentState;

    /**
     * The total value (to be checked against the threshold): existing before + computed for the current activity.
     */
    private Integer totalCount;

    /**
     * The value (to be checked against the threshold) that was computed for the current activity only (if any).
     *
     * @see ActivityPolicyRule#getLocalCount()
     * @see ActivityPolicyRule#getTotalCount()
     */
    private Integer localCount;

    private final @NotNull Set<DataNeed> dataNeeds;

    public ActivityPolicyRule(
            @NotNull ActivityPolicyType policy, @NotNull ActivityPath path, @NotNull Set<DataNeed> dataNeeds) {
        this.policy = policy;
        this.path = path;
        this.dataNeeds = dataNeeds;
    }

    public @NotNull ActivityPath getPath() {
        return path;
    }

    public @NotNull ActivityPolicyRuleIdentifier getRuleIdentifier() {
        return ActivityPolicyRuleIdentifier.of(policy, path);
    }

    public String getName() {
        return policy.getName();
    }

    public Integer getOrder() {
        return policy.getOrder();
    }

    /**
     * Local count of the rule, i.e., the count that was computed for the current activity only.
     * It is a part of {@link #getTotalCount()}.
     *
     * @see #getTotalCount()
     */
    public synchronized Integer getLocalCount() {
        return localCount;
    }

    /**
     * Current count that should be used for policy threshold evaluation.
     * This is the result of policy constraint evaluation.
     *
     * For activity trees, this value is the total count for the activity tree, i.e., it contains the relevant data from
     * all activities that were already finished.
     */
    public synchronized Integer getTotalCount() {
        return totalCount;
    }

    public synchronized ActivityPolicyStateType getCurrentState() {
        return currentState;
    }

    public synchronized void setCount(Integer localValue, Integer totalValue) {
        this.localCount = localValue;
        this.totalCount = totalValue;
    }

    public synchronized void setCurrentState(ActivityPolicyStateType currentState) {
        this.currentState = currentState;
    }

    @NotNull
    public ActivityPolicyType getPolicy() {
        return policy;
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
        debugDumpLabelLn(sb, "ActivityPolicyRule " + (getName() != null ? getName() + " " : ""), indent);
        debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        debugDumpLabelLn(sb, "policyRuleType", indent + 1);
        indentDebugDump(sb, indent + 2);
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, policy, ActivityPolicyType.COMPLEX_TYPE, PrismContext.LANG_XML);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedActivityPolicyRule{" +
                "policy=" + policy.getName() +
                '}';
    }

    @NotNull
    public List<ActivityPolicyActionType> getActions() {
        ActivityPolicyActionsType actions = policy.getPolicyActions();
        if (actions == null) {
            return List.of();
        }

        List<ActivityPolicyActionType> result = new ArrayList<>();

        addAction(result, actions.getNotification());
        addAction(result, actions.getRestartActivity());
        addAction(result, actions.getSkipActivity());
        addAction(result, actions.getSuspendTask());

        return result;
    }

    private void addAction(List<ActivityPolicyActionType> actions, ActivityPolicyActionType action) {
        if (action != null) {
            actions.add(action);
        }
    }
}
