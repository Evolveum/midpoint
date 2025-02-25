/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedActivityPolicyRule;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Notifies external observers about activity-related events.
 *
 * Preliminary implementation.
 */
@Experimental
public interface ActivityListener {

    /**
     * Called when the activity realization is complete.
     */
    void onActivityRealizationComplete(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull Task task,
            @NotNull OperationResult result);

    /**
     * Called when policy rule with notification action is triggered during activity execution.
     */
    void onActivityPolicyRuleTrigger(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull EvaluatedActivityPolicyRule policyRule,
            @NotNull Task task,
            @NotNull OperationResult result);
}
