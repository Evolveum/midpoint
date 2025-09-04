/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.selector.eval.SubjectedEvaluationContext;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class SelfClause extends SelectorClause {

    @NotNull private final RelationRegistry relationRegistry = SchemaService.get().relationRegistry();
    @NotNull private final Matching matching;

    private SelfClause(@NotNull Matching matching) {
        this.matching = matching;
    }

    static SelfClause object() {
        return new SelfClause(Matching.OBJECT);
    }

    static SelfClause deputyAssignment() {
        return new SelfClause(Matching.DEPUTY_ASSIGNMENT);
    }

    static SelfClause deputyReference() {
        return new SelfClause(Matching.DEPUTY_REFERENCE);
    }

    @Override
    public @NotNull String getName() {
        return "self(%s)".formatted(matching);
    }

    /**
     * Returns true in case incoming value's OID matches at least one of the "self" OIDs
     * (for "self" OIDs definition, look at {@link SubjectedEvaluationContext#getSelfOids}).
     * In case of OBJECT matching type, there can be 2 types of the incoming prism value objects:
     * 1. In case if AssigneeClause is defined as a "pure self" (meaning assignee can be "self" object only),
     * we don't need to resolve assignee's prism object, and we send PrismReferenceValue type object
     * to the SelfClause.matches method.
     * 2. In other cases (than "pure self" AssigneeClause), referenced object is resolved to "full" object
     * and sent to the SelfClause.matches method as PrismObjectValue type object.
     */
    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        // "Parsing" the incoming value
        String objectOid;
        switch (matching) {
            case OBJECT -> {
                if (value instanceof PrismReferenceValue) {
                    //in case the (assignee) referenced object was not resolved (see AssigneeClause.matches)
                    objectOid = ((PrismReferenceValue) value).getOid();
                } else {
                    //in case the (assignee) referenced object was resolved to a full object (see AssigneeClause.matches)
                    var object = ObjectTypeUtil.asObjectTypeIfPossible(value);
                    if (object == null) {
                        traceNotApplicable(ctx, "Not an object");
                        return false;
                    }
                    objectOid = object.getOid();
                }
            }
            case DEPUTY_ASSIGNMENT -> {
                var assignment = toAssignmentIfPossible(value);
                if (assignment == null) {
                    traceNotApplicable(ctx, "Not an assignment");
                    return false;
                }
                var targetRef = assignment.getTargetRef();
                if (targetRef == null) {
                    traceNotApplicable(ctx, "Assignment without targetRef");
                    return false;
                }
                if (!relationMatches(targetRef, ctx)) {
                    return false;
                }
                objectOid = targetRef.getOid();
            }
            case DEPUTY_REFERENCE -> {
                if (!(value instanceof PrismReferenceValue prv)) {
                    traceNotApplicable(ctx, "Not a reference value");
                    return false;
                }
                if (!relationMatches(prv.asReferencable(), ctx)) {
                    return false;
                }
                objectOid = prv.getOid();
            }
            default -> throw new AssertionError(matching);
        }
        if (objectOid == null) {
            traceNotApplicable(ctx, "No OID in object");
            return false;
        }

        // Comparing OIDs (relations were already matched)
        String principalOid = ctx.getPrincipalOid();
        if (principalOid == null) {
            traceNotApplicable(ctx, "no principal OID");
            return false;
        } else {
            if (principalOid.equals(objectOid)) {
                traceApplicable(ctx, "match on principal OID (%s)", objectOid);
                return true;
            } else if (ctx.getSelfOids().contains(objectOid)) {
                traceApplicable(ctx, "match on other 'self OID' (%s)", objectOid);
                return true;
            } else {
                traceNotApplicable(
                        ctx, "self OIDs: %s, object OID %s", ctx.getSelfOids(), objectOid);
                return false;
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean relationMatches(@NotNull Referencable referencable, @NotNull MatchingContext ctx) {
        QName objectRelation = referencable.getRelation();
        if (relationRegistry.isDelegation(objectRelation)) {
            return true;
        } else {
            traceNotApplicable(ctx, "Relation is not a deputy: %s", objectRelation);
            return false;
        }
    }

    private AssignmentType toAssignmentIfPossible(PrismValue value) {
        if (!(value instanceof PrismContainerValue<?> pcv)) {
            return null;
        }
        var clazz = pcv.getCompileTimeClass();
        if (clazz == null || !AssignmentType.class.isAssignableFrom(clazz)) {
            return null;
        }
        return (AssignmentType) pcv.asContainerable();
    }

    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) {
        if (!ObjectType.class.isAssignableFrom(ctx.getRestrictedType())) {
            traceNotApplicable(ctx, "not an object type");
            return false;
        }

        String principalOid = ctx.getPrincipalOid();
        if (principalOid == null) {
            traceNotApplicable(ctx, "no principal");
            return false;
        }

        // TODO other self OIDs?

        addConjunct(ctx, PrismContext.get().queryFactory().createInOid(principalOid));
        return true;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        // nothing to do here
    }

    @Override
    public String toString() {
        return "SelfClause{}";
    }

    enum Matching {
        OBJECT, DEPUTY_ASSIGNMENT, DEPUTY_REFERENCE
    }
}
