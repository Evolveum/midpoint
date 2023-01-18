package com.evolveum.midpoint.model.impl.simulation;

import java.util.*;

import com.evolveum.midpoint.task.api.ObjectProcessingListener;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

public class SimulationResultContextImpl implements SimulationResultContext, ObjectProcessingListener {

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
    public <O extends ObjectType> void onObjectProcessed(
            @Nullable O stateBefore,
            @Nullable ObjectDelta<O> executedDelta,
            @Nullable ObjectDelta<O> simulatedDelta,
            @NotNull Collection<String> eventTags,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            ProcessedObjectImpl<?> processedObject = ProcessedObjectImpl.create(stateBefore, simulatedDelta, eventTags);
            if (processedObject != null) {
                manager.storeProcessedObject(oid, processedObject, task, result);
            }
        } catch (SchemaException | ObjectNotFoundException e) {
            throw SystemException.unexpected(e, "when storing processed object information"); // Or should we ignore it?
        }
    }

    @Override
    public @NotNull ObjectProcessingListener objectProcessingListener() {
        return this;
    }

    @Override
    public @NotNull String getResultOid() {
        return oid;
    }

    @Override
    public @NotNull Collection<ProcessedObjectImpl<?>> getStoredProcessedObjects(OperationResult result) throws SchemaException {
        return manager.getStoredProcessedObjects(oid, result);
    }
}
