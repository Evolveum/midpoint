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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.task.api.ChangeExecutionListener;
import com.evolveum.midpoint.util.annotation.Experimental;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TODO
 */
@Experimental
public class SimulationResult {

    private final List<ObjectDelta<?>> simulatedDeltas = new ArrayList<>();
    private final List<ObjectDelta<?>> executedDeltas = new ArrayList<>();
    private ModelContext<?> lastModelContext;

    public List<ObjectDelta<?>> getSimulatedDeltas() {
        return simulatedDeltas;
    }

    public List<ObjectDelta<?>> getExecutedDeltas() {
        return executedDeltas;
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

    ChangeExecutionListener changeExecutionListener() {
        return (delta, executed, result) -> {
            if (executed) {
                executedDeltas.add(delta);
            } else {
                simulatedDeltas.add(delta);
            }
        };
    }

    public void assertNoExecutedDeltas() {
        assertThat(executedDeltas).as("executed deltas").isEmpty();
    }
}
