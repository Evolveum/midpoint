/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * We recognize several flavors of synchronization tasks: import, reconciliation, live sync, and async update; later maybe others.
 * This enum is used to distinguish them. We do not use archetype OID for this, as that is quite technical and too broad a term.
 * Moreover, here we can attach some common functionality to avoid excessive use of branching ("if" commands).
 */
public enum SynchronizationTaskFlavor {

    IMPORT(
            SystemObjectsType.ARCHETYPE_IMPORT_TASK.value(),
            resourceObjectSet -> new WorkDefinitionsType()
                    ._import(new ImportWorkDefinitionType()
                            .resourceObjects(resourceObjectSet))),

    RECONCILIATION(
            SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value(),
            resourceObjectSet -> new WorkDefinitionsType()
                    .reconciliation(new ReconciliationWorkDefinitionType()
                            .resourceObjects(resourceObjectSet))),

    LIVE_SYNC(
            SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value(),
            resourceObjectSet -> new WorkDefinitionsType()
                    .liveSynchronization(new LiveSyncWorkDefinitionType()
                            .resourceObjects(resourceObjectSet))),

    ASYNC_UPDATE(
            SystemObjectsType.ARCHETYPE_ASYNC_UPDATE_TASK.value(),
            resourceObjectSet -> new WorkDefinitionsType()
                    .asynchronousUpdate(new AsyncUpdateWorkDefinitionType()
                            .updatedResourceObjects(resourceObjectSet)));

    /** Structural archetype corresponding to given flavor */
    @NotNull private final String archetypeOid;

    /** Creator of {@link WorkDefinitionsType} for given flavor. */
    @NotNull private final Function<ResourceObjectSetType, WorkDefinitionsType> workDefinitionsCreator;

    SynchronizationTaskFlavor(
            @NotNull String archetypeOid,
            @NotNull Function<ResourceObjectSetType, WorkDefinitionsType> workDefinitionsCreator) {
        this.archetypeOid = archetypeOid;
        this.workDefinitionsCreator = workDefinitionsCreator;
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull String getArchetypeOid() {
        return archetypeOid;
    }

    public @NotNull WorkDefinitionsType createWorkDefinitions(@Nullable ResourceObjectSetType resourceObjectSet) {
        return workDefinitionsCreator.apply(resourceObjectSet);
    }
}
