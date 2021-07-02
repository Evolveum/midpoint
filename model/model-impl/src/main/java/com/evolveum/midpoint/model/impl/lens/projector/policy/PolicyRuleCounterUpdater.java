/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.repo.common.activity.execution.ActivityExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ExecutionContext;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.evolveum.midpoint.repo.common.activity.execution.ActivityCountersGroup.POLICY_RULES;

/**
 * Updates counters for policy rules, with the goal of determining if rules' thresholds have been reached.
 *
 * Currently supported only for object-level rules.
 */
@Component
@ProcessorExecution(focusRequired = true, focusType = AssignmentHolderType.class)
public class PolicyRuleCounterUpdater implements ProjectorProcessor {

    @ProcessorMethod
    public <AH extends AssignmentHolderType> void updateCounters(LensContext<AH> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        ExecutionContext executionContext = task.getExecutionContext();
        if (!(executionContext instanceof ActivityExecution)) {
            result.recordNotApplicable();
            return;
        }
        ActivityExecution activityExecution = (ActivityExecution) executionContext;

        /*
         * We update the counters in rules with thresholds in the following ways:
         *
         * 1) If a rule counter was already incremented in this clockwork run, we copy the counter value into the rule.
         * (Regardless of whether it has been triggered during the latest evaluation.)
         *
         * 2) All remaining rules are incremented - if they are triggered.
         *
         * All of this is needed because the rules are quite temporary; they are recreated on each projector run,
         * currently even on each focus iteration (which is maybe unnecessary). We certainly do not want to increase
         * the counters each time. But we need to have the current counter value in the rules even on further projector
         * runs.
         */

        LensFocusContext<AH> focusContext = context.getFocusContextRequired();

        List<EvaluatedPolicyRule> rulesToIncrement = new ArrayList<>();
        for (EvaluatedPolicyRuleImpl rule : focusContext.getObjectPolicyRules()) {
            if (!rule.hasThreshold()) {
                continue;
            }
            Integer knownCounterValue = focusContext.getPolicyRuleCounter(rule.getPolicyRuleIdentifier());
            if (knownCounterValue != null) {
                rule.setCount(knownCounterValue);
            } else {
                if (rule.isTriggered()) {
                    rulesToIncrement.add(rule);
                }
            }
        }
        if (!rulesToIncrement.isEmpty()) {
            activityExecution.incrementCounters(POLICY_RULES, rulesToIncrement, result);
            rulesToIncrement.forEach(
                    rule -> focusContext.setPolicyRuleCounter(rule.getPolicyRuleIdentifier(), rule.getCount()));
        }
    }
}
