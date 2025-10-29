/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.task.api.ExecutionSupport;

class ActivityPolicyRuleUpdater extends PolicyRuleCounterUpdater {

    private final @NotNull AbstractActivityRun<?, ?, ?> activityRun;

    ActivityPolicyRuleUpdater(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        super(activityRun);
        this.activityRun = activityRun;
    }

    @Override
    protected ExecutionSupport.CountersGroup getCountersGroup() {
        return activityRun.getCountersGroup();
    }

    @Override
    protected PolicyRulesContext<EvaluatedActivityPolicyRule> getPolicyRulesContext() {
        return activityRun.getActivityPolicyRulesContext();
    }
}
