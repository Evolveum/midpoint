/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.PolicyViolationException;

/**
 * Parent class for activity policy violation exceptions.
 */
public class ActivityPolicyViolationException extends PolicyViolationException {

    public ActivityPolicyViolationException(LocalizableMessage userFriendlyMessage, String technicalMessage) {
        super(userFriendlyMessage, technicalMessage);
    }
}
