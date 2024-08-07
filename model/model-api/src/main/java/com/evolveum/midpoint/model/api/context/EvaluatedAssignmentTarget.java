/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public interface EvaluatedAssignmentTarget extends DebugDumpable, Serializable {

    @NotNull
    PrismObject<? extends AssignmentHolderType> getTarget();

    boolean isDirectlyAssigned();

    // if this target applies to focus (by direct assignment or by some inducement)
    // currently matches only default (member) relations - TODO clarify this
    boolean appliesToFocus();

    /**
     * Returns {@code true} if this target applies to focus (by direct assignment or by some inducement).
     * For the first non-delegation assignment path segment accepts all relations.
     * The result is similar to those of {@code roleMembershipRef} plus {@code delegationRef}
     */
    boolean appliesToFocusWithAnyRelation(RelationRegistry relationRegistry);

    /**
     * True for roles whose constructions are evaluated - i.e. those roles that are considered to be applied
     * to the focal object (e.g. to the user).
      */
    boolean isEvaluateConstructions();

    /**
     * An assignment which assigns the given role (useful for knowing e.g. tenantRef or orgRef).
     * TODO consider providing here also the "magic assignment"
     * (https://docs.evolveum.com/midpoint/reference/roles-policies/assignment/configuration/#construction-variables)
     */
    AssignmentType getAssignment();

    @NotNull
    AssignmentPath getAssignmentPath();

    boolean isValid();
}
