package com.evolveum.midpoint.model.impl.simulation;

import java.util.*;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.task.api.AggregatedObjectProcessingListener;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

public class SimulationResultContextImpl implements SimulationResultContext, AggregatedObjectProcessingListener {

    private @NotNull String oid;
    private @NotNull SimulationResultManagerImpl manager;
    private @Nullable SimulationResultType configuration;


    public SimulationResultContextImpl(SimulationResultManagerImpl manager, @NotNull String storedOid, @Nullable SimulationResultType configuration) {
        this.manager = manager;
        this.oid = storedOid;
        this.configuration = configuration;
    }

    @Override
    public <O extends ObjectType> void onItemProcessed(
            @Nullable O stateBefore,
            @Nullable ObjectDelta<O> executedDelta,
            @Nullable ObjectDelta<O> simulatedDelta,
            @NotNull Collection<String> eventTags,
            @NotNull OperationResult result) {
        try {
            ProcessedObject<?> processedObject = ProcessedObject.create(stateBefore, simulatedDelta, eventTags);
            if (processedObject != null) {
                manager.storeProcessedObject(oid, processedObject.toBean(), result);
            }
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when converting delta"); // Or should we ignore it?
        }
    }

    @Override
    public @NotNull AggregatedObjectProcessingListener aggregatedObjectProcessingListener() {
        return this;
    }

    @Override
    public @NotNull String getResultOid() {
        return oid;
    }

    @Override
    public @NotNull Collection<ObjectDelta<?>> getStoredDeltas(OperationResult result) throws SchemaException {
        return manager.getStoredDeltas(oid, result);
    }
}
