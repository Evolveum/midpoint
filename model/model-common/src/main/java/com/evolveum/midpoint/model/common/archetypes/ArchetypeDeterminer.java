/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.archetypes;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Responsible for determining archetypes for an object.
 *
 * We concentrate on archetype assignments. Starting with 4.6 we ignore `archetypeRef` values.
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
        if (object instanceof AssignmentHolderType assignmentHolder) {
            oids = getArchetypeOidsFromAssignments(assignmentHolder);
        } else {
            oids = Set.of();
        }
        LOGGER.trace("Archetype OIDs determined: {}", oids);
        return oids;
    }

    /**
     * Determines all archetype OIDs in the "dynamic" case where the object is changed during clockwork processing.
     */
    <O extends ObjectType> @NotNull Set<String> determineArchetypeOids(O before, O after) {
        if (after == null) {
            // Object is being deleted. Let us simply take the OIDs from last known version (if there's any).
            return determineArchetypeOids(before);
        } else {
            // Values assigned "at the end" (i.e. in `after` version). This is the authoritative information.
            // Starting with 4.6, we simply ignore archetypeRef values, so it is sufficient to look here.
            return determineArchetypeOids(after);
        }
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
}
