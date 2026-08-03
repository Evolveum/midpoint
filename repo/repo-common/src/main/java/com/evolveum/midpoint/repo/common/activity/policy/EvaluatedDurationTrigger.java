/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.repo.common.activity.policy.evaluator.DurationConstraintEvaluator;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DurationThresholdPolicyConstraintType;

/**
 * Trigger for constraints that act on duration.
 *
 * @see DurationConstraintEvaluator
 */
public class EvaluatedDurationTrigger<C extends DurationThresholdPolicyConstraintType>
        extends EvaluatedPolicyRuleTrigger<C> {

    public EvaluatedDurationTrigger(
            @NotNull PolicyConstraintKind policyConstraintKind,
            @NotNull C constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {

        super(policyConstraintKind, constraint, message, shortMessage);
    }
}
