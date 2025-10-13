/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.impl.mock;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationResult;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MarkType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;

import org.jetbrains.annotations.NotNull;

public class SimulationResultMock implements SimulationResult {

    private final SimulationTransactionMock transaction;

    SimulationResultMock(SimulationTransactionMock simulationTransactionMock) {
        transaction = simulationTransactionMock;
    }

    @Override
    public @NotNull String getResultOid() {
        return "057eee97-3486-4dac-9e56-358d79bb8a73";
    }

    @Override
    public @NotNull SimulationDefinitionType getSimulationDefinition() {
        return new SimulationDefinitionType();
    }

    @Override
    public boolean isEventMarkEnabled(@NotNull MarkType mark) {
        return true;
    }

    @Override
    public SimulationTransaction getTransaction(String transactionId) {
        return transaction;
    }

    @Override
    public void close(OperationResult result) throws ObjectNotFoundException {
    }
}
