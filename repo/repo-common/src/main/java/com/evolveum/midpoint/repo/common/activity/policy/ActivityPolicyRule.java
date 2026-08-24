/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.util.DebugUtil.*;

import java.util.Set;

import com.evolveum.midpoint.schema.policy.PlainPolicyRuleIdentifier;
import com.evolveum.midpoint.schema.policy.PolicyRuleIdentifier;
import com.evolveum.midpoint.schema.config.PolicyRuleConfigItem;

import com.evolveum.midpoint.schema.util.task.ActivityPolicyRuleIdentifier;

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
 * An activity policy rule that lives withing specific activity run. It wraps an activity policy rule
 * in {@link #policyRuleCI} and provides additional information relevant for its (repeated) evaluation.
 *
 * === Lifecycle
 *
 * - These objects are created when an activity run ({@link AbstractActivityRun}) starts - in
 * {@link ActivityPolicyRulesCollector#collectRulesAndPreexistingValues(OperationResult)} method.
 *
 * - Rules they contain are evaluated periodically during the activity run - currently in
 * {@link ActivityPolicyRulesProcessor#evaluateAndExecuteRules(ItemProcessingResult, OperationResult)} method.
 *
 * === How it is related to {@link EvaluatedActivityPolicyRuleImpl}?
 *
 * - This class represents the rule as defined in the policy, enriched with the state that spans the whole activity run
 * (currently: the recorded {@link ActivityPolicyStateType}), and persisted in {@link ActivityPolicyRulesContext}
 * throughout that run.
 *
 * - On the other hand, {@link EvaluatedActivityPolicyRuleImpl} is a lightweight wrapper used only during a single evaluation cycle.
 *
 * There is exactly one instance of this class per activity run, so it is shared by all the worker threads of that
 * activity. Hence, only the state that really belongs to the whole run may be kept here; anything belonging to a single
 * evaluation (namely the counts a threshold is checked against) lives in {@link EvaluatedActivityPolicyRuleImpl}.
 *
 * TODO choose a better name, maybe "ActivityPolicyRuleInActivityRun"?
 *
 * @see ActivityPolicyRulesContext
 */
public class ActivityPolicyRule implements DebugDumpable {

    private final @NotNull PolicyRuleConfigItem policyRuleCI;

    private final @NotNull ActivityPath path;

    /** The bean recorded to the activity state (if any). */
    private ActivityPolicyStateType currentState;

    private final @NotNull Set<DataNeed> dataNeeds;

    /**
     * Either
     *
     * - {@link ActivityPolicyRuleIdentifier} (activityPath:ruleCID) for embedded rules
     * - or {@link PlainPolicyRuleIdentifier} (objectOID:ruleCID) for referenced rules and virtual assignments
     */
    private final PolicyRuleIdentifier policyRuleIdentifier;

    public ActivityPolicyRule(
            @NotNull PolicyRuleConfigItem policy,
            @NotNull ActivityPath path,
            @NotNull PolicyRuleIdentifier policyRuleIdentifier,
            @NotNull Set<DataNeed> dataNeeds) {
        this.policyRuleCI = policy;
        this.path = path;
        this.policyRuleIdentifier = policyRuleIdentifier;
        this.dataNeeds = dataNeeds;
    }

    public @NotNull PolicyRuleConfigItem getPolicyRuleConfigItem() {
        return policyRuleCI;
    }

    public @NotNull ActivityPath getPath() {
        return path;
    }

    public @NotNull PolicyRuleIdentifier getRuleIdentifier() {
        return policyRuleIdentifier;
    }

    public String getName() {
        return getPolicyBean().getName();
    }

    public Integer getOrder() {
        return getPolicyBean().getOrder();
    }

    public synchronized ActivityPolicyStateType getCurrentState() {
        return currentState;
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
        return "ActivityPolicyRule{" +
                "policy=" + policyRuleCI.getName() +
                '}';
    }
}
