/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.repo.common.activity.policy.evaluator.NumericConstraintEvaluator;
import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;

/**
 * Trigger for constraints that act on numeric values.
 *
 * @see NumericConstraintEvaluator
 */
public class EvaluatedNumericTrigger<C extends NumericThresholdPolicyConstraintType>
        extends EvaluatedPolicyRuleTrigger<C> {

    public EvaluatedNumericTrigger(
            @NotNull PolicyConstraintKind constraintKind,
            @NotNull C constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(constraintKind, constraint, message, shortMessage);
    }
}
