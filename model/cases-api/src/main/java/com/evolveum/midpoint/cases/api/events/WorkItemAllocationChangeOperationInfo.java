/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.events;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemOperationKindType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Primarily used to simplify passing parameters to {@link CaseEventCreationListener}.
 */
public class WorkItemAllocationChangeOperationInfo extends WorkItemOperationInfo {

    @NotNull private final List<ObjectReferenceType> currentActors;
    @Nullable private final List<ObjectReferenceType> newActors;

    public WorkItemAllocationChangeOperationInfo(
            WorkItemOperationKindType operationKind,
            @NotNull List<ObjectReferenceType> currentActors, @Nullable List<ObjectReferenceType> newActors) {
        super(operationKind);
        this.currentActors = currentActors;
        this.newActors = newActors;
    }

    @NotNull
    public List<ObjectReferenceType> getCurrentActors() {
        return currentActors;
    }

    @Nullable
    public List<ObjectReferenceType> getNewActors() {
        return newActors;
    }
}
