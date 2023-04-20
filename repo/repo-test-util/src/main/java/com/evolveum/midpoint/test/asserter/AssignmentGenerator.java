/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

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
}
