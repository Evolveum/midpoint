/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.task.api.SimulationProcessedObjectListener;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

public interface SimulationResultContext {

    @NotNull SimulationProcessedObjectListener getSimulationProcessedObjectListener(@NotNull String transactionId);

    /** OID of the {@link SimulationResultType} object. */
    @NotNull String getResultOid();
}
