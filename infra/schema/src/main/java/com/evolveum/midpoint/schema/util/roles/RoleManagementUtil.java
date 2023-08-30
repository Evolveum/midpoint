/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.roles;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.binding.AbstractReferencable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * General methods useful for role analysis and management, e.g. determination of "is induced by" relations between roles.
 */
public class RoleManagementUtil {

    /**
     * Returns OIDs of roles induced by a given role.
     * To be used e.g. for replacement of application role assignments by an equivalent business role assignment.
     *
     * TODO should we consider e.g. order constraints here? probably yes
     */
    public static @NotNull Set<String> getInducedRolesOids(@NotNull AbstractRoleType role) {
        return role.getInducement().stream()
                .map(AssignmentType::getTargetRef)
                .filter(Objects::nonNull) // non-role inducements (e.g. policy rules, constructions, etc)
                .map(AbstractReferencable::getOid)
                .filter(Objects::nonNull) // dynamically resolved references
                .collect(Collectors.toSet());
    }

    /**
     * Selects assignments that match the collection of role OIDs, e.g. when dealing with a migration from application
     * to business roles.
     *
     * We assume assignments have explicit OIDs, i.e. no dynamic references here.
     *
     * TODO review this assumption:
     *  - Currently, only the default relation is taken into account here.
     *
     * Roughly related: `UnassignExecutor`
     */
    public static @NotNull List<AssignmentType> getMatchingAssignments(
            @NotNull List<AssignmentType> assignments,
            @NotNull Collection<String> targetOids) {
        return assignments.stream()
                .filter(a -> assignmentMatches(a, targetOids))
                .toList();
    }

    private static boolean assignmentMatches(@NotNull AssignmentType assignment, @NotNull Collection<String> targetOids) {
        ObjectReferenceType targetRef = assignment.getTargetRef();
        return targetRef != null
                && PrismContext.get().isDefaultRelation(targetRef.getRelation())
                && targetOids.contains(targetRef.getOid());
    }
}
