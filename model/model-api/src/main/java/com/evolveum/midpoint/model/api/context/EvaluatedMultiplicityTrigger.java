/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class EvaluatedMultiplicityTrigger extends EvaluatedPolicyRuleTrigger<MultiplicityPolicyConstraintType> {

    public EvaluatedMultiplicityTrigger(@NotNull PolicyConstraintKindType kind, @NotNull MultiplicityPolicyConstraintType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
    }

    @Override
    public EvaluatedMultiplicityTriggerType toEvaluatedPolicyRuleTriggerType(PolicyRuleExternalizationOptions options,
            PrismContext prismContext) {
        EvaluatedMultiplicityTriggerType rv = new EvaluatedMultiplicityTriggerType();
        fillCommonContent(rv);
        return rv;
    }
}
