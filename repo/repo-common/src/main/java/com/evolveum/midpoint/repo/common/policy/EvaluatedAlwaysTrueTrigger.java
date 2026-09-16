/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AlwaysTruePolicyConstraintType;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.ALWAYS_TRUE;

/**
 * Triggered by {@link AlwaysTruePolicyConstraintType}.
 */
public class EvaluatedAlwaysTrueTrigger extends EvaluatedPolicyRuleTrigger<AlwaysTruePolicyConstraintType> {

    public EvaluatedAlwaysTrueTrigger(
            @NotNull AlwaysTruePolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(ALWAYS_TRUE, constraint, message, shortMessage, false);
    }
}
