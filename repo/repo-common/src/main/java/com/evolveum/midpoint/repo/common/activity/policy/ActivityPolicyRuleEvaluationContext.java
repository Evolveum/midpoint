/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import javax.xml.datatype.Duration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingResult;

/**
 * Context that holds important information for evaluation of activity policy rule.
 */
public class ActivityPolicyRuleEvaluationContext {

    private final @NotNull EvaluatedActivityPolicyRule rule;

    private final @NotNull AbstractActivityRun<?, ?, ?> activityRun;

    private final ItemProcessingResult processingResult;

    public ActivityPolicyRuleEvaluationContext(
            @NotNull EvaluatedActivityPolicyRule rule,
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            ItemProcessingResult processingResult) {

        this.activityRun = activityRun;
        this.rule = rule;
        this.processingResult = processingResult;
    }

    public @NotNull AbstractActivityRun<?, ?, ?> getActivityRun() {
        return activityRun;
    }

    public @NotNull EvaluatedActivityPolicyRule getRule() {
        return rule;
    }

    public ItemProcessingResult getProcessingResult() {
        return processingResult;
    }

    public @Nullable Duration getPreexistingExecutionTime() {
        return activityRun.getActivityPolicyRulesContext()
                .getPreexistingValues()
                .getExecutionTime(rule.getPath());
    }

    public @Nullable Integer getPreexistingExecutionAttemptNumber() {
        return activityRun.getActivityPolicyRulesContext()
                .getPreexistingValues()
                .getExecutionAttemptNumber(rule.getPath());
    }
}
