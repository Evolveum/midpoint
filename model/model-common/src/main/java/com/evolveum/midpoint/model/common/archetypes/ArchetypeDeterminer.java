/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.archetypes;

import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Responsible for determining archetypes for an object.
 *
 * Basically, we concentrate on archetype assignments. But - to be extra safe - we also look at `archetypeRef` values,
 * but with caution (in cases where object is being modified). See the code.
 */
@Component
class ArchetypeDeterminer {

    private static final Trace LOGGER = TraceManager.getTrace(ArchetypeDeterminer.class);

    @Autowired private RelationRegistry relationRegistry;

    /**
     * Determines all archetype OIDs (structural + auxiliary) for a given (static) object.
     */
    @NotNull Set<String> determineArchetypeOids(@Nullable ObjectType object) {
        Set<String> oids;
        if (object instanceof AssignmentHolderType) {
            AssignmentHolderType assignmentHolder = (AssignmentHolderType) object;
            // To be safe, we look also at archetypeRef values. This may change in the future.
            oids = Sets.union(
                    getArchetypeOidsFromAssignments(assignmentHolder),
                    determineArchetypeOidsFromArchetypeRef(assignmentHolder));
        } else {
            oids = Set.of();
        }
        LOGGER.trace("Archetype OIDs determined (no-change case): {}", oids);
        return oids;
    }

    /**
     * Determines all archetype OIDs in the "dynamic" case where the object is changed during clockwork processing.
     */
    <O extends ObjectType> @NotNull Set<String> determineArchetypeOids(O before, O after) {

        // First, let us sort out static cases, where there is no change in the object internals.

        if (after == null) {
            // Object is being deleted. Let us simply take the OIDs from last known version (if there's any).
            return determineArchetypeOids(before);
        }

        if (before == null) {
            // Object is being added, and we have no "before" version. Reduces to the static case just as above.
            return determineArchetypeOids(after);
        }

        // Here we know we have (some) change. Let's sort things out.

        if (!(after instanceof AssignmentHolderType)) {
            assert !(before instanceof AssignmentHolderType); // We know the clockwork does not change the type of objects.
            return Set.of();
        }

        return determineArchetypeOids(
                (AssignmentHolderType) before,
                (AssignmentHolderType) after);
    }

    private @NotNull Set<String> determineArchetypeOids(
            @NotNull AssignmentHolderType before, @NotNull AssignmentHolderType after) {

        // Values assigned "at the end" (i.e. in `after` version). This is usually the authoritative information.
        Set<String> assignedOidsAfter = getArchetypeOidsFromAssignments(after);

        // We look at archetypeRef because there may be rare (theoretical?) cases when we have archetypeRefs
        // without corresponding assignments. But we have to be careful to select only relevant OIDs!
        Set<String> relevantOidsFromRef = getRelevantArchetypeOidsFromArchetypeRef(before, after, assignedOidsAfter);

        // Finally, we take a union of these sets. Note that they usually overlap.
        var oids = Sets.union(
                assignedOidsAfter,
                relevantOidsFromRef);

        LOGGER.trace("Archetype OIDs determined (dynamic case): {}", oids);
        return oids;
    }

    private @NotNull Set<String> getArchetypeOidsFromAssignments(AssignmentHolderType assignmentHolder) {
        var oids = assignmentHolder.getAssignment().stream()
                .map(AssignmentType::getTargetRef)
                .filter(Objects::nonNull)
                .filter(ref -> QNameUtil.match(ArchetypeType.COMPLEX_TYPE, ref.getType()))
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .map(AbstractReferencable::getOid)
                .collect(Collectors.toSet());
        stateCheck(!oids.contains(null),
                "OID-less (e.g. dynamic filter based) archetype assignments are not supported; in %s", assignmentHolder);
        LOGGER.trace("Assigned archetype OIDs: {}", oids);
        return oids;
    }

    /**
     * Selects relevant archetype OIDs from `archetypeRef`. We must take care here because this information may be out of date
     * in situations where we try to determine archetypes _before_ the assignment evaluator updates `archetypeRef` values.
     * (Even the values in `after` version of the object are not up-to-date in that case.)
     *
     * The best we can do is to eliminate any `archetypeRef` OIDs that were assigned "before" but are not assigned "after":
     * meaning they are unassigned by some (primary/secondary) delta.
     */
    private Set<String> getRelevantArchetypeOidsFromArchetypeRef(
            AssignmentHolderType before, AssignmentHolderType after, Set<String> assignedOidsAfter) {

        Set<String> assignedOidsBefore = getArchetypeOidsFromAssignments(before);

        // These are OIDs that were assigned at the beginning, but are no longer assigned. These are forbidden to use.
        Set<String> unassignedOids = Sets.difference(assignedOidsBefore, assignedOidsAfter);

        // These are the relevant OIDs: they are in (presumed ~ "after") archetypeRef and were not unassigned.
        var relevant = Sets.difference(
                determineArchetypeOidsFromArchetypeRef(after),
                unassignedOids);

        LOGGER.trace("Relevant archetype OIDs from archetypeRef: {}", relevant);
        return relevant;
    }

    private @NotNull Set<String> determineArchetypeOidsFromArchetypeRef(AssignmentHolderType assignmentHolder) {
        var oids = assignmentHolder.getArchetypeRef().stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation())) // should be always the case
                .map(AbstractReferencable::getOid)
                .filter(Objects::nonNull) // should be always the case
                .collect(Collectors.toSet());
        LOGGER.trace("'Effective' archetype OIDs (from archetypeRef): {}", oids);
        return oids;
    }
}
