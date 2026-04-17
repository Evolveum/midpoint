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
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.policy.GenericEvaluatedPolicyRule;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

/**
 * This class is responsible for updating the counters of evaluated activity policy rules.
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
    protected abstract Collection<? extends GenericEvaluatedPolicyRule> getPolicyRules();

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

        // Combine with preexisting values to get total values
        Map<String, Integer> totalValues = computeTotalValues(executionSupport, currentValues);

        // Update the rules with the new counter values
        currentValues.forEach((id, value) -> {
            rulesToIncrementMap.get(id).setCount(value, totalValues.get(id));
            storeIncrementedPolicyRuleCounter(id, value);
        });
    }

    private Map<String, Integer> computeTotalValues(ExecutionSupport executionSupport, Map<String, Integer> currentValues) {
        if (!(executionSupport instanceof AbstractActivityRun<?, ?, ?> aar)) {
            return currentValues;
        }

        var preexistingCounters = aar.getActivityPolicyRulesContext().getPreexistingValues().getPreexistingCounters();
        // for each entry in currentValues, add the corresponding value from preexistingValues
        return currentValues.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> ComputationUtil.add(e.getValue(), preexistingCounters.get(e.getKey()))));
    }

    /** Returns rules that should have their counters incremented, in a form of map: ID -> rule. */
    private @NotNull Map<String, GenericEvaluatedPolicyRule> collectRulesToIncrement() {
        Collection<? extends GenericEvaluatedPolicyRule> rules = getPolicyRules();

        Map<String, GenericEvaluatedPolicyRule> rulesToIncrementMap = new HashMap<>();
        for (GenericEvaluatedPolicyRule rule : rules) {
            if (!rule.isTriggered()) {
                LOGGER.trace("Rule {} is not triggered, skipping counter update", rule.getRuleIdentifier());
                continue;
            }

            if (!rule.hasThreshold()) {
                LOGGER.trace("Rule {} does not have a threshold, skipping counter update", rule.getRuleIdentifier());
                continue;
            }

            Integer alreadyIncrementedValue = getIncrementedPolicyRuleCounter(rule.getRuleIdentifier().asString());
            if (alreadyIncrementedValue != null) {
                // todo skip also already incremented, uncomment and fix
//                    rule.setCount(alreadyIncrementedValue);
                LOGGER.trace("Rule {} already has an incremented value {}, skipping counter update",
                        rule.getRuleIdentifier(), alreadyIncrementedValue);
                continue;
            }

            LOGGER.trace("Incrementing counter for rule {}", rule.getRuleIdentifier());
            rulesToIncrementMap.put(rule.getRuleIdentifier().asString(), rule);
        }
        return rulesToIncrementMap;
    }
}
