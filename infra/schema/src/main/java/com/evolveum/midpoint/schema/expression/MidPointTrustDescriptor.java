/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.expression;

import java.util.Set;

import com.evolveum.midpoint.schema.util.FocusTypeUtil;

import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.TrustDescriptor;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.selector.spec.ArchetypeRefClause;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * A {@link TrustDescriptor} specific for midPoint.
 *
 * The most frequent case is {@link #forAuthorizedObject(ObjectType)}. See its documentation for details.
 */
@NullMarked
public interface MidPointTrustDescriptor extends TrustDescriptor {

    /**
     * Trust is determined based on the authorizations to manipulate given object type (optionally with archetype/subtype).
     *
     * When seeing e.g. a trust descriptor for {@code type=FunctionLibraryType}, we can assume that the expression was written
     * by a principal that has the authorization to manipulate function libraries (typically the root user). Therefore, the
     * expression is trusted to the root user level.
     *
     * At the other hand, when seeing e.g. a trust descriptor for {@code type=UserType}, we must assume that the user may
     * have been modified by a principal that has the authorization to manipulate users, in particular their assignments.
     * This can be a power user, or maybe even regular user, when he or she is asking for a new assignment.
     * (Yes, GUI does a kind of filtering that prevents expressions from being entered, but technically, at the model level,
     * the user has an authorization to create an assignment with expressions.) Therefore, expressions from {@code UserType}
     * should not be trusted much.
     *
     * In most cases, the expressions come (along with the whole object) from the repository. It is assumed that when creating
     * or modifying the object, the authorization checks were performed.
     *
     * In other cases, the expressions come from the delta that is to be executed against the repository. (It can be an ADD
     * OBJECT or MODIFY OBJECT delta.) We must NOT execute any expressions from this delta until the request was authorized!
     * But after authorization, we can assume that the expressions are trusted to the level of the object type/archetype/subtype,
     * just as if they came from the repository.
     *
     * NOTE: We use assigned archetype OIDs (and not effective ones) because these are checked in the authorization,
     * see {@link ArchetypeRefClause#matches(PrismValue, MatchingContext)}.
     *
     * USE WITH CARE! Make sure that the authorization checks were really performed on the object.
     */
    static MidPointTrustDescriptor forAuthorizedObject(ObjectType object) {
        return new AuthorizedObject(
                object.getOid(),
                object.getClass(),
                object instanceof AssignmentHolderType assignmentHolder ?
                        Set.copyOf(ObjectTypeUtil.getAssignedArchetypeOids(assignmentHolder)) : Set.of(),
                Set.copyOf(FocusTypeUtil.determineSubTypes(object)));
    }

    /**
     * @see #forAuthorizedObject(ObjectType)
     *
     * @param oid For diagnostic purposes only. It is NOT used for trust determination.
     */
    record AuthorizedObject(
            String oid,
            Class<? extends ObjectType> type,
            Set<String> archetypeOids,
            Set<String> subtypes) implements MidPointTrustDescriptor {

        @Override
        public String toString() {
            return "AuthorizedObject[%s: %s, archetypes: %s, subtypes: %s]".formatted(
                    type.getSimpleName(), oid, archetypeOids, subtypes);
        }
    }

    /**
     * Denotes an object that is assumed to come from the currently authenticated principal. Typically because it comes
     * from a GUI or REST request. So the trust level is determined by the authorizations of the current principal.
     *
     * USE WITH CARE! Make sure that the object indeed comes from the current principal.
     * Otherwise it can lead to execution of untrusted code.
     */
    static MidPointTrustDescriptor forCurrentPrincipal() {
        return new CurrentPrincipal();
    }

    /** @see MidPointTrustDescriptor#forCurrentPrincipal() */
    record CurrentPrincipal() implements MidPointTrustDescriptor {
        @Override
        public String toString() {
            return "CurrentPrincipal";
        }
    }

    /**
     * Denotes an object that is fully trusted. Typically because it is created by midPoint itself, from trusted components.
     *
     * USE WITH CARE! Make sure that the object is indeed generated by midPoint, and that its components are fully trusted.
     * Otherwise it can lead to execution of untrusted code.
     */
    static MidPointTrustDescriptor trusted() {
        return explicit(ExpressionProfile.full());
    }


    /**
     * Trust description carrying explicit {@link ExpressionProfile}.
     *
     * USE WITH CARE! Make sure that the expression profile was determined correctly.
     */
    static MidPointTrustDescriptor explicit(ExpressionProfile expressionProfile) {
        return new Explicit(expressionProfile);
    }

    /** @see #explicit(ExpressionProfile)  */
    record Explicit(ExpressionProfile expressionProfile) implements MidPointTrustDescriptor {
        @Override
        public String toString() {
            return "Explicit[%s]".formatted(expressionProfile);
        }
    }
}
