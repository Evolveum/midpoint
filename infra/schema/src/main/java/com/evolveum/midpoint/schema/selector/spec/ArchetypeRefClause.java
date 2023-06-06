/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.selector.spec;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.getOidsFromRefs;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import java.util.*;

import com.evolveum.midpoint.schema.selector.eval.ClauseFilteringContext;
import com.evolveum.midpoint.schema.selector.eval.ClauseMatchingContext;
import com.evolveum.midpoint.util.DebugUtil;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

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

    @Override
    public boolean matches(@NotNull PrismValue value, @NotNull ClauseMatchingContext ctx) {
        Object realValue = value.getRealValueIfExists();
        if (realValue instanceof AssignmentHolderType) {
            Collection<String> actualArchetypeOids =
                    getOidsFromRefs(
                            ((AssignmentHolderType) realValue).getArchetypeRef());
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

    @Override
    public boolean applyFilter(@NotNull ClauseFilteringContext ctx) throws SchemaException {
        addConjunct(
                ctx,
                PrismContext.get().queryFor(AssignmentHolderType.class)
                        .item(AssignmentHolderType.F_ARCHETYPE_REF)
                        .ref(archetypeOids.toArray(new String[0]))
                        .buildFilter());
        return true;
    }

    @Override
    void addDebugDumpContent(StringBuilder sb, int indent) {
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "oids", archetypeOids, indent + 1);
    }
}
