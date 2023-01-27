/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

/**
 * A chunk of data transmitted from the Clockwork to Simulation result manager
 * via {@link Task#acceptSimulationData(SimulationData, OperationResult)} method.
 */
public class SimulationDataImpl implements SimulationData {

    @NotNull private final LensContext<?> lensContext;

    private SimulationDataImpl(@NotNull LensContext<?> lensContext) {
        this.lensContext = lensContext;
    }

    public static SimulationDataImpl with(@NotNull LensContext<?> lensContext) {
        return new SimulationDataImpl(lensContext);
    }

    public @NotNull LensContext<?> getLensContext() {
        return lensContext;
    }
}
