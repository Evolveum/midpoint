/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import java.io.Serializable;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Facade interface that provides insight about construction that was evaluated by projector code.
 * It is used for several purposes, e.g. to display all evaluated assignments, both direct and indirect.
 *
 * WARNING: Implementation of this interface are NOT required to be Serializable.
 * They contain "live" data used by projector computation.
 * Do NOT store this object in web session.
 *
 * @author Radovan Semancik
 */
public interface EvaluatedResourceObjectConstruction extends DebugDumpable, Serializable {

    /**
     * Resource on which the object is to be constructed.
     */
    @NotNull
    PrismObject<ResourceType> getResource();

    /**
     * Kind of the resource object.
     */
    @NotNull ShadowKindType getKind();

    /**
     * Intent of the resource object.
     */
    @NotNull String getIntent();

    default @NotNull ResourceObjectTypeIdentification getTypeIdentification() {
        return ResourceObjectTypeIdentification.of(getKind(), getIntent());
    }

    /**
     * Tag of the resource object (for multiaccounts).
     */
    @Nullable String getTag();

    /**
     * Is the resource object directly assigned to the focus object?
     * (False means there is an inducement in the assignment path.)
     *
     * TODO What about delegations?
     */
    boolean isDirectlyAssigned();

    /**
     * Full path from the focus object to the respective construction.
     */
    AssignmentPath getAssignmentPath();

    /**
     * Is the construction weak (i.e. conditional)?
     * https://docs.evolveum.com/midpoint/reference/roles-policies/assignment/configuration/#strong-and-weak-constructions
     */
    boolean isWeak();
}
