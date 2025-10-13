/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DurationThresholdPolicyConstraintType;

public class DurationThresholdPolicyTrigger<C extends DurationThresholdPolicyConstraintType> extends EvaluatedActivityPolicyRuleTrigger<C> {

    public DurationThresholdPolicyTrigger(
            @NotNull C constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {

        super(constraint, message, shortMessage);
    }
}
