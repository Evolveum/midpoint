/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.selector.eval.OwnerResolver;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.*;

public class OwnerClause extends SelectorClause {

    @NotNull private final ValueSelector selector;

    private OwnerClause(@NotNull ValueSelector selector) {
        this.selector = selector;
    }

    static OwnerClause of(@NotNull ValueSelector selector) {
        return new OwnerClause(selector);
    }

    @Override
    public @NotNull String getName() {
        return "owner";
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
        OwnerResolver ownerResolver = ctx.ownerResolver;
        if (ownerResolver == null) {
            traceNotApplicable(ctx, "no owner resolver");
            return false;
        }
        var owners = ownerResolver.resolveOwner(object.asPrismObject());
        if (owners == null) {
            traceNotApplicable(ctx, "no owner");
            return false;
        }
        // The availability of the full information about the parent is the same as for the current value (for shadows),
        // or guaranteed for other kind of data. Let's keep it simple and assume the worst (i.e. the same as for current value).
        for (var owner : owners) {
            boolean matches =
                    selector.matches(owner.getValue(), ctx.next("o", "owner", null));
            traceApplicability(ctx, matches, "owner (%s) matches: %s", owner, matches);
            if (matches) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {
        // TODO: MID-3899
        // TODO what if owner is specified not as "self" ?
        FocusType subject = ctx.getPrincipalFocus();
        if (subject != null) {
            if (TaskType.class.isAssignableFrom(ctx.getRestrictedType())) {
                addConjunct(ctx, applyOwnerFilterOwnerRef(subject));
                return true;
            } else if (AbstractRoleType.class.isAssignableFrom(ctx.getRestrictedType())) {
                if (ctx.isReferencedBySupported()) {
                    addConjunct(ctx, applyReferencedByFilter(subject, ctx.getRestrictedType()));
                    return true;
                } else {
                    traceNotApplicable(ctx, "not applicable on non postgresql repository");
                    return false;
                }
            }
            traceNotApplicable(ctx, "applicability when searching is limited to TaskType & AbstractRoleType objects");
            return false;
        }
        traceNotApplicable(ctx, "no principal");
        return false;
    }

    private ObjectFilter applyReferencedByFilter(FocusType subject, Class<?> restrictedType) {
        var relations = SchemaService.get().relationRegistry().getAllRelationsFor(RelationKindType.OWNER).iterator();
        assert relations.hasNext();
        S_FilterExit builder = PrismContext.get().queryFor(restrictedType.asSubclass(AbstractRoleType.class))
                .referencedBy(subject.getClass(), FocusType.F_ROLE_MEMBERSHIP_REF, relations.next())
                .block().ownerId(subject.getOid()).endBlock();

        while (relations.hasNext()) {
            builder = builder.or().referencedBy(subject.getClass(), FocusType.F_ROLE_MEMBERSHIP_REF, relations.next())
                        .block().ownerId(subject.getOid()).endBlock();

        }
        return builder.buildFilter();

    }

    // TODO review this legacy code
    private ObjectFilter applyOwnerFilterOwnerRef(@NotNull FocusType subject) {
        S_FilterExit builder = PrismContext.get().queryFor(TaskType.class)
                .item(TaskType.F_OWNER_REF).ref(subject.getOid());
        // We select also tasks that are owned by any of subject's parent orgs - TODO why?
        for (ObjectReferenceType subjectParentOrgRef : subject.getParentOrgRef()) {
            if (PrismContext.get().isDefaultRelation(subjectParentOrgRef.getRelation())) {
                builder = builder.or().item(TaskType.F_OWNER_REF).ref(subjectParentOrgRef.getOid());
            }
        }
        return builder.buildFilter();
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "selector", selector, indent + 1);
    }

    @Override
    public String toString() {
        return "OwnerClause{" +
                "selector=" + selector +
                "}";
    }
}
