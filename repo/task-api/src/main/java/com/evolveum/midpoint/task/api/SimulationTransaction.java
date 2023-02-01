/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

/**
 * Live representation of a simulation transaction.
 *
 * Thread safety: Instances are to be used in multiple threads (worker tasks), so they must be thread-safe.
 */
public interface SimulationTransaction {

    /**
     * Adds a chunk of data to this transaction. We intentionally do not define what that "chunk" is,
     * because we don't have appropriate vocabulary here - to talk about LensContext and so on.
     */
    void writeSimulationData(@NotNull SimulationData data, @NotNull Task task, @NotNull OperationResult result);

    @NotNull SimulationResult getSimulationResult();

    default @NotNull String getResultOid() {
        return getSimulationResult().getResultOid();
    }

    @NotNull String getTransactionId();

    /** TODO */
    void open(OperationResult result);

    /** TODO */
    void commit(OperationResult result);
}
