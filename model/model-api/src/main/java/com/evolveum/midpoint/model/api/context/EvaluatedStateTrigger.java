/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedStateTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StatePolicyConstraintType;
import org.jetbrains.annotations.NotNull;

/** For {@link PolicyConstraintKind#OBJECT_STATE} and {@link PolicyConstraintKind#ASSIGNMENT_STATE}. */
public class EvaluatedStateTrigger extends EvaluatedClockworkPolicyRuleTrigger<StatePolicyConstraintType> {

    public EvaluatedStateTrigger(
            @NotNull PolicyConstraintKind kind,
            @NotNull StatePolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
    }

    @Override
    public EvaluatedStateTriggerType toEvaluatedPolicyRuleTriggerBean(@NotNull PolicyRuleExternalizationOptions options) {
        return toEvaluatedPolicyRuleTriggerBean(options, EvaluatedStateTriggerType::new);
    }
}
