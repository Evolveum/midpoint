/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import static com.evolveum.midpoint.schema.policy.PolicyConstraintKind.ORPHANED;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrphanedPolicyConstraintType;

public class EvaluatedOrphanedTrigger extends EvaluatedFocusPolicyRuleTrigger<OrphanedPolicyConstraintType> {

    public EvaluatedOrphanedTrigger(
            @NotNull OrphanedPolicyConstraintType constraint, LocalizableMessage message, LocalizableMessage shortMessage) {
        super(ORPHANED, constraint, message, shortMessage, false);
    }
}
