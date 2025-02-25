/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

/**
 * Context that holds important information for evaluation of activity policy rule.
 */
public class ActivityPolicyRuleEvaluationContext {

    @NotNull
    private final EvaluatedActivityPolicyRule rule;
    @NotNull
    private final AbstractActivityRun<?, ?, ?> activityRun;

    public ActivityPolicyRuleEvaluationContext(
            @NotNull EvaluatedActivityPolicyRule rule,
            @NotNull AbstractActivityRun<?, ?, ?> activityRun) {

        this.activityRun = activityRun;
        this.rule = rule;
    }

    public @NotNull AbstractActivityRun<?, ?, ?> getActivityRun() {
        return activityRun;
    }

    public @NotNull EvaluatedActivityPolicyRule getRule() {
        return rule;
    }
}
