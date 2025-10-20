/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.task.api.ExecutionSupport;

import static com.evolveum.midpoint.task.api.ExecutionSupport.CountersGroup.FULL_EXECUTION_MODE_POLICY_RULES;
import static com.evolveum.midpoint.task.api.ExecutionSupport.CountersGroup.PREVIEW_MODE_POLICY_RULES;

public class ActivityPolicyRuleUpdater extends PolicyRuleCounterUpdater {

    private final @NotNull AbstractActivityRun<?, ?, ?> activityRun;

    public ActivityPolicyRuleUpdater(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        super(activityRun);

        this.activityRun = activityRun;
    }

    @Override
    protected ExecutionSupport.CountersGroup getCountersGroup() {
        return activityRun.getActivityExecutionMode() == ExecutionModeType.FULL ?
                        FULL_EXECUTION_MODE_POLICY_RULES : PREVIEW_MODE_POLICY_RULES;
    }

    @Override
    protected PolicyRulesContext<EvaluatedActivityPolicyRule> getPolicyRulesContext() {
        return activityRun.getActivityPolicyRulesContext();
    }
}
