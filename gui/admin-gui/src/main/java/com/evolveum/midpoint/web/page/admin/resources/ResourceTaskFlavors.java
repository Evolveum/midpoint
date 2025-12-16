/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.evolveum.midpoint.web.component.util.SerializableBiFunction;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public final class ResourceTaskFlavors<T> implements ResourceTaskFlavor<T> {
    public static final ResourceTaskFlavor<CorrelatorsDefinitionType> CORRELATION_PREVIEW_ACIVITY =
            new ResourceTaskFlavors<>("Correlation", (resourceObjectSetType, correlators) ->
                    new WorkDefinitionsType()
                            .correlation(new CorrelationWorkDefinitionType()
                                    .resourceObjects(resourceObjectSetType)
                                    .correlators(correlators))
    );

    public static final ResourceTaskFlavor<Void> IMPORT = SynchronizationTaskFlavor.IMPORT;
    public static final ResourceTaskFlavor<Void> RECONCILIATION = SynchronizationTaskFlavor.RECONCILIATION;
    public static final ResourceTaskFlavor<Void> LIVE_SYNC = SynchronizationTaskFlavor.LIVE_SYNC;
    public static final ResourceTaskFlavor<Void> ASYNC_UPDATE = SynchronizationTaskFlavor.ASYNC_UPDATE;
    public static final ResourceTaskFlavor<Void> SHADOW_RECLASSIFICATION =
            SynchronizationTaskFlavor.SHADOW_RECLASSIFICATION;
    private static final Map<String, ResourceTaskFlavor<?>> FACTORIES_BY_ARHETYPE =
            Arrays.stream(SynchronizationTaskFlavor.values())
                    .collect(Collectors.toMap(SynchronizationTaskFlavor::getArchetypeOid, factory -> factory));

    private final SerializableBiFunction<ResourceObjectSetType, T, WorkDefinitionsType> workDefinitionsFactory;
    private final String name;

    public static ResourceTaskFlavor<?> forArchetype(String archetypeId) {
        return FACTORIES_BY_ARHETYPE.get(archetypeId);
    }

    ResourceTaskFlavors(String name, SerializableBiFunction<ResourceObjectSetType, T, WorkDefinitionsType> workDefinitionsFactory) {
        this.name = name;
        this.workDefinitionsFactory = workDefinitionsFactory;
    }

    @Override
    public WorkDefinitionsType createWorkDefinitions(@Nullable ResourceObjectSetType resourceObjectSet,
            @Nullable T configuration) {
        return this.workDefinitionsFactory.apply(resourceObjectSet, configuration);
    }

    @Override
    public String flavorName() {
        return this.name;
    }

}
