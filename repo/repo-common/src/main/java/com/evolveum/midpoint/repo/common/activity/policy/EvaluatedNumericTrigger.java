/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;

public class EvaluatedNumericTrigger<C extends NumericThresholdPolicyConstraintType>
        extends EvaluatedActivityPolicyRuleTrigger<C> {

    public EvaluatedNumericTrigger(
            @NotNull PolicyConstraintKindType constraintKind,
            @NotNull C constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(constraintKind, constraint, message, shortMessage);
    }
}
