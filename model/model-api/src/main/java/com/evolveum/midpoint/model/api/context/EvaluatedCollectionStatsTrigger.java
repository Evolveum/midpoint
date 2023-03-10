/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

public class EvaluatedCollectionStatsTrigger extends EvaluatedPolicyRuleTrigger<CollectionStatsPolicyConstraintType> {

    public EvaluatedCollectionStatsTrigger(
            @NotNull PolicyConstraintKindType kind, @NotNull CollectionStatsPolicyConstraintType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage) {
        super(kind, constraint, message, shortMessage, false);
    }

    @Override
    public EvaluatedCollectionStatsTriggerType toEvaluatedPolicyRuleTriggerBean(PolicyRuleExternalizationOptions options) {
        EvaluatedCollectionStatsTriggerType rv = new EvaluatedCollectionStatsTriggerType();
        fillCommonContent(rv);
        return rv;
    }
}
