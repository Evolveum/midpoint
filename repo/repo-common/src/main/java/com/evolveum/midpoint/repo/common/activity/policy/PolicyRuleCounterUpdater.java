/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import static com.evolveum.midpoint.task.api.ExecutionSupport.CountersGroup.FULL_EXECUTION_MODE_POLICY_RULES;
import static com.evolveum.midpoint.task.api.ExecutionSupport.CountersGroup.PREVIEW_MODE_POLICY_RULES;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRule;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

/**
 * This class is responsible for updating the counters of evaluated activity and focus policy rules.
 * It checks which rules have been triggered and have a threshold, and increments their counters accordingly.
 */
public abstract class PolicyRuleCounterUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleCounterUpdater.class);

    /**
     * Returns the already incremented counter value for the given rule identifier,
     * or null if it has not been incremented yet.
     */
    protected Integer getIncrementedPolicyRuleCounter(String ruleIdentifier) {
        return null;
    }

    /**
     * Callback method that can be used to store/cache counters.
     */
    protected void storeIncrementedPolicyRuleCounter(String ruleIdentifier, Integer counter) {
        // intentionally left empty
    }

    /**
     * Returns list of evaluated policy rules that should be considered for counter updates.
     */
    @NotNull
    protected abstract Collection<? extends EvaluatedPolicyRule> getPolicyRules();

    /**
     * Returns execution support (instance of activity run) that is used to handle
     * counter updates and to determine the execution mode (production vs simulation)
     */
    @NotNull
    protected abstract ExecutionSupport getExecutionSupport();

    /** Updates counters for the triggered "counter-style" rules in repo (activity state) and in memory (rules themselves). */
    public void updateCounters(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        // Collect rules that need their counters incremented
        var rulesToIncrementMap = collectRulesToIncrement(); // Map: ID -> rule.
        if (rulesToIncrementMap.isEmpty()) {
            LOGGER.trace("No rules to increment counters for");
            return;
        }

        // Increment the counters (stored in the activity state, usually the current one or parent one)
        // Should we update production or simulation counters?
        ExecutionSupport executionSupport = getExecutionSupport();

        ExecutionSupport.CountersGroup group =
                executionSupport.getActivityExecutionMode() == ExecutionModeType.FULL ?
                        FULL_EXECUTION_MODE_POLICY_RULES : PREVIEW_MODE_POLICY_RULES;
        Map<String, Integer> currentValues =
                executionSupport.incrementCounters(group, rulesToIncrementMap.keySet(), result);
        LOGGER.trace("Updated counters for group {}: {}", group, currentValues);

        // Update the rules with the new counter values
        currentValues.forEach((id, localValue) -> {
            setCount(rulesToIncrementMap.get(id), id, localValue);
            storeIncrementedPolicyRuleCounter(id, localValue);
        });
    }

    /**
     * Sets the local and the total count on the rule, deriving the total from the local one. Only the local value is
     * ever known by the caller: it is what {@link ExecutionSupport#incrementCounters} returns, and it is what
     * {@link #storeIncrementedPolicyRuleCounter(String, Integer)} caches.
     */
    private void setCount(@NotNull EvaluatedPolicyRule rule, String ruleIdentifier, Integer localValue) {
        rule.setCount(localValue, computeTotalValue(ruleIdentifier, localValue));
    }

    /**
     * The total value to be checked against the threshold: the value computed for the current activity (the local one)
     * plus the value that already existed before this activity run started - i.e. the counts contributed by the other
     * activities of the tree. See {@link PreexistingValues}.
     */
    private Integer computeTotalValue(String ruleIdentifier, Integer localValue) {
        if (!(getExecutionSupport() instanceof AbstractActivityRun<?, ?, ?> aar)) {
            return localValue;
        }

        var preexistingCounters = aar.getActivityPolicyRulesContext().getPreexistingValues().getPreexistingCounters();
        return ComputationUtil.add(localValue, preexistingCounters.get(ruleIdentifier));
    }

    /** Returns rules that should have their counters incremented, in a form of map: ID -> rule. */
    private @NotNull Map<String, EvaluatedPolicyRule> collectRulesToIncrement() {
        Collection<? extends EvaluatedPolicyRule> rules = getPolicyRules();

        Map<String, EvaluatedPolicyRule> rulesToIncrementMap = new HashMap<>();
        for (EvaluatedPolicyRule rule : rules) {
            if (!rule.isTriggered()) {
                LOGGER.trace("Rule {} is not triggered, skipping counter update", rule.getRuleIdentifier());
                continue;
            }

            if (!rule.hasThreshold()) {
                LOGGER.trace("Rule {} does not have a threshold, skipping counter update", rule.getRuleIdentifier());
                continue;
            }

            String ruleIdentifier = rule.getRuleIdentifier().asString();
            Integer alreadyIncrementedValue = getIncrementedPolicyRuleCounter(ruleIdentifier);
            if (alreadyIncrementedValue != null) {
                // The counter was already incremented for this rule during this evaluation, so we must not increment it
                // again. We only restore the counts on the rule. The cached value is the local one, hence the total has
                // to be derived from it the same way as on the incrementing path above.
                setCount(rule, ruleIdentifier, alreadyIncrementedValue);
                LOGGER.trace("Rule {} already has an incremented value {}, skipping counter update",
                        rule.getRuleIdentifier(), alreadyIncrementedValue);
                continue;
            }

            LOGGER.trace("Incrementing counter for rule {}", rule.getRuleIdentifier());
            rulesToIncrementMap.put(ruleIdentifier, rule);
        }
        return rulesToIncrementMap;
    }
}
