/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Generates assignments (in memory).
 *
 * Typical use: performance tests.
 *
 * Extend as necessary.
 */
public class AssignmentGenerator {

    private final Function<Integer, String> targetOidFunction = (i) -> UUID.randomUUID().toString();
    private final Function<Integer, QName> targetTypeFunction = (i) -> RoleType.COMPLEX_TYPE;
    private final Function<Integer, String> descriptionFunction = (i) -> "assignment " + i;

    public static AssignmentGenerator withDefaults() {
        return new AssignmentGenerator();
    }

    public void populateAssignments(AssignmentHolderType object, int numberOfAssignments) {
        List<AssignmentType> assignments = object.getAssignment();
        for (int i = 0; i < numberOfAssignments; i++) {
            assignments.add(
                    new AssignmentType()
                            .description(descriptionFunction.apply(i))
                            .targetRef(targetOidFunction.apply(i), targetTypeFunction.apply(i)));
        }
    }

    /**
     * For objects that are not going through the clockwork, we (very simplistically) create
     * `roleMembershipRef` and `archetypeRef` values from assignments.
     *
     * Assuming all assignments are valid.
     *
     * TODO improve if needed
     */
    public void createRoleRefs(AssignmentHolderType object) {
        List<ObjectReferenceType> roleMembershipRefList = object.getRoleMembershipRef();
        List<ObjectReferenceType> archetypeRefList = object.getArchetypeRef();
        for (AssignmentType assignment : object.getAssignment()) {
            ObjectReferenceType targetRef = assignment.getTargetRef();
            if (targetRef != null) {
                roleMembershipRefList.add(targetRef.clone());
                if (ArchetypeType.COMPLEX_TYPE.equals(targetRef.getType())) {
                    archetypeRefList.add(targetRef.clone());
                }
            }
        }
    }
}
