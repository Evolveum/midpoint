/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.task.api.SimulationData;

/**
 * {@link SimulationData} for the whole clockwork operation.
 */
public class FullOperationSimulationDataImpl implements SimulationData {

    @NotNull private final LensContext<?> lensContext;
    @NotNull private final OperationResult resultToRecord;

    private FullOperationSimulationDataImpl(@NotNull LensContext<?> lensContext, @NotNull OperationResult resultToRecord) {
        assert resultToRecord.isClosed();
        this.lensContext = lensContext;
        this.resultToRecord = resultToRecord;
    }

    public static FullOperationSimulationDataImpl with(
            @NotNull LensContext<?> lensContext, @NotNull OperationResult resultToRecord) {
        return new FullOperationSimulationDataImpl(lensContext, resultToRecord);
    }

    public @NotNull LensContext<?> getLensContext() {
        return lensContext;
    }

    @NotNull OperationResult getResultToRecord() {
        return resultToRecord;
    }
}
