/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.evolveum.midpoint.repo.common.policy.GenericEvaluatedPolicyRule;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * This class is responsible for updating the counters of evaluated activity policy rules.
 * It checks which rules have been triggered and have a threshold, and increments their counters accordingly.
 *
 * TODO: Way to similar to PolicyRuleCounterUpdater located in model.
 *  Consider refactoring to avoid code duplication.
 */
public class ActivityPolicyRuleCounterUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityPolicyRuleCounterUpdater.class);

    private final @NotNull AbstractActivityRun<?, ?, ?> activityRun;

    private final @NotNull Supplier<Collection<GenericEvaluatedPolicyRule>> policyRulesProvider;

    private final Function<String, Integer> alreadyIncrementedValueChecker;

    public ActivityPolicyRuleCounterUpdater(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull Supplier<Collection<GenericEvaluatedPolicyRule>> policyRulesProvider,
            Function<String, Integer> alreadyIncrementedValueChecker) {

        this.activityRun = activityRun;
        this.policyRulesProvider = policyRulesProvider;
        this.alreadyIncrementedValueChecker = alreadyIncrementedValueChecker;
    }

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
        ExecutionSupport.CountersGroup group = activityRun.getCountersGroup();
        Map<String, Integer> currentValues =
                activityRun.incrementCounters(group, rulesToIncrementMap.keySet(), result);
        LOGGER.trace("Updated counters for group {}: {}", group, currentValues);

        // Combine with preexisting values to get total values
        Map<String, Integer> totalValues = computeTotalValues(currentValues);

        // Update the rules with the new counter values
        currentValues.forEach((id, value) ->
                rulesToIncrementMap.get(id).setCount(value, totalValues.get(id)));
    }

    private Map<String, Integer> computeTotalValues(Map<String, Integer> currentValues) {
        var preexistingCounters = activityRun.getActivityPolicyRulesContext().getPreexistingValues().getPreexistingCounters();
        // for each entry in currentValues, add the corresponding value from preexistingValues
        return currentValues.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> ComputationUtil.add(e.getValue(), preexistingCounters.get(e.getKey()))));
    }

    /** Returns rules that should have their counters incremented, in a form of map: ID -> rule. */
    private @NotNull Map<String, GenericEvaluatedPolicyRule> collectRulesToIncrement() {
        Collection<GenericEvaluatedPolicyRule> rules = policyRulesProvider.get();

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

            if (alreadyIncrementedValueChecker != null) {
                Integer alreadyIncrementedValue = alreadyIncrementedValueChecker.apply(rule.getRuleIdentifier().toString());
                if (alreadyIncrementedValue != null) {
                    // todo skip also already incremented, uncomment and fix
//                    rule.setCount(alreadyIncrementedValue);
                    LOGGER.trace("Rule {} already has an incremented value {}, skipping counter update",
                            rule.getRuleIdentifier(), alreadyIncrementedValue);
                    continue;
                }
            }

            LOGGER.trace("Incrementing counter for rule {}", rule.getRuleIdentifier());
            rulesToIncrementMap.put(rule.getRuleIdentifier().toString(), rule);
        }
        return rulesToIncrementMap;
    }
}
