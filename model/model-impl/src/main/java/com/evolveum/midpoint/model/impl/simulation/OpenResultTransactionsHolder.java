/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricValuesType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository of open simulation result transactions.
 *
 * Thread safety is critical here. Multiple threads can update this information concurrently.
 *
 * TODO Open transactions are removed if the realization of activities finish correctly. Perhaps we should implement
 *  some housekeeping/clean-up procedures for the unusual cases, like suspended and then abandoned tasks, etc.
 */
@Component
public class OpenResultTransactionsHolder {

    @NotNull private final Map<GlobalTxKey, AggregatedMetricsComputation> transactions = new ConcurrentHashMap<>();

    void addProcessedObject(
            @NotNull ProcessedObjectImpl<?> processedObject,
            @NotNull SimulationTransaction tx,
            @NotNull Task task,
            @NotNull OperationResult result) throws CommonException {
        transactions.computeIfAbsent(
                        new GlobalTxKey(tx),
                        (k) -> AggregatedMetricsComputation.create(result))
                .addProcessedObject(processedObject, task, result);
    }

    void removeTransaction(@NotNull SimulationTransaction tx) {
        transactions.remove(new GlobalTxKey(tx));
    }

    void removeWholeResult(@NotNull String resultOid) {
        transactions.keySet().removeIf(
                key -> resultOid.equals(key.resultOid));
    }

    List<SimulationMetricValuesType> getMetricsValues(SimulationTransaction tx) {
        AggregatedMetricsComputation computation = transactions.get(new GlobalTxKey(tx));
        return computation != null ? computation.toBeans() : List.of();
    }

    private static class GlobalTxKey {

        @NotNull private final String resultOid;
        @NotNull private final String transactionId;

        private GlobalTxKey(@NotNull SimulationTransaction tx) {
            this.resultOid = tx.getResultOid();
            this.transactionId = tx.getTransactionId();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GlobalTxKey that = (GlobalTxKey) o;
            return transactionId.equals(that.transactionId)
                    && resultOid.equals(that.resultOid);
        }

        @Override
        public int hashCode() {
            return Objects.hash(resultOid, transactionId);
        }

        @Override
        public String toString() {
            return "GlobalTxKey{" +
                    "resultOid='" + resultOid + '\'' +
                    ", transactionId=" + transactionId +
                    '}';
        }
    }
}
