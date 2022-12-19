/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.test;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.AggregatedObjectProcessingListener;
import com.evolveum.midpoint.test.DummyAuditEventListener;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO
 */
@Experimental
public class SimulationResult {

    @Nullable private final SimulationResultContext simulationResultContext;
    private final List<ObjectDelta<?>> executedDeltas = new ArrayList<>();
    private final List<ObjectDeltaOperation<?>> auditedDeltas = new ArrayList<>();
    private final List<ProcessedObject<?>> simulatedObjects = new ArrayList<>();
    private ModelContext<?> lastModelContext;

    SimulationResult(@Nullable SimulationResultContext simulationResultContext) {
        this.simulationResultContext = simulationResultContext;
    }

    public List<ObjectDelta<?>> getExecutedDeltas() {
        return executedDeltas;
    }

    public List<ObjectDelta<?>> getSimulatedDeltas() {
        return simulatedObjects.stream()
                .map(ProcessedObject::getDelta)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Collection<String> getTagsForObjectType(@NotNull Class<? extends ObjectType> type) {
        return simulatedObjects.stream()
                .filter(o -> type.equals(o.getType()))
                .flatMap(o -> o.getEventTags().stream())
                .collect(Collectors.toSet());
    }

    public ModelContext<?> getLastModelContext() {
        return lastModelContext;
    }

    ProgressListener contextRecordingListener() {
        return new ProgressListener() {
            @Override
            public void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation) {
                lastModelContext = modelContext;
            }

            @Override
            public boolean isAbortRequested() {
                return false;
            }
        };
    }

    AggregatedObjectProcessingListener aggregatedObjectProcessingListener() {
        return this::onItemProcessed;
    }

    DummyAuditEventListener auditEventListener() {
        return record -> auditedDeltas.addAll(record.getDeltas());
    }

    private <O extends ObjectType> void onItemProcessed(
            @Nullable O stateBefore,
            @Nullable ObjectDelta<O> executedDelta,
            @Nullable ObjectDelta<O> simulatedDelta,
            @NotNull Collection<String> eventTags,
            @NotNull OperationResult ignored) throws SchemaException {
        if (executedDelta != null) {
            executedDeltas.add(executedDelta);
        }
        ProcessedObject<?> processedObject = ProcessedObject.create(stateBefore, simulatedDelta, eventTags);
        if (processedObject != null) {
            simulatedObjects.add(processedObject);
        }
    }

    public void assertNoExecutedNorAuditedDeltas() {
        // This is a bit fake. We currently do not report executed deltas using onItemProcessed method.
        assertThat(executedDeltas).as("executed deltas").isEmpty();

        // In a similar way, auditing is currently explicitly turned off in non-persistent mode. Nevertheless, this
        // could catch situations when (e.g. embedded) clockwork is executed in persistent mode.
        assertThat(auditedDeltas).as("audited deltas").isEmpty();
    }

    public Collection<ObjectDelta<?>> getStoredDeltas(OperationResult result) throws SchemaException {
        return simulationResultContext != null ?
                simulationResultContext.getStoredDeltas(result) : List.of();
    }
}
