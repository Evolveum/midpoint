/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.repo.common.policy.PolicyRuleExternalizationOptions;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedTimeValidityTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeValidityPolicyConstraintType;
import org.jetbrains.annotations.NotNull;

/**
 * This class is not used, because the preliminary code for triggering time validity was removed (from validity scanner)
 * by mistake; and the serious implementation (in clockwork) was never done.
 */
public class EvaluatedTimeValidityTrigger extends EvaluatedClockworkPolicyRuleTrigger<TimeValidityPolicyConstraintType> {

    public EvaluatedTimeValidityTrigger(
            @NotNull PolicyConstraintKind kind,
            @NotNull TimeValidityPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
    }

    @Override
    public EvaluatedTimeValidityTriggerType toEvaluatedPolicyRuleTriggerBean(@NotNull PolicyRuleExternalizationOptions options) {
        return toEvaluatedPolicyRuleTriggerBean(options, EvaluatedTimeValidityTriggerType::new);
    }
}
