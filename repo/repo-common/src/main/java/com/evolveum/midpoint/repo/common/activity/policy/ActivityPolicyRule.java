/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.util.DebugUtil.*;

import java.util.Set;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleIdentifier;
import com.evolveum.midpoint.schema.config.PolicyRuleConfigItem;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismPrettyPrinter;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityPolicyStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * A policy rule that is being evaluated in a context of given activity.
 *
 * - These objects are created when an activity run ({@link AbstractActivityRun}) starts - in
 * {@link ActivityPolicyRulesCollector#collectRulesAndPreexistingValues(OperationResult)} method.
 *
 * - They are evaluated periodically during the activity run - currently in
 * {@link ActivityPolicyRulesProcessor#evaluateAndExecuteRules(ItemProcessingResult, OperationResult)} method.
 *
 * How it is related to {@link EvaluatedActivityPolicyRuleImpl}:
 *
 * This class represents the rule as defined in the policy, enriched with data such as current state and counters, and persisted
 * in {@link ActivityPolicyRulesContext} throughout an activity run.
 *
 * On the other hand, {@link EvaluatedActivityPolicyRuleImpl} is a lightweight wrapper used only during a single evaluation cycle.
 *
 * TODO think of a better name, since this class is responsible for storing activity policy state (counters, data needs, etc.)
 */
public class ActivityPolicyRule implements DebugDumpable {

    private final @NotNull PolicyRuleConfigItem policyRuleCI;

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

    /**
     * Useful in case when policy rule comes via activity/policyRef -> identifier should be REF_OID:INDUCEMENT_CID.
     */
    private final PolicyRuleIdentifier customPolicyRuleIdentifier;

    public ActivityPolicyRule(
            @NotNull PolicyRuleConfigItem policy,
            @NotNull ActivityPath path,
            PolicyRuleIdentifier  customPolicyRuleIdentifier,
            @NotNull Set<DataNeed> dataNeeds) {
        this.policyRuleCI = policy;
        this.path = path;
        this.customPolicyRuleIdentifier = customPolicyRuleIdentifier;
        this.dataNeeds = dataNeeds;
    }

    public @NotNull PolicyRuleConfigItem getPolicyRuleConfigItem() {
        return policyRuleCI;
    }

    public @NotNull ActivityPath getPath() {
        return path;
    }

    public @NotNull PolicyRuleIdentifier getRuleIdentifier() {
        if (customPolicyRuleIdentifier != null) {
            return customPolicyRuleIdentifier;
        }

        return ActivityPolicyRuleIdentifier.of(getPolicyBean(), path);
    }

    public String getName() {
        return getPolicyBean().getName();
    }

    public Integer getOrder() {
        return getPolicyBean().getOrder();
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

    public @NotNull PolicyRuleType getPolicyBean() {
        return policyRuleCI.value();
    }

    public @NotNull ConfigurationItemOrigin getOrigin() {
        return policyRuleCI.origin();
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
        PrismPrettyPrinter.debugDumpValue(sb, indent + 2, policyRuleCI, PolicyRuleType.COMPLEX_TYPE, PrismContext.LANG_XML);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedActivityPolicyRule{" +
                "policy=" + policyRuleCI.getName() +
                '}';
    }
}
