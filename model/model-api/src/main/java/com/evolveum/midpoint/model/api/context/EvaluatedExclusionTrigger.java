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

import java.util.Collection;
import java.util.Objects;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.TriggeredPolicyRulesStorageStrategyType.FULL;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class EvaluatedExclusionTrigger extends EvaluatedPolicyRuleTrigger<ExclusionPolicyConstraintType> {

    @NotNull private final EvaluatedAssignment conflictingAssignment;
    private final ObjectType conflictingTarget;
    private final AssignmentPath conflictingPath;

    // we keep thisTarget and thisPath here because in the future they might be useful
    public EvaluatedExclusionTrigger(@NotNull ExclusionPolicyConstraintType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage, @NotNull EvaluatedAssignment conflictingAssignment,
            ObjectType thisTarget, ObjectType conflictingTarget, AssignmentPath thisPath, AssignmentPath conflictingPath) {
        this(constraint, message, shortMessage, conflictingAssignment, conflictingTarget, conflictingPath, false);
    }

    public EvaluatedExclusionTrigger(@NotNull ExclusionPolicyConstraintType constraint,
            LocalizableMessage message, LocalizableMessage shortMessage, @NotNull EvaluatedAssignment conflictingAssignment,
            ObjectType conflictingTarget, AssignmentPath conflictingPath, boolean enforcementOverride) {
        super(PolicyConstraintKindType.EXCLUSION, constraint, message, shortMessage, enforcementOverride);
        this.conflictingAssignment = conflictingAssignment;
        this.conflictingTarget = conflictingTarget;
        this.conflictingPath = conflictingPath;
    }

    public @NotNull EvaluatedAssignment getConflictingAssignment() {
        return conflictingAssignment;
    }

    public ObjectType getConflictingTarget() {
        return conflictingTarget;
    }

    public AssignmentPath getConflictingPath() {
        return conflictingPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EvaluatedExclusionTrigger))
            return false;
        if (!super.equals(o))
            return false;
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
    public EvaluatedExclusionTriggerType toEvaluatedPolicyRuleTriggerBean(PolicyRuleExternalizationOptions options) {
        EvaluatedExclusionTriggerType rv = new EvaluatedExclusionTriggerType();
        fillCommonContent(rv);
        if (options.getTriggeredRulesStorageStrategy() == FULL) {
            rv.setConflictingObjectRef(ObjectTypeUtil.createObjectRef(conflictingTarget));
            rv.setConflictingObjectDisplayName(ObjectTypeUtil.getDisplayName(conflictingTarget));
            if (conflictingPath != null) {
                rv.setConflictingObjectPath(conflictingPath.toAssignmentPathType(options.isIncludeAssignmentsContent()));
            }
            if (options.isIncludeAssignmentsContent() && conflictingAssignment.getAssignment() != null) {
                rv.setConflictingAssignment(conflictingAssignment.getAssignment().clone());
            }
        }
        return rv;
    }

    @Override
    public Collection<? extends PrismObject<?>> getTargetObjects() {
        // conflicting target should be non-empty ... but who knows for sure?
        return conflictingTarget != null ? singleton(conflictingTarget.asPrismObject()) : emptySet();
    }
}
