package com.evolveum.midpoint.model.impl.simulation;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.task.api.SimulationDataConsumer;

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
    public @NotNull SimulationDataConsumer getSimulationDataConsumer(@NotNull String transactionId) {
        return (data, task, result) ->
                manager.storeSimulationData(oid, transactionId, data, task, result);
    }

    @Override
    public @NotNull String getResultOid() {
        return oid;
    }
}
