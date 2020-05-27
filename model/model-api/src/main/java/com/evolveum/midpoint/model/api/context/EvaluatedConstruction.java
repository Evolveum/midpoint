/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.context;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Facade interface that provides insight about construction that was evaluted by projector code.
 * It is used for several purposes, e.g. to display all evaluated assignments, both direct and indirect.
 *
 * WARNING: Implementation of this interface are NOT required to be Serializable.
 * They contain "live" data used by projector computation.
 * Do NOT store this object in web session.
 *
 * @author mederly
 * @author Radovan Semancik
 */
public interface EvaluatedConstruction extends DebugDumpable {

    PrismObject<ResourceType> getResource();

    @NotNull
    ShadowKindType getKind();

    String getIntent();

    @Nullable
    String getTag();

    boolean isDirectlyAssigned();

    AssignmentPath getAssignmentPath();

    boolean isWeak();
}
