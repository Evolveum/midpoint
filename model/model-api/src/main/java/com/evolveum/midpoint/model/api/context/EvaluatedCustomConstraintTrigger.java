/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomPolicyConstraintType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.CUSTOM;

public class EvaluatedCustomConstraintTrigger extends EvaluatedClockworkPolicyRuleTrigger<CustomPolicyConstraintType> {

    public EvaluatedCustomConstraintTrigger(
            @NotNull CustomPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(CUSTOM, constraint, message, shortMessage, false);
    }
}
