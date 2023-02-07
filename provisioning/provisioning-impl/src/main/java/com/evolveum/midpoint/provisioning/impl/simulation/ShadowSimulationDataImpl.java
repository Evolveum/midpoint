/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.simulation;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class ShadowSimulationDataImpl implements SimulationData {

    @NotNull private final ShadowType shadow;
    @NotNull private final ObjectDelta<ShadowType> delta;
    @NotNull private final Collection<String> eventMarks;

    private ShadowSimulationDataImpl(
            @NotNull ShadowType shadow, @NotNull ObjectDelta<ShadowType> delta, @NotNull Collection<String> eventMarks) {
        this.shadow = shadow;
        this.delta = delta;
        this.eventMarks = eventMarks;
    }

    public static ShadowSimulationDataImpl of(
            @NotNull ShadowType shadow, @NotNull ObjectDelta<ShadowType> delta, @NotNull Collection<String> eventMarks) {
        return new ShadowSimulationDataImpl(shadow, delta, eventMarks);
    }

    public @NotNull ShadowType getShadow() {
        return shadow;
    }

    public @NotNull ObjectDelta<ShadowType> getDelta() {
        return delta;
    }

    public @NotNull Collection<String> getEventMarks() {
        return eventMarks;
    }

    @Override
    public String toString() {
        return "ShadowSimulationDataImpl{" +
                "shadow=" + shadow +
                ", delta=" + delta +
                ", eventMarks=" + eventMarks +
                '}';
    }
}
