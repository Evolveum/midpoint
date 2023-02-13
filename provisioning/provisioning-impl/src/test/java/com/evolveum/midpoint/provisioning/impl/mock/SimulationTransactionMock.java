/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.mock;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationData;
import com.evolveum.midpoint.task.api.SimulationResult;
import com.evolveum.midpoint.task.api.SimulationTransaction;
import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SimulationTransactionMock implements SimulationTransaction {

    private final List<SimulationData> simulationDataList = new ArrayList<>();

    @Override
    public void writeSimulationData(@NotNull SimulationData data, @NotNull Task task, @NotNull OperationResult result) {
        System.out.println("Simulation data: " + data);
        simulationDataList.add(data);
    }

    @Override
    public @NotNull SimulationResult getSimulationResult() {
        return new SimulationResultMock(this);
    }

    @Override
    public @NotNull String getTransactionId() {
        return "1";
    }

    @Override
    public void open(OperationResult result) {
    }

    @Override
    public void commit(OperationResult result) {
    }

    public @NotNull List<SimulationData> getSimulationDataList() {
        return simulationDataList;
    }

    public void clear() {
        simulationDataList.clear();
    }
}
