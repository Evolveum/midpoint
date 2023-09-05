/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

/**
 * When new assignments are being created (either as part of focus "add" or "modify" operation), we need to know their PCV IDs
 * beforehand. Otherwise, we are not able to create final `roleMembershipRef` value metadata for them (as they contain complete
 * assignment paths), resulting in multiple execution-stage audit events: first one with no-ID assignment paths in metadata,
 * and second one with correct assignment paths (having IDs). See MID-8659.
 *
 * The solution lies in acquiring assignment PCV IDs in earlier stages of the processing (from the repository).
 *
 * However, we _do not_ store the IDs in the assignments themselves, as their are sometimes re-generated (e.g. by mappings),
 * and the IDs could be easily lost. Hence, we store these IDs in {@link #generatedIdsMap} here instead. They are stored
 * in {@link EvaluatedAssignmentImpl#externalAssignmentId} (and at other similar places), and provided to appropriate places
 * during the computations; and, of course, implanted into deltas before the execution, so they are persistently stored
 * in the repository.
 *
 * NOTE: {@link TemporaryContainerIdStore} addresses a similar problem. However, in that case, IDs are not really important,
 * and they can be safely discarded when the delta is executed. (Unlike this case, where we need to keep them.)
 */
public class AssignmentIdStore implements ShortDumpable {

    /** The map keys are parent-less assignments, immutable (to avoid hashCode being inadvertently changed). */
    @NotNull private final Map<AssignmentType, Long> generatedIdsMap = new HashMap<>();

    public Long getKnownExternalId(AssignmentType assignment) {
        return generatedIdsMap.get(assignment);
    }

    public void put(AssignmentType assignment, Long newId) {
        AssignmentType cloned = assignment.clone();
        cloned.freeze();
        generatedIdsMap.put(cloned, newId);
    }

    public boolean isEmpty() {
        return generatedIdsMap.isEmpty();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append("AssignmentIdStore(");
        sb.append(generatedIdsMap.values());
        sb.append(")");
    }
}
