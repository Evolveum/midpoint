/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.schema.policy.PolicyConstraintKind;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * Common abstract superclass for triggered exclusion and requirement constraints.
 */
public abstract class EvaluatedExclusionRequirementTrigger extends EvaluatedFocusPolicyRuleTrigger<ExclusionPolicyConstraintType> {

    @NotNull private final EvaluatedAssignment thisAssignment;
    @NotNull private final ObjectType thisTarget;
    @NotNull private final AssignmentPath thisPath;

    // we keep thisTarget and thisPath here because in the future they might be useful
    public EvaluatedExclusionRequirementTrigger(
            @NotNull PolicyConstraintKind policyConstraintKind,
            @NotNull ExclusionPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage,
            @NotNull EvaluatedAssignment thisAssignment,
            @NotNull ObjectType thisTarget,
            @NotNull AssignmentPath thisPath,
            boolean enforcementOverride) {
        super(policyConstraintKind, constraint, message, shortMessage, enforcementOverride);
        this.thisAssignment = thisAssignment;
        this.thisTarget = thisTarget;
        this.thisPath = thisPath;
    }

    public @NotNull EvaluatedAssignment getThisAssignment() {
        return thisAssignment;
    }

    public @NotNull ObjectType getThisTarget() {
        return thisTarget;
    }

    public @NotNull AssignmentPath getThisPath() {
        return thisPath;
    }

}
