/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import java.util.List;

import com.evolveum.midpoint.model.api.ModelInteractionService;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

public interface SimulationResultManager {

    /**
     * Creates a new simulation result in repository.
     *
     * @param definition Definition to use. If null, the default one is used.
     * @see #defaultDefinition()
     */
    @NotNull SimulationResultContext openNewSimulationResult(
            @Nullable SimulationDefinitionType definition,
            @Nullable String rootTaskOid,
            @Nullable ConfigurationSpecificationType configurationSpecification,
            @NotNull OperationResult result)
            throws ConfigurationException;

    /** Closes the simulation result, e.g. computes the metrics. No "processed object" records should be added afterwards. */
    void closeSimulationResult(@NotNull String simulationResultOid, Task task, OperationResult result)
            throws ObjectNotFoundException;

    /** TODO */
    void openSimulationResultTransaction(
            @NotNull String simulationResultOid, @NotNull String transactionId, OperationResult result);

    /** TODO */
    void commitSimulationResultTransaction(
            @NotNull String simulationResultOid, @NotNull String transactionId, OperationResult result);

    /** TODO better name */
    SimulationResultContext newSimulationContext(@NotNull String resultOid);

    /**
     * Returns the default simulation definition: either from the system configuration (if present there), or a new one.
     */
    SimulationDefinitionType defaultDefinition() throws ConfigurationException;

    /**
     * Fetches and parses all stored processed objects from given {@link SimulationResultType}.
     */
    @VisibleForTesting
    @NotNull List<? extends ProcessedObject<?>> getStoredProcessedObjects(@NotNull String oid, OperationResult result)
            throws SchemaException;

    /**
     * See {@link ModelInteractionService#executeInSimulationMode(TaskExecutionMode, SimulationDefinitionType, Task,
     * OperationResult, SimulatedFunctionCall)}.
     */
    <X> X executeInSimulationMode(
            @NotNull TaskExecutionMode mode,
            @Nullable SimulationDefinitionType simulationDefinition,
            @NotNull Task task,
            @NotNull OperationResult result,
            @NotNull SimulatedFunctionCall<X> functionCall)
                    throws CommonException;

    /** Returns the definition of a metric or `null` if there's none. */
    @Nullable SimulationMetricDefinitionType getMetricDefinition(@NotNull String identifier);

    interface SimulatedFunctionCall<X> {
        X execute() throws CommonException;
    }
}
