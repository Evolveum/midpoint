/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import static com.evolveum.midpoint.task.api.ExecutionSupport.CountersGroup.FULL_EXECUTION_MODE_POLICY_RULES;
import static com.evolveum.midpoint.task.api.ExecutionSupport.CountersGroup.PREVIEW_MODE_POLICY_RULES;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

/**
 * Updates counters for policy rules, with the goal of determining if rules' thresholds have been reached.
 *
 * Currently supported only for object-level rules.
 *
 * Intentionally package-private.
 */
class PolicyRuleCounterUpdater<AH extends AssignmentHolderType> {

    @NotNull private final LensContext<AH> lensContext;
    @NotNull private final Task task;

    PolicyRuleCounterUpdater(@NotNull LensContext<AH> lensContext, @NotNull Task task) {
        this.lensContext = lensContext;
        this.task = task;
    }

    void updateCounters(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        ExecutionSupport executionSupport = task.getExecutionSupport();
        if (executionSupport == null) {
            return;
        }

        /*
         * We update the counters in rules with thresholds in the following ways:
         *
         * 1) If a rule counter was already incremented in this clockwork run, we copy the counter value into the rule.
         * (Regardless of whether it has been triggered during the latest evaluation.)
         *
         * 2) All remaining rules are incremented - if they are triggered.
         *
         * All of this is needed because the rules are quite temporary; they are recreated on each projector run,
         * currently even on each focus iteration. We certainly do not want to increase the counters each time.
         * But we need to have the current counter value in the rules even on further projector runs.
         */

        LensFocusContext<AH> focusContext = lensContext.getFocusContext();
        if (focusContext == null) {
            return;
        }

        List<EvaluatedPolicyRule> rulesToIncrement = new ArrayList<>();
        for (EvaluatedPolicyRuleImpl rule : focusContext.getObjectPolicyRules()) {
            if (!rule.hasThreshold()) {
                continue;
            }
            Integer alreadyIncrementedValue = focusContext.getPolicyRuleCounter(rule.getPolicyRuleIdentifier());
            if (alreadyIncrementedValue != null) {
                rule.setCount(alreadyIncrementedValue);
                continue;
            }

            if (!rule.isTriggered()) {
                continue;
            }
            rulesToIncrement.add(rule);
        }

        if (rulesToIncrement.isEmpty()) {
            return;
        }

        Map<String, EvaluatedPolicyRule> rulesByIdentifier = rulesToIncrement.stream()
                .collect(Collectors.toMap(EvaluatedPolicyRule::getPolicyRuleIdentifier, Function.identity()));

        ExecutionSupport.CountersGroup group =
                executionSupport.getActivityExecutionMode() == ExecutionModeType.FULL ?
                        FULL_EXECUTION_MODE_POLICY_RULES : PREVIEW_MODE_POLICY_RULES;

        Map<String, Integer> currentValues =
                executionSupport.incrementCounters(group, rulesByIdentifier.keySet(), result);

        currentValues.forEach((id, value) -> {
            rulesByIdentifier.get(id).setCount(value);
            focusContext.setPolicyRuleCounter(id, value);
        });
    }
}
