/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SuspendTaskPolicyActionType;

public class ActivityPolicyRulesEnforcer {

    private final @NotNull AbstractActivityRun<?, ?, ?> activityRun;

    public ActivityPolicyRulesEnforcer(@NotNull AbstractActivityRun<?, ?, ?> activityRun) {
        this.activityRun = activityRun;
    }

    public void enforceRules() throws ThresholdPolicyViolationException {
        List<EvaluatedActivityPolicyRule> rules = activityRun.getActivityPolicyRulesContext().getPolicyRules();

        if (rules.isEmpty()) {
            return;
        }

        // todo check also whether action condition is enabled
        for (EvaluatedActivityPolicyRule rule : rules) {
            if (rule.containsAction(SuspendTaskPolicyActionType.class)) {

                throw new ThresholdPolicyViolationException("Policy violation, rule: " + rule);
            }
        }

        // todo implement notification event
    }
}
