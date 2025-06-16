/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.task.api.ExecutionSupport;

public class ActivityPolicyRuleUpdater extends PolicyRuleCounterUpdater {

    private final @NotNull AbstractActivityRun<?, ?, ?> activityRun;

    public ActivityPolicyRuleUpdater(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        super(activityRun);

        this.activityRun = activityRun;
    }

    @Override
    protected ExecutionSupport.CountersGroup getCountersGroup() {
        return ExecutionSupport.CountersGroup.ACTIVITY_POLICY_RULES;
    }

    @Override
    protected PolicyRulesContext<EvaluatedActivityPolicyRule> getPolicyRulesContext() {
        return activityRun.getActivityPolicyRulesContext();
    }
}
