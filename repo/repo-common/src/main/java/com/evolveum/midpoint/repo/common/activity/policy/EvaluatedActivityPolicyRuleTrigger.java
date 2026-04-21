/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

public class EvaluatedActivityPolicyRuleTrigger<CT extends AbstractPolicyConstraintType> extends EvaluatedPolicyRuleTrigger<CT> {

    public EvaluatedActivityPolicyRuleTrigger(
            @NotNull PolicyConstraintKind constraintKind,
            @NotNull CT constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {

        super(constraintKind, constraint, message, shortMessage);
    }
}
