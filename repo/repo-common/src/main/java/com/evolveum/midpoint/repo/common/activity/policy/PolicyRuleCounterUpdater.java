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
 * Consider refactoring to avoid code duplication, however new set of interfaces for
 * PolicyRulesContext and EvaluatedPolicyRule will be needed.
 */
public class PolicyRuleCounterUpdater {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleCounterUpdater.class);

    private final @NotNull AbstractActivityRun<?, ?, ?> activityRun;

    public PolicyRuleCounterUpdater(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    public void updateCounters(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        ActivityPolicyRulesContext context = activityRun.getActivityPolicyRulesContext();

        List<EvaluatedActivityPolicyRule> rulesToIncrement = new ArrayList<>();
        for (EvaluatedActivityPolicyRule rule : context.getPolicyRules()) {
            if (!rule.isTriggered() || !rule.hasThreshold()) {
                LOGGER.trace("Rule {} is not triggered or does not have a threshold, skipping counter update", rule.getRuleIdentifier());
                continue;
            }

            if (context.getCounter(rule.getRuleIdentifier()) != null) {
                // The counter was already incremented in this run, so we just copy it to the rule.
                Integer counter = context.getCounter(rule.getRuleIdentifier());
                rule.setCount(counter);
                LOGGER.trace("Counter for rule {} was already incremented to {}, copying it to the rule",
                        rule.getRuleIdentifier(), counter);
                continue;
            }

            // The counter was not incremented yet, so we increment it now.
            LOGGER.trace("Incrementing counter for rule {}", rule.getRuleIdentifier());
            rulesToIncrement.add(rule);
        }

        Map<String, EvaluatedActivityPolicyRule> rulesByIdentifier = rulesToIncrement.stream()
                .collect(Collectors.toMap(EvaluatedActivityPolicyRule::getRuleIdentifier, Function.identity()));

        Map<String, Integer> currentValues =
                activityRun.incrementCounters(
                        ExecutionSupport.CountersGroup.ACTIVITY_POLICY_RULES, rulesByIdentifier.keySet(), result);

        currentValues.forEach((id, value) -> {
            rulesByIdentifier.get(id).setCount(value);
            context.setCounter(id, value);
        });
    }
}
