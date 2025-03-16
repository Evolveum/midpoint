/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.prism.PrismContainerValue.asContainerable;

class Util {

    static boolean isNonNegative(PlusMinusZero mode) {
        // mode == null is also considered negative, because it is a combination of PLUS and MINUS;
        // so the net result is that for both old and new state there exists an unsatisfied condition on the path.
        return mode == PlusMinusZero.ZERO || mode == PlusMinusZero.PLUS;
    }

    static boolean isChanged(PlusMinusZero mode) {
        // mode == null is also considered negative, because it is a combination of PLUS and MINUS;
        // so the net result is that for both old and new state there exists an unsatisfied condition on the path.
        return mode == PlusMinusZero.PLUS || mode == PlusMinusZero.MINUS;
    }

    static boolean isAllowedByLimitations(AssignmentPathSegment segment, AssignmentType nextAssignment, EvaluationContext<?> ctx) {
        AssignmentType currentAssignment = segment.getAssignment(ctx.evaluateOld);
        AssignmentSelectorType targetLimitation = currentAssignment.getLimitTargetContent();
        if (isDeputyDelegation(nextAssignment, ctx.ae.relationRegistry)) {       // delegation of delegation
            return targetLimitation != null && BooleanUtils.isTrue(targetLimitation.isAllowTransitive());
        } else {
            // As for the case of targetRef==null: we want to pass target-less assignments (focus mappings, policy rules etc)
            // from the delegator to delegatee. To block them we should use order constraints (but also for assignments?).
            return targetLimitation == null || nextAssignment.getTargetRef() == null ||
                    FocusTypeUtil.selectorMatches(targetLimitation, nextAssignment, ctx.ae.prismContext);
        }
    }

    private static boolean isDeputyDelegation(AssignmentType assignmentType, RelationRegistry relationRegistry) {
        ObjectReferenceType targetRef = assignmentType.getTargetRef();
        return targetRef != null && relationRegistry.isDelegation(targetRef.getRelation());
    }

    @Nullable
    static QName getNormalizedRelation(@NotNull AssignmentType assignment) {
        return assignment.getTargetRef() != null ?
                SchemaService.get().normalizeRelation(assignment.getTargetRef().getRelation()) : null;
    }

    static boolean shouldCollectMembership(AssignmentPathSegmentImpl segment) {
        /*
         * We obviously want to process membership from the segment if it's of matching order.
         *
         * But we want to do that also for targets obtained via delegations. The current (approximate) approach is to
         * collect membership from all assignments of any user that we find on the assignment path.
         *
         * TODO: does this work for invalid (effectiveStatus = disabled) assignments?
         */
        return /*!segment.isArchetypeHierarchy()
                && */(segment.direct || segment.isMatchingOrder || segment.source instanceof UserType);
    }

    static AssignmentType getAssignment(
            ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
            boolean evaluateOld) {
        return asContainerable(getAssignmentPcv(assignmentIdi, evaluateOld));
    }

    static PrismContainerValue<AssignmentType> getAssignmentPcv(
            ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> assignmentIdi,
            boolean evaluateOld) {
        return assignmentIdi.getSingleValue(evaluateOld);
    }
}
