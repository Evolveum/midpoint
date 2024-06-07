/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/** Information about assignments added, matched, and marked for deletion by associations processing. */
public class AssignmentsProcessingContext {

    /**
     * These assignments should be added because of the complex value processing.
     * They should have temporary (negative) IDs. The ID is also used as a key here.
     */
    @NotNull private final Map<Long, AssignmentType> assignmentsToAdd = new HashMap<>();

    /** These assignments (pointed to by IDs) were correlated to by association inbounds. We will not delete them. */
    @NotNull private final Set<Long> assignmentsToKeep = new HashSet<>();

    /**
     * These assignments (pointed to by IDs) should be deleted, unless there is a reason to keep them,
     * like being in {@link #assignmentsToKeep}.
     */
    @NotNull private final Set<Long> assignmentsToDelete = new HashSet<>();

    @NotNull Map<Long, AssignmentType> getAssignmentsToAdd() {
        return assignmentsToAdd;
    }

    @NotNull Set<Long> getAssignmentsToKeep() {
        return assignmentsToKeep;
    }

    @NotNull Set<Long> getAssignmentsToDelete() {
        return assignmentsToDelete;
    }

    public void addAssignmentsToKeep(Set<Long> assignmentsToKeep) {
        this.assignmentsToKeep.addAll(assignmentsToKeep);
    }

    public void addAssignmentsToDelete(Set<Long> assignmentsToDelete) {
        this.assignmentsToDelete.addAll(assignmentsToDelete);
    }

    public void addAssignmentToAdd(AssignmentType assignment) {
        var id = stateNonNull(assignment.getId(), "No ID in %s", assignment);
        var existing = assignmentsToAdd.put(id, assignment);
        stateCheck(existing == null, "Adding assignment twice? %s vs %s", existing, assignment);
    }
}
