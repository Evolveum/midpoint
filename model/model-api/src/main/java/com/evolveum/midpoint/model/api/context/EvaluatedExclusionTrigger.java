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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents triggered exclusion constraint.
 *
 * [NOTE]
 * ====
 * When present in "foreign policy rules" ({@link EvaluatedAssignment#getAllAssociatedPolicyRules()}), then the
 * values in {@link #conflictingAssignment}, {@link #conflictingTarget}, {@link #thisTarget} and so on may be misleading.
 * They are correct with regards to the original evaluated assignment, but not for the other one.
 *
 * Hence, to get the correct values, use {@link #getRealConflictingAssignment(EvaluatedAssignment)}.
 * ====
 */
public class EvaluatedExclusionTrigger extends EvaluatedPolicyRuleTrigger<ExclusionPolicyConstraintType> {

    // See warning in class javadoc

    @NotNull private final EvaluatedAssignment conflictingAssignment;
    @NotNull private final ObjectType conflictingTarget;
    @NotNull private final AssignmentPath conflictingPath;

    @NotNull private final EvaluatedAssignment thisAssignment;
    @NotNull private final ObjectType thisTarget;
    @NotNull private final AssignmentPath thisPath;

    // we keep thisTarget and thisPath here because in the future they might be useful
    public EvaluatedExclusionTrigger(
            @NotNull ExclusionPolicyConstraintType constraint,
            LocalizableMessage message,
            LocalizableMessage shortMessage,
            @NotNull EvaluatedAssignment thisAssignment,
            @NotNull EvaluatedAssignment conflictingAssignment,
            @NotNull ObjectType thisTarget,
            @NotNull ObjectType conflictingTarget,
            @NotNull AssignmentPath thisPath,
            @NotNull AssignmentPath conflictingPath,
            boolean enforcementOverride) {
        super(PolicyConstraintKindType.EXCLUSION, constraint, message, shortMessage, enforcementOverride);
        this.thisAssignment = thisAssignment;
        this.conflictingAssignment = conflictingAssignment;
        this.thisTarget = thisTarget;
        this.conflictingTarget = conflictingTarget;
        this.thisPath = thisPath;
        this.conflictingPath = conflictingPath;
    }

    public @NotNull EvaluatedAssignment getConflictingAssignment() {
        return conflictingAssignment;
    }

    public @NotNull EvaluatedAssignment getRealConflictingAssignment(@NotNull EvaluatedAssignment owner) {
        // We are relying on equals method here, but most probably the identity is (in fact) checked there, and it should be OK.
        if (conflictingAssignment.equals(owner)) {
            // This is a "foreign rule" situation; the owner perceived by the client is - in fact - the conflicting side here
            // So, the "real" conflicting side is the owning assignment.
            return thisAssignment;
        } else {
            // Standard situation.
            return conflictingAssignment;
        }
    }

    public @NotNull ObjectType getConflictingTarget() {
        return conflictingTarget;
    }

    public @NotNull AssignmentPath getConflictingPath() {
        return conflictingPath;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EvaluatedExclusionTrigger)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        EvaluatedExclusionTrigger that = (EvaluatedExclusionTrigger) o;
        return Objects.equals(conflictingAssignment, that.conflictingAssignment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), conflictingAssignment);
    }

    @Override
    protected void debugDumpSpecific(StringBuilder sb, int indent) {
        // cannot debug dump conflicting assignment in detail, as we might go into infinite loop
        // (the assignment could have evaluated rule that would point to another conflicting assignment, which
        // could point back to this rule)
        DebugUtil.debugDumpWithLabelToStringLn(sb, "conflictingAssignment", conflictingAssignment, indent);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "conflictingPath", conflictingPath, indent);
    }

    @Override
    public EvaluatedExclusionTriggerType toEvaluatedPolicyRuleTriggerBean(
            @NotNull PolicyRuleExternalizationOptions options, @Nullable EvaluatedAssignment newOwner) {
        EvaluatedExclusionTriggerType rv = new EvaluatedExclusionTriggerType();
        fillCommonContent(rv);
        if (options.isFullStorageStrategy()) {
            rv.setConflictingObjectRef(ObjectTypeUtil.createObjectRef(conflictingTarget));
            rv.setConflictingObjectDisplayName(ObjectTypeUtil.getDisplayName(conflictingTarget));
            rv.setConflictingObjectPath(conflictingPath.toAssignmentPathBean(options.isIncludeAssignmentsContent()));
            if (options.isIncludeAssignmentsContent() && conflictingAssignment.getAssignment() != null) {
                rv.setConflictingAssignment(conflictingAssignment.getAssignment().clone());
            }
        }
        return rv;
    }

    @Override
    public Collection<? extends PrismObject<?>> getTargetObjects() {
        return List.of(conflictingTarget.asPrismObject());
    }

    @Override
    public boolean isRelevantForNewOwner(@Nullable EvaluatedAssignment newOwner) {
        return newOwner == null
                || conflictingAssignment.equals(newOwner);
    }
}
