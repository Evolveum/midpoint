package com.evolveum.midpoint.model.impl.simulation;

import java.util.*;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.task.api.AggregatedObjectProcessingListener;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;

import com.google.common.collect.ImmutableMap;

public class SimulationResultContextImpl implements SimulationResultContext, AggregatedObjectProcessingListener {

    private @NotNull String oid;
    private @NotNull SimulationResultManagerImpl manager;
    private @Nullable SimulationResultType configuration;

    private static final Map<ChangeType, ObjectProcessingStateType> DELTA_TO_PROCESSING_STATE = new ImmutableMap.Builder<ChangeType, ObjectProcessingStateType>()
            .put(ChangeType.ADD, ObjectProcessingStateType.ADDED)
            .put(ChangeType.DELETE, ObjectProcessingStateType.DELETED)
            .put(ChangeType.MODIFY, ObjectProcessingStateType.MODIFIED)
            .build();

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
            @NotNull OperationResult result) {
        try {
            SimulationResultProcessedObjectType processedObject = createProcessedObject(stateBefore, simulatedDelta);
            if (processedObject != null) {
                manager.storeProcessedObject(oid, processedObject, result);
            }
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "when converting delta"); // Or should we ignore it?
        }
    }

    private <O extends ObjectType> SimulationResultProcessedObjectType createProcessedObject(
            @Nullable O stateBefore, @Nullable ObjectDelta<O> delta) throws SchemaException {

        if (stateBefore == null && delta == null) {
            return null;
        }

        @Nullable O stateAfter = computeStateAfter(stateBefore, delta);
        @Nullable O anyState = MiscUtil.getFirstNonNull(stateAfter, stateBefore);

        // We may consider returning null if anyState is null (meaning that the delta is MODIFY/DELETE with null stateBefore)

        SimulationResultProcessedObjectType processedObject = new SimulationResultProcessedObjectType();
        processedObject.setOid(determineOid(anyState, delta)); // may be null in strange cases
        if (anyState != null) {
            processedObject.setName(anyState.getName()); // may be null but we don't care
        }

        if (delta != null) {
            processedObject.setState(DELTA_TO_PROCESSING_STATE.get(delta.getChangeType()));
            processedObject.setType(toQName(delta.getObjectTypeClass()));
            processedObject.setDelta(DeltaConvertor.toObjectDeltaType(delta));
        } else {
            processedObject.setState(ObjectProcessingStateType.UNMODIFIED);
        }

        // TODO before, after

        addMetrics(delta, processedObject);
        return processedObject;
    }

    private <O extends ObjectType> @Nullable String determineOid(O anyState, ObjectDelta<O> delta) {
        if (anyState != null) {
            String oid = anyState.getOid();
            if (oid != null) {
                return oid;
            }
        }
        if (delta != null) {
            return delta.getOid();
        }
        return null;
    }

    private <O extends ObjectType> O computeStateAfter(O stateBefore, ObjectDelta<O> delta) throws SchemaException {
        if (stateBefore == null) {
            if (delta == null) {
                return null;
            } else if (delta.isAdd()) {
                return delta.getObjectToAdd().asObjectable();
            } else {
                // We may relax this before release - we may still store the delta
                throw new IllegalStateException("No initial state and MODIFY/DELETE delta? Delta: " + delta);
            }
        } else if (delta != null) {
            //noinspection unchecked
            PrismObject<O> clone = (PrismObject<O>) stateBefore.asPrismObject().clone();
            delta.applyTo(clone);
            return clone.asObjectable();
        } else {
            return stateBefore;
        }
    }

    private void addMetrics(@Nullable ObjectDelta<?> delta, SimulationResultProcessedObjectType processedObject) {
    }

    @Override
    public AggregatedObjectProcessingListener aggregatedObjectProcessingListener() {
        return this;
    }

    private QName toQName(Class<?> objectTypeClass) {
        return PrismContext.get().getSchemaRegistry().determineTypeForClass(objectTypeClass);
    }

    @Override
    public @NotNull String getResultOid() {
        return oid;
    }

    @Override
    public @NotNull Collection<ObjectDelta<?>> getStoredDeltas(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return manager.getStoredDeltas(oid, result);
    }
}
