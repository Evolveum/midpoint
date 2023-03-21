/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedStateTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StatePolicyConstraintType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EvaluatedStateTrigger extends EvaluatedPolicyRuleTrigger<StatePolicyConstraintType> {

    public EvaluatedStateTrigger(
            @NotNull PolicyConstraintKindType kind, @NotNull StatePolicyConstraintType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
    }

    @Override
    public EvaluatedStateTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options, @Nullable EvaluatedAssignment newOwner) {
        EvaluatedStateTriggerType rv = new EvaluatedStateTriggerType();
        fillCommonContent(rv);
        return rv;
    }
}
