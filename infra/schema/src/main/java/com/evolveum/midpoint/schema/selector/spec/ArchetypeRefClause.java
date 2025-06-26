/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import java.util.*;

import com.evolveum.midpoint.schema.selector.eval.FilteringContext;
import com.evolveum.midpoint.schema.selector.eval.MatchingContext;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Processes the `archetypeRef` clause. For checking assignments vs effective archetype references, please see
 * javadoc for {@link #matches(PrismValue, MatchingContext)} method.
 */
public class ArchetypeRefClause extends SelectorClause {

    /** Not empty, no null values. Immutable. */
    @NotNull private final Set<String> archetypeOids;

    private ArchetypeRefClause(@NotNull Set<String> archetypeOids) {
        this.archetypeOids = Collections.unmodifiableSet(archetypeOids);
    }

    static ArchetypeRefClause of(@NotNull List<ObjectReferenceType> archetypeRefList) throws ConfigurationException {
        Preconditions.checkArgument(!archetypeRefList.isEmpty());
        var oids = new HashSet<String>();
        for (ObjectReferenceType ref : archetypeRefList) {
            oids.add(
                    configNonNull(
                            ref.getOid(),
                            "No OID in archetypeRef specification in the selector: %s", ref));
        }
        return new ArchetypeRefClause(oids);
    }

    @Override
    public @NotNull String getName() {
        return "archetypeRef";
    }

    /**
     * Determines whether the archetype(s) do match the selector.
     *
     * We intentionally check assignments instead of "effective" archetype OIDs here. The reasons are:
     *
     * 1. When GUI creates an object, it sets the assignment, not the archetypeRef.
     * 2. Deltas that change the archetype refer to the assignment, not the archetypeRef.
     * (This is important when determining whether we are leaving the zone of control.)
     *
     * The #1 could be worked around by setting the archetypeRef before autz are checked. However, the #2 is more serious.
     *
     * To be seen if this is the correct way forward. An alternative would be to compute the effective object content
     * (archetypeRef, but also e.g. parentOrgRef) before the actual matching. However, this could be quite complex and
     * expensive. So, currently we have no option other than matching the assignments values.
     */
    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull MatchingContext ctx) {
        Object realValue = value.getRealValueIfExists();
        if (realValue instanceof AssignmentHolderType assignmentHolder) {
            Collection<String> actualArchetypeOids = ObjectTypeUtil.getAssignedArchetypeOids(assignmentHolder);
            for (String actualArchetypeOid : actualArchetypeOids) {
                if (archetypeOids.contains(actualArchetypeOid)) {
                    return true;
                }
            }
            traceNotApplicable(
                    ctx, "archetype mismatch, expected %s, was %s",
                    archetypeOids, actualArchetypeOids);
        } else {
            traceNotApplicable(
                    ctx, "archetype mismatch, expected %s but object has none (it is not of AssignmentHolderType)",
                    archetypeOids);
        }
        return false;
    }

    /**
     * Currently, we act upon the effective archetypeRef value, not the value in assignments. It should be far more efficient
     * when running against the repository. Moreover, in the repository, it should be equivalent, as the effective value
     * should precisely reflect the assignments' values there.
     *
     * @see #matches(PrismValue, MatchingContext)
     */
    @Override
    public boolean toFilter(@NotNull FilteringContext ctx) throws SchemaException {
        if (!isApplicable(ctx)) {
            return false;
        }
        addConjunct(
                ctx,
                PrismContext.get().queryFor(AssignmentHolderType.class)
                        .item(AssignmentHolderType.F_ARCHETYPE_REF)
                        .ref(archetypeOids.toArray(new String[0]))
                        .buildFilter());
        return true;
    }

    private boolean isApplicable(@NotNull FilteringContext ctx) {
        if (AssignmentHolderType.class.isAssignableFrom(ctx.getFilterType())) {
            return true;
        }
        if (AssignmentHolderType.class.isAssignableFrom(ctx.getRestrictedType())) {
            return true;
        }
        return false;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "oids", archetypeOids, indent + 1);
    }

    @Override
    public String toString() {
        return "ArchetypeRefClause{archetypeOids=" + archetypeOids + "}";
    }
}
