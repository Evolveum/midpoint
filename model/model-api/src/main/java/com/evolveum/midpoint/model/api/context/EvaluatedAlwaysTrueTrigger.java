/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AlwaysTruePolicyConstraintType;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.ALWAYS_TRUE;

public class EvaluatedAlwaysTrueTrigger extends EvaluatedFocusPolicyRuleTrigger<AlwaysTruePolicyConstraintType> {

    public EvaluatedAlwaysTrueTrigger(
            @NotNull AlwaysTruePolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(ALWAYS_TRUE, constraint, message, shortMessage, false);
    }
}
