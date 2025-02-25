/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events;

import com.evolveum.midpoint.repo.common.activity.policy.EvaluatedActivityPolicyRule;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

/**
 * Event that is triggered by the 'notification' policy rule action during activity execution.
 */
public class ActivityPolicyRuleEventImpl extends ActivityEventImpl {

    private @NotNull EvaluatedActivityPolicyRule policyRule;

    public ActivityPolicyRuleEventImpl(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull EvaluatedActivityPolicyRule policyRule) {

        super(activityRun);

        this.policyRule = policyRule;
    }

    public @NotNull EvaluatedActivityPolicyRule getPolicyRule() {
        return policyRule;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        debugDumpCommon(sb, indent);
        DebugUtil.debugDumpWithLabelToString(sb, "activityRun", getActivityRun(), indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "policyRule", getPolicyRule(), indent + 1);
        return sb.toString();
    }
}
