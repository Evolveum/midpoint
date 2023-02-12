/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/** TODO */
public class ShadowSimulationData implements SimulationData {

    @NotNull private final ShadowType shadowBefore;
    @NotNull private final ObjectDelta<ShadowType> delta;

    private ShadowSimulationData(
            @NotNull ShadowType shadowBefore, @NotNull ObjectDelta<ShadowType> delta) {
        this.shadowBefore = shadowBefore;
        this.delta = delta;
    }

    public static ShadowSimulationData of(@NotNull ShadowType shadow, @NotNull Collection<ItemDelta<?,?>> itemDeltas) {
        ObjectDelta<ShadowType> delta = shadow.asPrismObject().createModifyDelta();
        delta.addModifications(itemDeltas);
        return new ShadowSimulationData(shadow, delta);
    }

    public @NotNull ShadowType getShadowBefore() {
        return shadowBefore;
    }

    public @NotNull ObjectDelta<ShadowType> getDelta() {
        return delta;
    }

    @Override
    public String toString() {
        return "ShadowSimulationData{" +
                "shadowBefore=" + shadowBefore +
                ", delta=" + delta +
                '}';
    }
}
