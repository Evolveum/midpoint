/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.SimulationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurationSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationMetricDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

public interface SimulationResultManager {

    /**
     * Creates a new simulation result in repository.
     *
     * @param definition Definition to use. If null, the default one is used.
     * @see #defaultDefinition()
     */
    @NotNull SimulationResult createSimulationResult(
            @Nullable SimulationDefinitionType definition,
            @Nullable String rootTaskOid,
            @Nullable ConfigurationSpecificationType configurationSpecification,
            @NotNull OperationResult result)
            throws ConfigurationException;

    /**
     * Provides a {@link SimulationResult} for given simulation result OID. May involve repository get operation.
     *
     * Makes sure that the simulation result is open. Although this does not prevent writing to closed results (as the
     * result may be closed after obtaining the context), it should be good enough to cover e.g. cases when we re-use
     * existing result by mistake.
     */
    SimulationResult getSimulationResult(@NotNull String resultOid, @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException;

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
     * See {@link ModelInteractionService#executeWithSimulationResult(TaskExecutionMode, SimulationDefinitionType, Task,
     * OperationResult, SimulatedFunctionCall)}.
     *
     * When in `functionCall`, the {@link Task#getSimulationTransaction()} returns non-null value for the task.
     */
    <X> X executeWithSimulationResult(
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
