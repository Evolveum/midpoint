/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

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
 * Consider refactoring to avoid code duplication, however new set of interfaces for
 * PolicyRulesContext and EvaluatedPolicyRule will be needed.
 */
public abstract class PolicyRuleCounterUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleCounterUpdater.class);

    private final @NotNull ExecutionSupport executionSupport;

    PolicyRuleCounterUpdater(@NotNull ExecutionSupport executionSupport) {
        this.executionSupport = executionSupport;
    }

    protected abstract PolicyRulesContext<?> getPolicyRulesContext();

    protected abstract ExecutionSupport.CountersGroup getCountersGroup();

    void updateCounters(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        PolicyRulesContext<?> context = getPolicyRulesContext();

        List<EvaluatedPolicyRule> rulesToIncrement = new ArrayList<>();
        for (EvaluatedPolicyRule rule : context.getPolicyRules()) {
            if (!rule.isTriggered()) {
                LOGGER.trace("Rule {} is not triggered, skipping counter update", rule.getRuleIdentifier());
                continue;
            }

            if (rule.getThresholdValueType() != ThresholdValueType.COUNTER) {
                LOGGER.trace("Rule {} does not have a threshold against counters, skipping counter update", rule.getRuleIdentifier());
                continue;
            }

            Integer counter = context.getCounter(rule.getRuleIdentifier());
            if (counter != null) {
                // The counter was already incremented in this run, so we just copy it to the rule.
                // FIXME shouldn't we put the rule to "rulesToIncrement", or increment the counter in some way?
                rule.setThresholdValueType(ThresholdValueType.COUNTER, counter);
                LOGGER.trace("Counter for rule {} was already incremented to {}, copying it to the rule",
                        rule.getRuleIdentifier(), counter);
                continue;
            }

            // The counter was not incremented yet, so we increment it now.
            LOGGER.trace("Incrementing counter for rule {}", rule.getRuleIdentifier());
            rulesToIncrement.add(rule);
        }

        Map<String, EvaluatedPolicyRule> rulesToIncrementByIdentifier = rulesToIncrement.stream()
                .collect(Collectors.toMap(EvaluatedPolicyRule::getRuleIdentifier, Function.identity()));

        ExecutionSupport.CountersGroup group = getCountersGroup();

        Map<String, Integer> currentValues =
                executionSupport.incrementCounters(group, rulesToIncrementByIdentifier.keySet(), result);
        LOGGER.trace("Updated counters for group {}: {}", group, currentValues);

        currentValues.forEach((id, value) -> {
            rulesToIncrementByIdentifier.get(id).setThresholdValueType(ThresholdValueType.COUNTER, value);
            context.setCounter(id, value);
        });
    }
}
