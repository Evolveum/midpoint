/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.CUSTOM;

public class EvaluatedCustomConstraintTrigger extends EvaluatedFocusPolicyRuleTrigger<CustomPolicyConstraintType> {

    public EvaluatedCustomConstraintTrigger(
            @NotNull CustomPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(CUSTOM, constraint, message, shortMessage, false);
    }

    @Override
    public EvaluatedPolicyRuleTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options, @Nullable EvaluatedAssignment newOwner) {
        EvaluatedPolicyRuleTriggerType rv = new EvaluatedPolicyRuleTriggerType();
        fillCommonContent(rv);
        return rv;
    }
}
