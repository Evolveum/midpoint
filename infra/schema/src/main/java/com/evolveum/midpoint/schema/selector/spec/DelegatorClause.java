/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class DelegatorClause extends SelectorClause {

    @NotNull private final ValueSelector selector;
    private final boolean allowInactive;

    private DelegatorClause(@NotNull ValueSelector selector, boolean allowInactive) {
        this.selector = selector;
        this.allowInactive = allowInactive;
    }

    static DelegatorClause of(@NotNull ValueSelector selector, boolean allowInactive) {
        return new DelegatorClause(selector, allowInactive);
    }

    @Override
    public @NotNull String getName() {
        return "delegator";
    }

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        var object = asObjectTypeIfPossible(value);
        if (object == null) {
            traceNotApplicable(ctx, "Not an object");
            return false;
        }
        if (!selector.isPureSelf()) {
            // Currently, we support only "self" delegator selector clause
            throw new UnsupportedOperationException("Unsupported non-self delegator clause");
        }
        if (!(object instanceof UserType)) {
            traceNotApplicable(ctx, "object is not a user");
            return false;
        }

        String principalOid = ctx.getPrincipalOid();
        if (principalOid != null) {
            for (ObjectReferenceType objectDelegatedRef : ((UserType) object).getDelegatedRef()) {
                if (principalOid.equals(objectDelegatedRef.getOid())) {
                    return true;
                }
            }
        }

        if (allowInactive) {
            for (AssignmentType assignment : ((UserType) object).getAssignment()) {
                ObjectReferenceType assignmentTargetRef = assignment.getTargetRef();
                if (assignmentTargetRef == null) {
                    continue;
                }
                if (principalOid != null && principalOid.equals(assignmentTargetRef.getOid())) {
                    if (SchemaService.get().relationRegistry().isDelegation(assignmentTargetRef.getRelation())) {
                        return true;
                    }
                }
            }
        }

        traceNotApplicable(ctx, "delegator does not match");
        return false;
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {
        // TODO: MID-3899
        traceNotApplicable(ctx, "not supported when searching");
        return false;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "allowInactive", allowInactive, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "selector", selector, indent + 1);
    }

    @Override
    public String toString() {
        return "DelegatorClause{" +
                "selector=" + selector +
                ", allowInactive=" + allowInactive +
                "}";
    }
}
