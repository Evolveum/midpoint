/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.PolicyViolationException;

/**
 * Parent class for activity policy violation exceptions.
 */
public class ActivityPolicyViolationException extends PolicyViolationException {

    public ActivityPolicyViolationException(LocalizableMessage userFriendlyMessage, String technicalMessage) {
        super(userFriendlyMessage, technicalMessage);
    }

    /**
     * Activity run result status that this violation should eventually produce. Subclasses carrying specific
     * semantics (halt, abort, ...) override this, so they do not need to be enumerated at the conversion sites.
     */
    public @NotNull ActivityRunResultStatus getRunResultStatus() {
        return ActivityRunResultStatus.PERMANENT_ERROR;
    }
}
