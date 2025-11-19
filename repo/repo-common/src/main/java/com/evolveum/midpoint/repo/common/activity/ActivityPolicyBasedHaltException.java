/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.util.LocalizableMessage;

/** An exception indicating that an activity should be halted because of a policy rule. */
public class ActivityPolicyBasedHaltException extends ActivityPolicyViolationException {

    public ActivityPolicyBasedHaltException(LocalizableMessage userFriendlyMessage, String technicalMessage) {

        super(userFriendlyMessage, technicalMessage);
    }
}
