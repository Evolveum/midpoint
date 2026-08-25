/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.model.impl.simulation;

import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class MappingSimulationData<T extends ObjectType> implements SimulationData {

    private final T objectBefore;
    private final ObjectDelta<T> simulationDelta;
    private final OperationResult mappingEvaluationResult;

    public MappingSimulationData(T objectBefore, @Nullable ObjectDelta<T> simulationDelta,
            OperationResult mappingEvaluationResult) {
        this.objectBefore = objectBefore;
        this.simulationDelta = simulationDelta;
        this.mappingEvaluationResult = mappingEvaluationResult;
    }

    public T getObjectBefore() {
        return this.objectBefore;
    }

    public OperationResult getMappingEvaluationResult() {
        return this.mappingEvaluationResult;
    }

    public Optional<ObjectDelta<T>> getSimulationDelta() {
        return Optional.ofNullable(this.simulationDelta);
    }

}
