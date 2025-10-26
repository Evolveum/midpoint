/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.ThresholdPolicyViolationException;

public class ActivityThresholdPolicyViolationException extends ThresholdPolicyViolationException {

    private final @NotNull ActivityRunResultStatus activityRunResultStatus;

    private final @NotNull PolicyViolationContext policyViolationContext;

    public ActivityThresholdPolicyViolationException(
            LocalizableMessage userFriendlyMessage,
            String technicalMessage,
            @NotNull ActivityRunResultStatus activityRunResultStatus,
            @NotNull PolicyViolationContext policyViolationContext) {

        super(userFriendlyMessage, technicalMessage);

        this.activityRunResultStatus = activityRunResultStatus;
        this.policyViolationContext = policyViolationContext;
    }

    @NotNull
    public ActivityRunResultStatus getActivityRunResultStatus() {
        return activityRunResultStatus;
    }

    @NotNull
    public PolicyViolationContext getPolicyViolationContext() {
        return policyViolationContext;
    }
}
