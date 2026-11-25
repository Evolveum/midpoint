/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.policy;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.HasAssignmentPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * Counterpart of the PostgreSQL enum `PolicyReferenceType`: which reference of an indexed policy rule
 * a row of a policy rule reference table holds.
 *
 * Each value is identified by the item holding the reference (a constraint, or the rule itself for marks)
 * and the reference item. Keep in sync with the enum in the database scripts and with the reference items
 * of `IndexedPolicyRuleType` in the schema.
 */
public enum MPolicyReferenceType {

    MARK(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_MARK_REF),

    EXCLUSION_TARGET(
            PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF),
    EXCLUSION_TARGET_ARCHETYPE(
            PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_ARCHETYPE_REF),

    REQUIREMENT_TARGET(
            PolicyConstraintsType.F_REQUIREMENT, ExclusionPolicyConstraintType.F_TARGET_REF),
    REQUIREMENT_TARGET_ARCHETYPE(
            PolicyConstraintsType.F_REQUIREMENT, ExclusionPolicyConstraintType.F_TARGET_ARCHETYPE_REF),

    HAS_ASSIGNMENT_TARGET(
            PolicyConstraintsType.F_HAS_ASSIGNMENT, HasAssignmentPolicyConstraintType.F_TARGET_REF),
    HAS_ASSIGNMENT_TARGET_ARCHETYPE(
            PolicyConstraintsType.F_HAS_ASSIGNMENT, HasAssignmentPolicyConstraintType.F_TARGET_ARCHETYPE_REF),

    HAS_NO_ASSIGNMENT_TARGET(
            PolicyConstraintsType.F_HAS_NO_ASSIGNMENT, HasAssignmentPolicyConstraintType.F_TARGET_REF),
    HAS_NO_ASSIGNMENT_TARGET_ARCHETYPE(
            PolicyConstraintsType.F_HAS_NO_ASSIGNMENT, HasAssignmentPolicyConstraintType.F_TARGET_ARCHETYPE_REF);

    private final QName holder;
    private final QName refItem;

    MPolicyReferenceType(QName holder, QName refItem) {
        this.holder = holder;
        this.refItem = refItem;
    }

    /** Item holding the reference: a constraint, or the policy rule itself for marks. */
    public @NotNull QName getHolder() {
        return holder;
    }

    /** The reference item, e.g. targetRef. */
    public @NotNull QName getRefItem() {
        return refItem;
    }

    /**
     * Returns the type for the holder and the reference item.
     *
     * @throws IllegalStateException if the combination is not indexed; this means the extraction
     * and this enum are out of sync
     */
    public static @NotNull MPolicyReferenceType from(@NotNull QName holder, @NotNull QName refItem) {
        for (MPolicyReferenceType type : values()) {
            if (QNameUtil.match(type.holder, holder) && QNameUtil.match(type.refItem, refItem)) {
                return type;
            }
        }
        throw new IllegalStateException(
                "No policy reference type for " + holder.getLocalPart() + "/" + refItem.getLocalPart());
    }
}
