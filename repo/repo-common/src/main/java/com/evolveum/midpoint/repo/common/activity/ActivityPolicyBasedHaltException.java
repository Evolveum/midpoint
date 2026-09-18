/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityHaltingInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyActionType;

/**
 * An exception indicating that an activity should be halted because of a policy rule.
 * Eventually results in {@link ActivityRunResultStatus#HALTING_ERROR} being reported.
 */
public class ActivityPolicyBasedHaltException extends ActivityPolicyViolationException {

    /** If present, the halt is recorded in the activity state, so the activity cannot continue until it is cleared. */
    private final @Nullable ActivityHaltingInformationType haltingInformation;

    public ActivityPolicyBasedHaltException(LocalizableMessage userFriendlyMessage, String technicalMessage) {
        this(userFriendlyMessage, technicalMessage, null);
    }

    private ActivityPolicyBasedHaltException(
            LocalizableMessage userFriendlyMessage,
            String technicalMessage,
            @Nullable ActivityHaltingInformationType haltingInformation) {
        super(userFriendlyMessage, technicalMessage);
        this.haltingInformation = haltingInformation;
    }

    /** Halt caused by an action of the given policy rule; it is to be recorded in the activity state. */
    public static ActivityPolicyBasedHaltException forRule(
            LocalizableMessage userFriendlyMessage,
            String technicalMessage,
            @NotNull PolicyActionType action,
            @Nullable String ruleIdentifier,
            @Nullable String ruleName) {
        var information = new ActivityHaltingInformationType()
                .policyAction(action.clone())
                .policyIdentifier(ruleIdentifier)
                .policyName(ruleName)
                .message(LocalizationUtil.createLocalizableMessageType(userFriendlyMessage));
        return new ActivityPolicyBasedHaltException(userFriendlyMessage, technicalMessage, information);
    }

    public @Nullable ActivityHaltingInformationType getHaltingInformation() {
        return haltingInformation;
    }

    @Override
    public @NotNull ActivityRunResultStatus getRunResultStatus() {
        return ActivityRunResultStatus.HALTING_ERROR;
    }
}
