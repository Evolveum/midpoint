/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

public class PolicyRuleExternalizationOptions implements Serializable {

    @NotNull private final TriggeredPolicyRulesStorageStrategyType triggeredRulesStorageStrategy;
    private final boolean includeAssignmentsContent;

    public PolicyRuleExternalizationOptions() {
        this(FULL, false);
    }

    public PolicyRuleExternalizationOptions(
            TriggeredPolicyRulesStorageStrategyType triggeredRulesStorageStrategy, boolean includeAssignmentsContent) {
        this.triggeredRulesStorageStrategy = Objects.requireNonNullElse(triggeredRulesStorageStrategy, FULL);
        this.includeAssignmentsContent = includeAssignmentsContent;
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
}
