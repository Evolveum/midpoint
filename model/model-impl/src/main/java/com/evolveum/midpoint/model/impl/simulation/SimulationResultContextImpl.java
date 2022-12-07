package com.evolveum.midpoint.model.impl.simulation;

import java.util.Map;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.simulation.SimulationResultContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ObjectProcessingListener;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;
import com.google.common.collect.ImmutableMap;

public class SimulationResultContextImpl implements SimulationResultContext, ObjectProcessingListener {

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
    public void onChangeExecuted(@NotNull ObjectDelta<?> delta, boolean executed, @NotNull OperationResult result) {
        //
        SimulationResultProcessedObjectType processedObject = createProcessedObject(delta);
        manager.storeProcessedObject(oid, processedObject, result);

    }

    private SimulationResultProcessedObjectType createProcessedObject(@NotNull ObjectDelta<?> delta) {
        SimulationResultProcessedObjectType processedObject = new SimulationResultProcessedObjectType();
        processedObject.setOid(delta.getOid());
        // TODO fill up name

        processedObject.setState(DELTA_TO_PROCESSING_STATE.get(delta.getChangeType()));
        processedObject.setType(toQName(delta.getObjectTypeClass()));

        PrismContainerValue<SimulationResultProcessedObjectType> pcv = processedObject.asPrismContainerValue();
        if (delta.isAdd()) {
            PrismObject<?> objectToAdd = delta.getObjectToAdd();
            processedObject.setName(PolyString.toPolyStringType(objectToAdd.getName()));
            //pcv.findOrCreateContainer(containerName)
        } else if (delta.isDelete()) {
            // TODO: Fill up before state
        }
        return processedObject;
    }

    @Override
    public ObjectProcessingListener objectProcessingListener() {
        return this;
    }

    private QName toQName(Class<?> objectTypeClass) {
        return PrismContext.get().getSchemaRegistry().determineTypeForClass(objectTypeClass);
    }

}
