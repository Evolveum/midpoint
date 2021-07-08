/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.policy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyThresholdType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SuspendTaskPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WaterMarkType;

/**
 * @author katka
 *
 */
@Component
public class PolicyRuleSuspendTaskExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleSuspendTaskExecutor.class);

    public <O extends ObjectType> void execute(@NotNull ModelContext<O> context, Task task, OperationResult result)
            throws ThresholdPolicyViolationException, ObjectNotFoundException, SchemaException {
        ModelElementContext<O> focusCtx = context.getFocusContext();

        if (focusCtx == null) {
            return;
        }

        for (EvaluatedPolicyRule policyRule : focusCtx.getObjectPolicyRules()) {
            // In theory we could count events also for other kinds of actions (not only SuspendTask)
            if (policyRule.containsEnabledAction(SuspendTaskPolicyActionType.class)) {
                if (isOverThreshold(policyRule.getPolicyThreshold(), policyRule.getCount())) {
                    throw new ThresholdPolicyViolationException("Policy rule violation: " + policyRule.getPolicyRule());
                }
            }
        }
    }

    // TODO move to policy rule implementation
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


