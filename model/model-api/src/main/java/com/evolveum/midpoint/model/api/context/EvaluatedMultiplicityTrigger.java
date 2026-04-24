/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

public class EvaluatedMultiplicityTrigger extends EvaluatedClockworkPolicyRuleTrigger<MultiplicityPolicyConstraintType> {

    public EvaluatedMultiplicityTrigger(
            @NotNull PolicyConstraintKind kind, @NotNull MultiplicityPolicyConstraintType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
    }

    @Override
    public EvaluatedMultiplicityTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options) {
        return toEvaluatedPolicyRuleTriggerBean(options, EvaluatedMultiplicityTriggerType::new);
    }
}
