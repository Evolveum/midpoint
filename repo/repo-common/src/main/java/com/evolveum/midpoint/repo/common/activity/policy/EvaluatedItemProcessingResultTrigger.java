/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.policy;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemProcessingResultPolicyConstraintType;

public class EvaluatedItemProcessingResultTrigger
        extends EvaluatedActivityPolicyRuleTrigger<ItemProcessingResultPolicyConstraintType> {

    public EvaluatedItemProcessingResultTrigger(
            @NotNull ItemProcessingResultPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage) {

        super(PolicyConstraintKind.ITEM_PROCESSING_RESULT, constraint, message, shortMessage);
    }
}
