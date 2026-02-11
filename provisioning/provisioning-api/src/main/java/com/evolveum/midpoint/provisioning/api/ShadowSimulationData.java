/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/** TODO */
public class ShadowSimulationData implements SimulationData {

    @NotNull private final ShadowType shadowBefore;
    private final ObjectDelta<ShadowType> delta;

    private ShadowSimulationData(
            @NotNull ShadowType shadowBefore, ObjectDelta<ShadowType> delta) {
        this.shadowBefore = shadowBefore;
        this.delta = delta;
    }

    // TODO I think that the second parameter should be ObjectDelta, not collection of ItemDelta, because here we can
    //  not be sure, the item deltas are applicable to the shadow type. The caller, on the other hand should have
    //  this knowledge.
    public static ShadowSimulationData of(@NotNull ShadowType shadow, Collection<ItemDelta<?,?>> itemDeltas) {
        if (itemDeltas == null || itemDeltas.isEmpty()) {
            return new ShadowSimulationData(shadow, null);
        }

        ObjectDelta<ShadowType> delta = shadow.asPrismObject().createModifyDelta();
        delta.addModifications(itemDeltas);
        return new ShadowSimulationData(shadow, delta);
    }

    public @NotNull ShadowType getShadowBefore() {
        return shadowBefore;
    }

    public Optional<ObjectDelta<ShadowType>> getDelta() {
        return Optional.ofNullable(delta);
    }

    @Override
    public String toString() {
        return "ShadowSimulationData{" +
                "shadowBefore=" + shadowBefore +
                ", delta=" + delta +
                '}';
    }
}
