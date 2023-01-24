package com.evolveum.midpoint.model.impl.simulation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.task.api.SimulationProcessedObjectListener;
import com.evolveum.midpoint.util.MiscUtil;

public class SimulationResultContextImpl implements SimulationResultContext {

    private final @NotNull String oid;
    private final @NotNull SimulationResultManagerImpl manager;

    // The simulation definition could be here as well. But it would require fetching the simulation result object from
    // the repository (each time the context is created). It is quite acceptable, but let us do it only when really needed.

    SimulationResultContextImpl(
            @NotNull SimulationResultManagerImpl manager,
            @NotNull String storedOid) {
        this.manager = manager;
        this.oid = storedOid;
    }

    @Override
    public @NotNull SimulationProcessedObjectListener getSimulationProcessedObjectListener(@NotNull String transactionId) {
        return (processedObject, task, result) ->
                manager.storeProcessedObject(
                        oid,
                        transactionId,
                        MiscUtil.castSafely(processedObject, ProcessedObjectImpl.class),
                        task, result);
    }

    @Override
    public @NotNull String getResultOid() {
        return oid;
    }
}
