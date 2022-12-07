package com.evolveum.midpoint.model.api.simulation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

public interface SimulationResultManager {

    SimulationResultContext newSimulationResult(@Nullable SimulationResultType configuration,
            @NotNull OperationResult parentResult);

    SimulationResultType newConfiguration();




}
