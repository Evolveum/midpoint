/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

/**
 * Live representation of a simulation result.
 *
 * Thread safety: Instances are to be used in multiple threads (worker tasks), so they must be thread-safe.
 */
public interface SimulationResult {

    /** OID of the {@link SimulationResultType} object. */
    @NotNull String getResultOid();

    /** Returns the definition for the current simulation. */
    @NotNull SimulationDefinitionType getSimulationDefinition();

    /** Is this particular event mark enabled for the current simulation? */
    boolean isEventMarkEnabled(@NotNull MarkType mark);

    /** TODO */
    SimulationTransaction getTransaction(String transactionId);

    default SimulationTransaction openTransaction(String transactionId, OperationResult result) {
        SimulationTransaction transaction = getTransaction(transactionId);
        transaction.open(result);
        return transaction;
    }

    /** Closes the simulation result. No "processed object" records should be added afterwards. */
    void close(OperationResult result) throws ObjectNotFoundException;
}
