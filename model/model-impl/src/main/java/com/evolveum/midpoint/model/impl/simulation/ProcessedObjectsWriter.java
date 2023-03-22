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

import com.evolveum.midpoint.prism.delta.ObjectDelta;

import com.evolveum.midpoint.provisioning.api.ShadowSimulationData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;
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
        if (data instanceof FullOperationSimulationDataImpl) {
            writeFullData(((FullOperationSimulationDataImpl) data), result);
        } else if (data instanceof SingleDeltaSimulationDataImpl<?>) {
            writeSingleDelta(((SingleDeltaSimulationDataImpl<?>) data), result);
        } else if (data instanceof ShadowSimulationData) {
            writeShadowSimulationData(((ShadowSimulationData) data), result);
        } else {
            LOGGER.warn("Simulation data of unexpected type: {}", MiscUtil.getValueWithClass(data));
        }
    }

    private void writeFullData(FullOperationSimulationDataImpl fullData, OperationResult result) {
        LensContext<?> lensContext = fullData.getLensContext();
        try {
            LOGGER.trace("Storing {} into {}", lensContext, simulationTransaction);

            ClosedResultsChecker.INSTANCE.checkNotClosed(
                    simulationTransaction.getResultOid());

            OperationResult resultToRecord = fullData.getResultToRecord();
            LOGGER.trace("Result to record: {}", resultToRecord);

            boolean resultRecordedToProjection = false;

            LensFocusContext<?> focusContext = lensContext.getFocusContext();
            ProcessedObjectImpl<?> focusRecord = createProcessedObject(focusContext, result);

            List<ProcessedObjectImpl<ShadowType>> projectionRecords = new ArrayList<>();
            for (LensProjectionContext projectionContext : lensContext.getProjectionContexts()) {
                ProcessedObjectImpl<ShadowType> projectionRecord = createProcessedObject(projectionContext, result);
                if (projectionRecord != null) {
                    if (projectionContext.isSynchronizationSource()) {
                        LOGGER.trace("Result recorded to {}", projectionContext);
                        projectionRecord.setResultAndStatus(resultToRecord);
                        resultRecordedToProjection = true;
                    }
                    projectionRecords.add(projectionRecord);
                } else {
                    LOGGER.trace("No processed object for {}", projectionContext);
                }
            }

            if (!resultRecordedToProjection) {
                if (focusRecord != null) {
                    LOGGER.trace("Result recorded to focus");
                    focusRecord.setResultAndStatus(resultToRecord);
                } else if (!result.isError()) {
                    LOGGER.trace("Result not recorded, but it is not an error -> ignoring");
                } else {
                    // What should we do? Let us attach the result to any projection available.
                    if (!projectionRecords.isEmpty()) {
                        ProcessedObjectImpl<ShadowType> any = projectionRecords.get(0);
                        LOGGER.warn("Couldn't find the element context where an error should be recorded, using any: {}", any);
                        any.setResultAndStatus(resultToRecord);
                    } else {
                        LOGGER.warn("Error during simulated processing couldn't be recorded: {}", result.getMessage());
                    }
                }
            }

            // Setting links between focus and its projections
            if (focusRecord != null) {
                focusRecord.setProjectionRecords(projectionRecords.size());
                storeProcessedObjects(List.of(focusRecord), task, result);
            }

            for (ProcessedObjectImpl<ShadowType> projectionRecord : projectionRecords) {
                projectionRecord.setFocusRecordId(focusRecord != null ? focusRecord.getRecordId() : null);
            }

            storeProcessedObjects(projectionRecords, task, result);

        } catch (CommonException e) {
            // TODO which exception to treat?
            throw SystemException.unexpected(e, "when storing processed object information");
        }
    }

    private <O extends ObjectType> ProcessedObjectImpl<O> createProcessedObject(
            @Nullable LensElementContext<O> elementContext, OperationResult result)
            throws CommonException {
        if (elementContext != null) {
            return ProcessedObjectImpl.create(elementContext, simulationTransaction, task, result);
        } else {
            return null;
        }
    }

    private <E extends ObjectType> void writeSingleDelta(SingleDeltaSimulationDataImpl<E> data, OperationResult result) {
        LensElementContext<E> elementContext = data.getElementContext();
        ObjectDelta<E> simulationDelta = data.getSimulationDelta();
        try {
            LOGGER.trace("Storing delta in {} into {}", elementContext, simulationTransaction);

            ProcessedObjectImpl<E> processedObject =
                    ProcessedObjectImpl.createSingleDelta(elementContext, simulationDelta, simulationTransaction, task, result);
            storeProcessedObjects(List.of(processedObject), task, result);

        } catch (CommonException e) {
            // TODO which exception to treat?
            throw SystemException.unexpected(e, "when storing processed object information");
        }
    }

    private void writeShadowSimulationData(ShadowSimulationData data, OperationResult result) {
        try {
            LOGGER.trace("Storing delta in {} into {}", data, simulationTransaction);

            ProcessedObjectImpl<ShadowType> processedObject =
                    ProcessedObjectImpl.createForShadow(data, simulationTransaction);
            storeProcessedObjects(List.of(processedObject), task, result);

        } catch (CommonException e) {
            // TODO which exception to treat?
            throw SystemException.unexpected(e, "when storing processed object information");
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
        Collection<SimulationResultProcessedObjectType> processedObjectsBeans = ProcessedObjectImpl.toBeans(processedObjects);
        List<ItemDelta<?, ?>> modifications = PrismContext.get().deltaFor(SimulationResultType.class)
                .item(SimulationResultType.F_PROCESSED_OBJECT)
                .addRealValues(processedObjectsBeans)
                .asItemDeltas();
        ModelBeans.get().cacheRepositoryService.modifyObject(
                SimulationResultType.class,
                simulationTransaction.getResultOid(),
                modifications,
                result);
        // Repository seems to generate PCV IDs in processedObjectsBeans. We propagate them back to ProcessedObject instances.
        //
        // TODO This is fragile. Actually, the repository is NOT obliged to do so, and does it more-or-less by accident.
        //  (For example, it narrows incoming deltas, sometimes cloning them, so the PCV IDs may get lost in the process.)
        //  But for this particular case it seems to work.
        for (ProcessedObjectImpl<?> processedObject : processedObjects) {
            processedObject.propagateRecordId();
        }
    }

    private static OpenResultTransactionsHolder getOpenResultTransactionsHolder() {
        return ModelBeans.get().simulationResultManager.getOpenResultTransactionsHolder();
    }
}
