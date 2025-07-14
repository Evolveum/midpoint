/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

// todo better name [viliam]

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;

/**
 * Helper class for activity policy processing that can be used in non-iterative activities.
 */
public class ActivityPolicyProcessorHelper {

    private static final ThreadLocal<AbstractActivityRun<?, ?, ?>> ACTIVITY_RUN_THREAD_LOCAL = new ThreadLocal<>();

    public static void setCurrentActivityRun(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        ACTIVITY_RUN_THREAD_LOCAL.set(activityRun);
    }

    public static void clearCurrentActivityRun() {
        ACTIVITY_RUN_THREAD_LOCAL.remove();
    }

    private static AbstractActivityRun<?, ?, ?> getCurrentActivityRun() {
        return ACTIVITY_RUN_THREAD_LOCAL.get();
    }

    public static void initialize() {
        AbstractActivityRun<?, ?, ?> activityRun = getCurrentActivityRun();
        if (activityRun == null) {
            throw new IllegalStateException("ActivityRun is not available in the current thread. ");
        }

        ActivityPolicyRulesProcessor processor = new ActivityPolicyRulesProcessor(activityRun);
        processor.collectRules();
    }

    public static void evaluateAndEnforceRules(ItemProcessingResult processingResult, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ThresholdPolicyViolationException, ObjectAlreadyExistsException {

        AbstractActivityRun<?, ?, ?> activityRun = getCurrentActivityRun();
        if (activityRun == null) {
            throw new IllegalStateException("ActivityRun is not available in the current thread. ");
        }

        ActivityPolicyRulesProcessor processor = new ActivityPolicyRulesProcessor(activityRun);
        processor.evaluateAndEnforceRules(null, result);
    }
}
