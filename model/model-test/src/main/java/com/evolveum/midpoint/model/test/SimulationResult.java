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
import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.AggregatedObjectProcessingListener;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO
 */
@Experimental
public class SimulationResult {

    @Nullable private final SimulationResultContext simulationResultContext;
    private final List<ObjectDelta<?>> executedDeltas = new ArrayList<>();
    private final List<ObjectDelta<?>> simulatedDeltas = new ArrayList<>();
    private ModelContext<?> lastModelContext;

    SimulationResult(@Nullable SimulationResultContext simulationResultContext) {
        this.simulationResultContext = simulationResultContext;
    }

    public List<ObjectDelta<?>> getExecutedDeltas() {
        return executedDeltas;
    }

    public List<ObjectDelta<?>> getSimulatedDeltas() {
        return simulatedDeltas;
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

    private <O extends ObjectType> void onItemProcessed(
            @Nullable O stateBefore,
            @Nullable ObjectDelta<O> executedDelta,
            @Nullable ObjectDelta<O> simulatedDelta,
            @NotNull OperationResult result) {
        if (executedDelta != null) {
            executedDeltas.add(executedDelta);
        }
        if (simulatedDelta != null) {
            simulatedDeltas.add(simulatedDelta);
        }
    }

    public void assertNoExecutedDeltas() {
        assertThat(executedDeltas).as("executed deltas").isEmpty();
    }

    public @Nullable SimulationResultContext getSimulationResultContext() {
        return simulationResultContext;
    }

    public Collection<ObjectDelta<?>> getStoredDeltas(OperationResult result) throws SchemaException {
        return simulationResultContext != null ?
                simulationResultContext.getStoredDeltas(result) : List.of();
    }
}
