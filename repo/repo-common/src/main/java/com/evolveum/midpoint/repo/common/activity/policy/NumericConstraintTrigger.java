/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NumericThresholdPolicyConstraintType;

public class NumericConstraintTrigger<C extends NumericThresholdPolicyConstraintType>
        extends EvaluatedActivityPolicyRuleTrigger<C> {

    public NumericConstraintTrigger(@NotNull C constraint, LocalizableMessage message, LocalizableMessage shortMessage) {
        super(constraint, message, shortMessage);
    }
}
