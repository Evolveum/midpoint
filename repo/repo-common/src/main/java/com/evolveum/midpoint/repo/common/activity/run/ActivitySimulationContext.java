/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
