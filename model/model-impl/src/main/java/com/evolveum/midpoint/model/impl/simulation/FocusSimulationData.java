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
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class FocusSimulationData implements SimulationData {

    private final FocusType focusBefore;
    private final ObjectDelta<FocusType> simulationDelta;

    public FocusSimulationData(FocusType focusBefore, @Nullable ObjectDelta<FocusType> simulationDelta) {
        this.focusBefore = focusBefore;
        this.simulationDelta = simulationDelta;
    }

    public FocusType getFocusBefore() {
        return this.focusBefore;
    }

    public Optional<ObjectDelta<FocusType>> getSimulationDelta() {
        return Optional.ofNullable(this.simulationDelta);
    }

}
