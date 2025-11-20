/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityAbortingInformationType;

/** An exception indicating that an activity should be aborted because of a policy rule. */
public class ActivityPolicyBasedAbortException extends ActivityPolicyViolationException implements AbortingInformationAware {

    private final @NotNull ActivityAbortingInformationType abortingInformation;

    public ActivityPolicyBasedAbortException(
            LocalizableMessage userFriendlyMessage,
            String technicalMessage,
            @NotNull ActivityAbortingInformationType abortingInformation) {

        super(userFriendlyMessage, technicalMessage);
        this.abortingInformation = abortingInformation;
    }

    @Override
    public @NotNull ActivityAbortingInformationType getAbortingInformation() {
        return abortingInformation;
    }
}
