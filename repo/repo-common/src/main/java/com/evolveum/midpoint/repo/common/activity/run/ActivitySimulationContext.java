/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;

/** Represents a running simulation with open simulation result. TODO decide what to do with this */
public class ActivitySimulationContext {

    @NotNull private final String simulationResultOid;

    public ActivitySimulationContext(@NotNull String simulationResultOid) {
        this.simulationResultOid = simulationResultOid;
    }

    public @NotNull String getSimulationResultOid() {
        return simulationResultOid;
    }

    public @NotNull ObjectReferenceType getSimulationResultRef() {
        return ObjectTypeUtil.createObjectRef(simulationResultOid, ObjectTypes.SIMULATION_RESULT);
    }
}
