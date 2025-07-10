/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

// todo better name [viliam]

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;

import java.util.Map;

/**
 * Helper class for activity policy processing that can be used in non-iterative activities.
 */
public class ActivityPolicyProcessorHelper {

    // todo implement
    public static void initialize(Map map) {
        // todo how to get ActivityRun from TaskType because there's nothing really accessible from script...
        ActivityPolicyRulesProcessor processor = new ActivityPolicyRulesProcessor(null);
        processor.collectRules();
    }

    public static void evaluateAndEnforceRules(ItemProcessingResult processingResult, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, ThresholdPolicyViolationException, ObjectAlreadyExistsException {

        ActivityPolicyRulesProcessor processor = new ActivityPolicyRulesProcessor(null);
        processor.evaluateAndEnforceRules(null, result);
    }
}
