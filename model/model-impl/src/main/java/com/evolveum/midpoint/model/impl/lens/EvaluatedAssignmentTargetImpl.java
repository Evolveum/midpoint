/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

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

    final PrismObject<? extends AssignmentHolderType> target;
    private final boolean evaluateConstructions;
    @NotNull private final AssignmentPathImpl assignmentPath;     // TODO reconsider (maybe we should store only some lightweight information here)
    private final AssignmentType assignment;
    private Collection<ExclusionPolicyConstraintType> exclusions = null;
    private final boolean isValid;

    EvaluatedAssignmentTargetImpl(
            PrismObject<? extends AssignmentHolderType> target, boolean evaluateConstructions,
            @NotNull AssignmentPathImpl assignmentPath, AssignmentType assignment,
            boolean isValid) {
        this.target = target;
        this.evaluateConstructions = evaluateConstructions;
        this.assignmentPath = assignmentPath;
        this.assignment = assignment;
        this.isValid = isValid;
    }

    @Override
    public PrismObject<? extends AssignmentHolderType> getTarget() {
        return target;
    }

    @Override
    public boolean isDirectlyAssigned() {
        return assignmentPath.size() == 1;
    }

    @Override
    public boolean appliesToFocus() {
        return assignmentPath.last().isMatchingOrder();
    }

    @Override
    public boolean appliesToFocusWithAnyRelation(RelationRegistry relationRegistry) {
        // TODO clean up this method
        if (appliesToFocus()) {
            // This covers any indirectly assigned targets, like user -> org -> parent-org -> root-org -(I)-> role
            // And directly assigned membership relations as well.
            return true;
        }
        // And this covers any directly assigned non-membership relations (like approver or owner).
        // Actually I think these should be also covered by appliesToFocus() i.e. their isMatchingOrder should be true.
        // But for some reason it is currently not so.
        EvaluationOrder order = assignmentPath.last().getEvaluationOrder();
        if (order.getSummaryOrder() == 1) {
            return true;
        }
        if (order.getSummaryOrder() != 0) {
            return false;
        }
        int delegationCount = 0;
        for (QName delegationRelation : relationRegistry.getAllRelationsFor(RelationKindType.DELEGATION)) {
            delegationCount += order.getMatchingRelationOrder(delegationRelation);
        }
        return delegationCount > 0;
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

}
