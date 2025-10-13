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

public class EvaluatedTimeValidityTrigger extends EvaluatedPolicyRuleTrigger<TimeValidityPolicyConstraintType> {

    public EvaluatedTimeValidityTrigger(
            @NotNull PolicyConstraintKindType kind, @NotNull TimeValidityPolicyConstraintType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
    }

    @Override
    public EvaluatedTimeValidityTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options, @Nullable EvaluatedAssignment newOwner) {
        EvaluatedTimeValidityTriggerType rv = new EvaluatedTimeValidityTriggerType();
        fillCommonContent(rv);
        return rv;
    }
}
