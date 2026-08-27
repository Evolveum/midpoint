/*
 * Copyright (c) 2016-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.policy.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractPolicyConstraintType;

/**
 * An {@link EvaluatedPolicyRuleTrigger} that is specific for the clockwork processing.
 */
public abstract class EvaluatedClockworkPolicyRuleTrigger<CT extends AbstractPolicyConstraintType>
        extends EvaluatedPolicyRuleTrigger<CT> {

    public EvaluatedClockworkPolicyRuleTrigger(
            @NotNull PolicyConstraintKind constraintKind,
            @NotNull CT constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage,
            boolean enforcementOverride) {
        super(constraintKind, constraint, message, shortMessage, enforcementOverride);
    }

    /**
     * Returns {@code true} if this trigger is relevant after a policy rule was "transplanted"
     * to a new {@link EvaluatedAssignment}.
     *
     * For the whole context, see {@link EvaluatedClockworkPolicyRule} and {@link ForeignEvaluatedClockworkPolicyRule}.
     *
     * Approximate solution for now. It works well for {@link EvaluatedExclusionTrigger}s but we're not sure for other ones.
     *
     * @see EvaluatedClockworkPolicyRule#getRelevantTriggersFilter()
     */
    public boolean isRelevantForAssignmentOverride(@NotNull EvaluatedAssignment assignmentOverride) {
        // For all triggers except exclusion ones we always return "true", as we have no way of knowing whether they
        // are relevant in the context of "the other side". This may change in the future, after we'll learn how to
        // understand this.
        return true;
    }
}
