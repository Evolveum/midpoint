/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.selector.eval.SelectorProcessingContext;
import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext.DelegatorSelection;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectTypeIfPossible;

public class RelatedObjectClause extends SelectorClause {

    @NotNull private final ValueSelector selector;

    private RelatedObjectClause(@NotNull ValueSelector selector) {
        this.selector = selector;
    }

    static RelatedObjectClause of(@NotNull ValueSelector selector) {
        return new RelatedObjectClause(selector);
    }

    @Override
    public @NotNull String getName() {
        return "relatedObject";
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
        PrismObject<? extends ObjectType> relatedObject = getRelatedObject(object, ctx);
        if (relatedObject == null) {
            traceNotApplicable(ctx, "has no related object");
            return false;
        }
        // The related object is always known "in full", as it is fetched from the repository via objectResolver.
        boolean matches =
                selector.matches(
                        relatedObject.getValue(),
                        ctx.next(DelegatorSelection.NO_DELEGATOR, "rel", "related object", true));
        traceApplicability(ctx, matches, "related object (%s) matches: %s", relatedObject, matches);
        return matches;
    }

    private PrismObject<? extends ObjectType> getRelatedObject(ObjectType object, @NotNull SelectorProcessingContext ctx) {
        if (object instanceof CaseType aCase) {
            return ctx.resolveReference(aCase.getObjectRef(), object, "related object");
        } else if (object instanceof TaskType task) {
            return ctx.resolveReference(task.getObjectRef(), object, "related object");
        } else {
            return null;
        }
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {
        Class<?> objectType = ctx.getRestrictedType();
        if (CaseType.class.isAssignableFrom(objectType)
                || TaskType.class.isAssignableFrom(objectType)) {
            //noinspection unchecked
            addConjunct(ctx, createFilter((Class<? extends ObjectType>) objectType, ctx));
            return true;
        } else {
            traceNotApplicable(ctx, "this specification is applicable for search only for cases and tasks");
            return false;
        }
    }

    private ObjectFilter createFilter(Class<? extends ObjectType> objectType, @NotNull FilteringContext ctx) {
        // we assume CaseType.F_OBJECT_REF == TaskType.F_OBJECT_REF here
        return PrismContext.get().queryFor(objectType)
                .item(CaseType.F_OBJECT_REF)
                .ref(ctx.getSelfOidsArray(DelegatorSelection.NO_DELEGATOR))
                .buildFilter();
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "selector", selector, indent + 1);
    }

    @Override
    public String toString() {
        return "RelatedObjectClause{" +
                "selector=" + selector +
                "}";
    }
}
