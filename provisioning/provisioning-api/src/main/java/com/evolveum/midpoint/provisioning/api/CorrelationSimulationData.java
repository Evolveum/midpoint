/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.provisioning.api;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class CorrelationSimulationData implements SimulationData {

    private final ShadowType shadowBefore;
    private final ObjectDelta<ShadowType> delta;

    public CorrelationSimulationData(ShadowType shadowBefore, ObjectDelta<ShadowType> delta) {
        this.shadowBefore = shadowBefore;
        this.delta = delta;
    }

    public ShadowType getShadowBefore() {
        return shadowBefore;
    }

    public ObjectDelta<ShadowType> getDelta() {
        return delta;
    }

    @Override
    public String toString() {
        return "CorrelationSimulationData{" +
                "shadowBefore=" + shadowBefore +
                ", delta=" + delta +
                '}';
    }
}
