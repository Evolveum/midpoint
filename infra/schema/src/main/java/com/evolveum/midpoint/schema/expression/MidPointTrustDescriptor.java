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
 * == Open questions
 *
 * We implicitly assume that the prism objects (of which configuration items are parts) come from the repository, where they
 * were stored according to the respective authorizations. (It is, after all, a necessary condition to use the origin as a basis
 * for expression profile determination!)
 *
 * But, then, what about (full) objects coming not from the repository but from external sources?
 */
@NullMarked
public interface MidPointTrustDescriptor extends TrustDescriptor {

    /**
     * Denotes an object that is fully trusted. Typically because it is created by midPoint itself, from trusted sources.
     *
     * USE WITH CARE! If misused, it circumvents the safety checks and can lead to execution of untrusted code.
     */
    static MidPointTrustDescriptor trusted() {
        return explicit(ExpressionProfile.full());
    }

    /**
     * Trust description carrying explicit {@link ExpressionProfile}.
     *
     * USE WITH CARE! If misused, it circumvents the safety checks and can lead to execution of untrusted code.
     */
    static MidPointTrustDescriptor explicit(ExpressionProfile expressionProfile) {
        return new Explicit(expressionProfile);
    }

    /**
     * Trust is determined based on the object type and archetype OIDs, because this is typically what is used
     * in authorizations.
     *
     * The object must come from the repository (and not e.g. via REST or GUI), because we can guarantee that it underwent
     * the authorization checks and is therefore trusted - to the level of its type and archetype(s).
     *
     * NOTE: We use assigned archetype OIDs (and not effective ones) because these are checked in the authorization,
     * see {@link ArchetypeRefClause#matches(PrismValue, MatchingContext)}.
     *
     */
    static MidPointTrustDescriptor forRepositoryObject(ObjectType object) {
        return new RepositoryObject(
                object.getOid(),
                object.getClass(),
                object instanceof AssignmentHolderType assignmentHolder ?
                        Set.copyOf(ObjectTypeUtil.getAssignedArchetypeOids(assignmentHolder)) : Set.of(),
                Set.copyOf(FocusTypeUtil.determineSubTypes(object)));
    }

    /** @see #explicit(ExpressionProfile)  */
    record Explicit(ExpressionProfile expressionProfile) implements MidPointTrustDescriptor {
        @Override
        public String toString() {
            return "Explicit[%s]".formatted(expressionProfile);
        }
    }

    /**
     * @see MidPointTrustDescriptor#forRepositoryObject(ObjectType)
     *
     * @param oid For diagnostic purposes only. It is not used for trust determination.
     */
    record RepositoryObject(
            String oid,
            Class<? extends ObjectType> type,
            Set<String> archetypeOids,
            Set<String> subtypes) implements MidPointTrustDescriptor {

        @Override
        public String toString() {
            return "RepositoryObject[%s: %s, archetypes: %s, subtypes: %s]".formatted(
                    type.getSimpleName(), oid, archetypeOids, subtypes);
        }
    }
}
