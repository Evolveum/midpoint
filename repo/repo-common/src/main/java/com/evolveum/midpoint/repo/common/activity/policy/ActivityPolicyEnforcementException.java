/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.ActivityRunPolicyException;

/**
 * Carries an {@link ActivityRunPolicyException} thrown by {@link ActivityPolicyProcessorHelper}
 * out of a non-iterative activity, e.g. from within a script.
 *
 * The intermediate layers (like the scripting infrastructure) usually wrap exceptions on their way up,
 * so the activity framework looks this exception up in the cause chain when the activity run fails.
 * It is deliberately a dedicated type thrown only by the helper: an unrelated {@link ActivityRunPolicyException}
 * that happens to be buried in a cause chain must not be (mis)interpreted as a policy enforcement request.
 */
public class ActivityPolicyEnforcementException extends RuntimeException {

    @NotNull private final ActivityRunPolicyException policyException;

    public ActivityPolicyEnforcementException(@NotNull ActivityRunPolicyException policyException) {
        super(policyException.getMessage(), policyException);
        this.policyException = policyException;
    }

    public @NotNull ActivityRunPolicyException getPolicyException() {
        return policyException;
    }
}
