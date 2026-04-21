/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import java.util.Collection;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;

public class ActivityCompositeTrigger extends EvaluatedActivityPolicyRuleTrigger<PolicyConstraintsType> {

    private final @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers;

    public ActivityCompositeTrigger(
            @NotNull PolicyConstraintKind constraintKind,
            @NotNull PolicyConstraintsType constraint,
            @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers) {
        super(constraintKind, constraint, null, null);

        this.innerTriggers = innerTriggers;
    }

    public @NotNull Collection<EvaluatedPolicyRuleTrigger<?>> getInnerTriggers() {
        return innerTriggers;
    }
}
