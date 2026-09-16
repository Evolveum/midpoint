/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CollectionStatsPolicyConstraintType;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.COLLECTION_STATS;

public class EvaluatedCollectionStatsTrigger extends EvaluatedClockworkPolicyRuleTrigger<CollectionStatsPolicyConstraintType> {

    public EvaluatedCollectionStatsTrigger(
            @NotNull CollectionStatsPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {
        super(COLLECTION_STATS, constraint, message, shortMessage, false);
    }
}
