/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import java.util.List;

public interface SimulationResultManager {

    @NotNull SimulationResultContext newSimulationResult(
            @Nullable SimulationResultType configuration, @NotNull OperationResult parentResult);

    @NotNull List<ProcessedObject<?>> getStoredProcessedObjects(@NotNull String oid, OperationResult result)
            throws SchemaException;

    /** TODO better name */
    SimulationResultContext newSimulationContext(@NotNull String resultOid);

    SimulationResultType newConfiguration();

}
