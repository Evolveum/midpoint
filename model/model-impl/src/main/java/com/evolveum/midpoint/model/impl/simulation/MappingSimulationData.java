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

public class MappingSimulationData implements SimulationData {

    private final FocusType focusBefore;
    private final ShadowType shadow;
    private final ObjectDelta<FocusType> simulationDelta;
    private final OperationResult mappingEvaluationResult;

    public MappingSimulationData(FocusType focusBefore, ShadowType shadow, @Nullable ObjectDelta<FocusType> simulationDelta,
            OperationResult mappingEvaluationResult) {
        this.focusBefore = focusBefore;
        this.shadow = shadow;
        this.simulationDelta = simulationDelta;
        this.mappingEvaluationResult = mappingEvaluationResult;
    }

    public FocusType getFocusBefore() {
        return this.focusBefore;
    }

    public ShadowType getShadow() {
        return this.shadow;
    }

    public OperationResult getMappingEvaluationResult() {
        return this.mappingEvaluationResult;
    }

    public Optional<ObjectDelta<FocusType>> getSimulationDelta() {
        return Optional.ofNullable(this.simulationDelta);
    }

}
