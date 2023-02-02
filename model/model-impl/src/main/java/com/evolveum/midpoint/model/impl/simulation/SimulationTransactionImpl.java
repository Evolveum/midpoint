/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.simulation.SimulationMetricComputer;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.SimulationTransaction;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SimulationTransactionImpl implements SimulationTransaction {

    private static final Trace LOGGER = TraceManager.getTrace(SimulationTransactionImpl.class);

    @NotNull private final SimulationResultImpl simulationResult;
    @NotNull private final String transactionId;
    @NotNull private final SimulationResultManagerImpl simulationResultManager;

    SimulationTransactionImpl(
            @NotNull SimulationResultImpl simulationResult,
            @NotNull String transactionId) {

        this.simulationResult = simulationResult;
        this.transactionId = transactionId;
        this.simulationResultManager = ModelBeans.get().simulationResultManager;
    }

    @Override
    public void writeSimulationData(@NotNull SimulationData data, @NotNull Task task, @NotNull OperationResult result) {
        ProcessedObjectsWriter.write(data, this, task, result);
    }

    @Override
    public @NotNull SimulationResultImpl getSimulationResult() {
        return simulationResult;
    }

    @Override
    public @NotNull String getTransactionId() {
        return transactionId;
    }

    @Override
    public void open(OperationResult result) {
        LOGGER.trace("Opening simulation transaction {}", this);

        simulationResultManager.deleteTransactionIfPresent(getResultOid(), transactionId, result);

        getOpenResultTransactionsHolder().removeTransaction(this);
    }

    @Override
    public void commit(OperationResult result) {
        try {
            LOGGER.trace("Committing simulation result transaction {}", this);

            ModelBeans.get().cacheRepositoryService.modifyObjectDynamically(
                    SimulationResultType.class,
                    getResultOid(),
                    null,
                    oldResult ->
                            PrismContext.get().deltaFor(SimulationResultType.class)
                                    .item(SimulationResultType.F_METRIC)
                                    .replaceRealValues(
                                            computeUpdatedMetricsValues(oldResult.getMetric()))
                                    .asItemDeltas(),
                    null,
                    result);
        } catch (ObjectNotFoundException | SchemaException | ObjectAlreadyExistsException e) {
            throw SystemException.unexpected(e, "when committing simulation result transaction");
        }

        getOpenResultTransactionsHolder().removeTransaction(this);
    }


    /**
     * Adds current in-memory metric values for the transaction being committed to the (aggregated) metrics values
     * that will go to the result.
     */
    private List<SimulationMetricValuesType> computeUpdatedMetricsValues(List<SimulationMetricValuesType> old) {
        List<SimulationMetricValuesType> current = getOpenResultTransactionsHolder().getMetricsValues(this);
        List<SimulationMetricValuesType> sum = SimulationMetricComputer.add(old, current);
        // TODO consider removal of the following logging call (too verbose)
        LOGGER.trace("Computed updated metrics for {}:\n OLD:\n{}\n CURRENT:\n{}\n SUM:\n{}",
                this, DebugUtil.debugDumpLazily(old), DebugUtil.debugDumpLazily(current), DebugUtil.debugDumpLazily(sum));
        return sum;
    }

    private OpenResultTransactionsHolder getOpenResultTransactionsHolder() {
        return simulationResultManager.getOpenResultTransactionsHolder();
    }

    @Override
    public String toString() { // TODO
        return "SimulationTransactionImpl{" +
                "simulationResult=" + simulationResult +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}
