/*
 * Copyright (c) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.EvaluationOrder;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author semancik
 *
 */
public class EvaluatedAssignmentTargetImpl implements EvaluatedAssignmentTarget {

    @NotNull private final PrismObject<? extends AssignmentHolderType> target;
    private final boolean evaluateConstructions;
    @NotNull private final AssignmentPathImpl assignmentPath; // TODO reconsider (maybe we should store only some lightweight information here)
    private final AssignmentType assignment;
    private Collection<ExclusionPolicyConstraintType> exclusions = null;
    private final boolean isValid;

    EvaluatedAssignmentTargetImpl(
            @NotNull PrismObject<? extends AssignmentHolderType> target, boolean evaluateConstructions,
            @NotNull AssignmentPathImpl assignmentPath, AssignmentType assignment,
            boolean isValid) {
        this.target = target;
        this.evaluateConstructions = evaluateConstructions;
        this.assignmentPath = assignmentPath;
        this.assignment = assignment;
        this.isValid = isValid;
    }

    @Override
    public @NotNull PrismObject<? extends AssignmentHolderType> getTarget() {
        return target;
    }

    @Override
    public boolean isDirectlyAssigned() {
        return assignmentPath.size() == 1;
    }

    @Override
    public boolean appliesToFocus() {
        return assignmentPath.last().isMatchingOrder;
    }

    @Override
    public boolean appliesToFocusWithAnyRelation(RelationRegistry relationRegistry) {
        if (appliesToFocus() || isDirectlyAssigned()) {
            // This covers any indirectly assigned targets, like user -> org -> parent-org -> root-org -(I)-> role
            // And directly assigned membership relations as well.
            // All delegations + directly/indirectly assigned via membership.
            return true;
        }
        // Treating delegation at the beginning + optionally one arbitrary step (like approver or owner)
        int i = 0;
        while (i < assignmentPath.size() && assignmentPath.getSegment(i).isDelegation()) {
            i++;
        }
        return i >= assignmentPath.size() - 1; // Either all delegation, or delegation + one step
    }

    @Override
    public boolean isEvaluateConstructions() {
        return evaluateConstructions;
    }

    @Override
    public AssignmentType getAssignment() {
        return assignment;
    }

    @Override
    @NotNull
    public AssignmentPathImpl getAssignmentPath() {
        return assignmentPath;
    }

    public String getOid() {
        return target.getOid();
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    /**
     * Only for legacy exclusions. Not reliable. Do not use if you can avoid it.
     * It will get deprecated eventually.
     */
    public Collection<ExclusionPolicyConstraintType> getExclusions() {
        if (exclusions == null) {
            exclusions = new ArrayList<>();
        }
        return exclusions;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "EvaluatedAssignmentTarget", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "Target", target, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Assignment", String.valueOf(assignment), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "EvaluateConstructions", evaluateConstructions, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Valid", isValid, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "Path", assignmentPath, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedAssignmentTargetImpl{" +
                "target=" + target +
                ", isValid=" + isValid +
                '}';
    }
}
