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

/**
 * @author semancik
 *
 */
public interface EvaluatedAssignmentTarget extends DebugDumpable {

    PrismObject<? extends AssignmentHolderType> getTarget();

    boolean isDirectlyAssigned();

    // if this target applies to focus (by direct assignment or by some inducement)
    // currently matches only default (member) relations - TODO clarify this
    boolean appliesToFocus();

    // if this target applies to focus (by direct assignment or by some inducement)
    // accepts all relations
    // TODO clarify this
    boolean appliesToFocusWithAnyRelation(RelationRegistry relationRegistry);

    /**
     * True for roles whose constructions are evaluated - i.e. those roles that are considered to be applied
     * to the focal object (e.g. to the user).
      */
    boolean isEvaluateConstructions();

    /**
     * An assignment which assigns the given role (useful for knowing e.g. tenantRef or orgRef).
     * TODO consider providing here also the "magic assignment"
     * (https://wiki.evolveum.com/display/midPoint/Assignment+Configuration#AssignmentConfiguration-ConstructionVariables)
     */
    AssignmentType getAssignment();

    @NotNull
    AssignmentPath getAssignmentPath();

    boolean isValid();
}
