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

    public <O extends ObjectType> void execute(@NotNull ModelContext<O> context, Task task, OperationResult result)
            throws ThresholdPolicyViolationException, ObjectNotFoundException, SchemaException {
        ModelElementContext<O> focusCtx = context.getFocusContext();

        if (focusCtx == null) {
            return;
        }

        String id = context.getTaskTreeOid(task, result);
        if (id == null) {
            LOGGER.trace("No persistent task context, no counting!");
            return;
        }

        for (EvaluatedPolicyRule policyRule : focusCtx.getPolicyRules()) {
            // In theory we could count events also for other kinds of actions (not only SuspendTask)
            if (policyRule.containsEnabledAction(SuspendTaskPolicyActionType.class)) {
                CounterSpecification counterSpec = counterManager.getCounterSpec(id, policyRule.getPolicyRuleIdentifier(), policyRule.getPolicyRule());
                LOGGER.trace("Created/found counter specification {} for:\n{}", counterSpec, DebugUtil.debugDumpLazily(policyRule));
                int countAfter = counterSpec.incrementAndGet();
                if (isOverThreshold(policyRule.getPolicyThreshold(), countAfter)) {
                    throw new ThresholdPolicyViolationException("Policy rule violation: " + policyRule.getPolicyRule());
                }
            }
        }
    }

    private boolean isOverThreshold(PolicyThresholdType thresholdSettings, int counter) throws SchemaException {
        // TODO: better implementation that takes high water mark into account
        WaterMarkType lowWaterMark = thresholdSettings != null ? thresholdSettings.getLowWaterMark() : null;
        if (lowWaterMark == null) {
            LOGGER.trace("No low water mark defined.");
            return true;
        }
        Integer lowWaterCount = lowWaterMark.getCount();
        if (lowWaterCount == null) {
            throw new SchemaException("No count in low water mark in a policy rule");
        }
        return counter >= lowWaterCount;
    }
}


