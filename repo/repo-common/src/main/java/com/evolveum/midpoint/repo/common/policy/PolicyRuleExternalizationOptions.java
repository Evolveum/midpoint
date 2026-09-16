/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

import java.io.Serializable;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedPolicyRuleTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType;

/**
 * Options driving the conversion of {@link EvaluatedPolicyRuleTrigger} to its externalized form,
 * i.e., {@link EvaluatedPolicyRuleTriggerType}.
 */
public class PolicyRuleExternalizationOptions implements Serializable {

    /** TODO */
    @NotNull private final TriggeredPolicyRulesStorageStrategyType triggeredRulesStorageStrategy;

    /** TODO */
    private final boolean includeAssignmentsContent;

    /** Which triggers should be copied to the output? Used also for inner triggers. */
    private final TriggerFilter triggerFilter;

    public static PolicyRuleExternalizationOptions empty() {
        return new PolicyRuleExternalizationOptions();
    }

    public PolicyRuleExternalizationOptions() {
        this(FULL, false, null);
    }

    public PolicyRuleExternalizationOptions(
            TriggeredPolicyRulesStorageStrategyType triggeredRulesStorageStrategy,
            boolean includeAssignmentsContent,
            TriggerFilter triggerFilter) {
        this.triggeredRulesStorageStrategy = Objects.requireNonNullElse(triggeredRulesStorageStrategy, FULL);
        this.includeAssignmentsContent = includeAssignmentsContent;
        this.triggerFilter = triggerFilter;
    }

    public PolicyRuleExternalizationOptions withTriggerFilter(TriggerFilter triggerFilter) {
        return new PolicyRuleExternalizationOptions(triggeredRulesStorageStrategy, includeAssignmentsContent, triggerFilter);
    }

    @NotNull
    public TriggeredPolicyRulesStorageStrategyType getTriggeredRulesStorageStrategy() {
        return triggeredRulesStorageStrategy;
    }

    public boolean isIncludeAssignmentsContent() {
        return includeAssignmentsContent;
    }

    public boolean isFullStorageStrategy() {
        return triggeredRulesStorageStrategy == FULL;
    }

    public boolean matchesSelector(EvaluatedPolicyRuleTrigger<?> trigger) {
        return triggerFilter == null || triggerFilter.test(trigger);
    }
}
