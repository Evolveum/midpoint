/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.ModelCommonBeans;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

/**
 * Writes simulation data into the repository - in the form of "processed objects".
 */
class ProcessedObjectsWriter {

    private static final Trace LOGGER = TraceManager.getTrace(ProcessedObjectsWriter.class);

    @NotNull private final SimulationData data;
    @NotNull private final SimulationTransactionImpl simulationTransaction;
    @NotNull private final Task task;

    private ProcessedObjectsWriter(
            @NotNull SimulationData data,
            @NotNull SimulationTransactionImpl simulationTransaction,
            @NotNull Task task) {
        this.data = data;
        this.simulationTransaction = simulationTransaction;
        this.task = task;
    }

    static void write(
            @NotNull SimulationData data,
            @NotNull SimulationTransactionImpl simulationTransaction,
            @NotNull Task task,
            @NotNull OperationResult result) {
        new ProcessedObjectsWriter(data, simulationTransaction, task)
                .write(result);
    }

    private void write(@NotNull OperationResult result) {
        if (!(data instanceof SimulationDataImpl)) {
            LOGGER.warn("Simulation data of unexpected type: {}", MiscUtil.getValueWithClass(data));
            return;
        }
        LensContext<?> lensContext = ((SimulationDataImpl) data).getLensContext();
        try {
            LOGGER.trace("Storing {} into {}", lensContext, simulationTransaction);

            ClosedResultsChecker.INSTANCE.checkNotClosed(
                    simulationTransaction.getResultOid());

            LensFocusContext<?> focusContext = lensContext.getFocusContext();
            ProcessedObjectImpl<?> focusRecord = createProcessedObject(focusContext, result);

            List<ProcessedObjectImpl<ShadowType>> projectionRecords = new ArrayList<>();
            for (LensProjectionContext projectionContext : lensContext.getProjectionContexts()) {
                ProcessedObjectImpl<ShadowType> projectionRecord = createProcessedObject(projectionContext, result);
                if (projectionRecord != null) {
                    projectionRecords.add(projectionRecord);
                }
            }

            // Setting links between focus and its projections
            if (focusRecord != null) {
                focusRecord.setRecordId(generateRecordId());
                focusRecord.setProjectionRecords(projectionRecords.size());
                storeProcessedObjects(List.of(focusRecord), task, result);
            }

            for (ProcessedObjectImpl<ShadowType> projectionRecord : projectionRecords) {
                projectionRecord.setRecordId(generateRecordId());
                projectionRecord.setFocusRecordId(focusRecord != null ? focusRecord.getRecordId() : null);
            }
            storeProcessedObjects(projectionRecords, task, result);

        } catch (CommonException e) {
            // TODO which exception to treat?
            throw SystemException.unexpected(e, "when storing processed object information");
        }
    }

    // TODO remove
    /**
     * Generates the ID for the "processed object" record. Should be unique within a simulation result.
     *
     * Later, this may be done right in the repository service.
     */
    private String generateRecordId() {
        return ModelCommonBeans.get().lightweightIdentifierGenerator.generate().toString();
    }

    private <O extends ObjectType> ProcessedObjectImpl<O> createProcessedObject(
            @Nullable LensElementContext<O> elementContext, OperationResult result) {
        if (elementContext == null) {
            return null;
        }
        try {
            return ProcessedObjectImpl.create(elementContext, simulationTransaction, task, result);
        } catch (CommonException e) {
            // TODO do we need more precise error reporting here?
            //  Or should we conceal some of these exceptions? (Probably not.)
            throw new SystemException(
                    "Couldn't process or store the simulation object processing record: " + e.getMessage(), e);
        }
    }

    private void storeProcessedObjects(
            @NotNull Collection<? extends ProcessedObjectImpl<?>> processedObjects,
            @NotNull Task task,
            @NotNull OperationResult result) throws CommonException {
        if (processedObjects.isEmpty()) {
            return;
        }
        for (ProcessedObjectImpl<?> processedObject : processedObjects) {
            LOGGER.trace("Going to store processed object into {}: {}", simulationTransaction, processedObject);
            getOpenResultTransactionsHolder().addProcessedObject(processedObject, simulationTransaction, task, result);
        }
        List<ItemDelta<?, ?>> modifications = PrismContext.get().deltaFor(SimulationResultType.class)
                .item(SimulationResultType.F_PROCESSED_OBJECT)
                .addRealValues(ProcessedObjectImpl.toBeans(processedObjects))
                .asItemDeltas();
        ModelBeans.get().cacheRepositoryService.modifyObject(
                SimulationResultType.class,
                simulationTransaction.getResultOid(),
                modifications,
                result);
    }

    private static OpenResultTransactionsHolder getOpenResultTransactionsHolder() {
        return ModelBeans.get().simulationResultManager.getOpenResultTransactionsHolder();
    }
}
