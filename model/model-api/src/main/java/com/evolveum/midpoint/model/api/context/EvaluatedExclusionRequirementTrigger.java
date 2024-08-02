/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EvaluatedExclusionTriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Common abstract superclass for triggered exclusion and requirement constraints.
 */
public abstract class EvaluatedExclusionRequirementTrigger extends EvaluatedPolicyRuleTrigger<ExclusionPolicyConstraintType> {

    @NotNull private final EvaluatedAssignment thisAssignment;
    @NotNull private final ObjectType thisTarget;
    @NotNull private final AssignmentPath thisPath;

    // we keep thisTarget and thisPath here because in the future they might be useful
    public EvaluatedExclusionRequirementTrigger(
            @NotNull PolicyConstraintKindType policyConstraintKind,
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
