/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.task.api.SimulationData;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * {@link SimulationData} representing a single delta (in a context). Used to implement "compare-mode" mappings/items.
 */
@Experimental
public class SingleDeltaSimulationDataImpl<E extends ObjectType> implements SimulationData {

    @NotNull private final LensElementContext<E> elementContext;
    @NotNull private final ObjectDelta<E> simulationDelta;

    private SingleDeltaSimulationDataImpl(
            @NotNull LensElementContext<E> elementContext, @NotNull ObjectDelta<E> simulationDelta) {
        this.elementContext = elementContext;
        this.simulationDelta = simulationDelta;
    }

    public static <E extends ObjectType> SingleDeltaSimulationDataImpl<E> of(
            @NotNull LensElementContext<E> elementContext, @NotNull ObjectDelta<E> simulationDelta) {
        return new SingleDeltaSimulationDataImpl<>(elementContext, simulationDelta);
    }

    public @NotNull LensElementContext<E> getElementContext() {
        return elementContext;
    }

    @NotNull ObjectDelta<E> getSimulationDelta() {
        return simulationDelta;
    }
}
