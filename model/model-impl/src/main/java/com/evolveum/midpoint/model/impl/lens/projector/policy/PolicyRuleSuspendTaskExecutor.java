/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.repo.api.CounterManager;
import com.evolveum.midpoint.repo.api.CounterSpecification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author katka
 *
 */
@Component
public class PolicyRuleSuspendTaskExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleSuspendTaskExecutor.class);

    @Autowired private CounterManager counterManager;

    public <O extends ObjectType> void execute(@NotNull ModelContext<O> context, Task task, OperationResult result) throws ThresholdPolicyViolationException, ObjectNotFoundException, SchemaException {
        ModelElementContext<O> focusCtx = context.getFocusContext();

        if (focusCtx == null) {
            return;
        }

        TaskType taskType = task.getUpdatedOrClonedTaskObject().asObjectable();
        for (EvaluatedPolicyRule policyRule : focusCtx.getPolicyRules()) {
            // TEMPORARY fix for MID-6343; TODO implement seriously
            if (policyRule.containsEnabledAction(SuspendTaskPolicyActionType.class)) {
                CounterSpecification counterSpec = counterManager
                        .getCounterSpec(taskType, policyRule.getPolicyRuleIdentifier(), policyRule.getPolicyRule());
                LOGGER.trace("Found counter specification {} for {}", counterSpec, DebugUtil.debugDumpLazily(policyRule));

                int counter = 1;
                if (counterSpec != null) {
                    counter = counterSpec.getCount();
                }
                counter = checkEvaluatedPolicyRule(task, policyRule, counter, result);

                if (counterSpec != null) {
                    LOGGER.trace("Setting new count = {} to counter spec", counter);
                    counterSpec.setCount(counter);
                }
            }
        }

    }

    private synchronized int checkEvaluatedPolicyRule(Task task, EvaluatedPolicyRule policyRule, int counter, OperationResult result) throws ThresholdPolicyViolationException, ObjectNotFoundException, SchemaException {
        counter++;
        LOGGER.trace("Suspend task action enabled for {}, checking threshold settings", DebugUtil.debugDumpLazily(policyRule));
        PolicyThresholdType thresholdSettings = policyRule.getPolicyThreshold();
        if (isOverThreshold(thresholdSettings, counter)) {
            throw new ThresholdPolicyViolationException("Policy rule violation: " + policyRule.getPolicyRule());
        }
        return counter;
    }

    private boolean isOverThreshold(PolicyThresholdType thresholdSettings, int counter) throws SchemaException {
        // TODO: better implementation that takes hight water mark into account
        WaterMarkType lowWaterMark = thresholdSettings.getLowWaterMark();
        if (lowWaterMark == null) {
            LOGGER.trace("No low water mark defined.");
            return true;
        }
        Integer lowWaterCount = lowWaterMark.getCount();
        if (lowWaterCount == null) {
            throw new SchemaException("No count in low water mark in a policy rule");
        }
        return (counter >= lowWaterCount);
    }
}


