/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.simulation;

import java.util.List;

import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

public interface SimulationResultManager {

    /**
     * Creates a new simulation result in repository.
     *
     * @param definition Definition to use. If null, the default one is used.
     *
     * @see #defaultDefinition()
     */
    @NotNull SimulationResultContext newSimulationResult(
            @Nullable SimulationDefinitionType definition, @NotNull OperationResult result) throws ConfigurationException;

    /** TODO better name */
    SimulationResultContext newSimulationContext(@NotNull String resultOid);

    /**
     * Returns the default simulation definition: either from the system configuration (if present there), or a new one.
     */
    SimulationDefinitionType defaultDefinition() throws ConfigurationException;

    /**
     * Fetches and parses all stored processed objects from given {@link SimulationResultType}.
     */
    @NotNull List<ProcessedObject<?>> getStoredProcessedObjects(@NotNull String oid, OperationResult result)
            throws SchemaException;

    /** Closes the simulation result, e.g. computes the metrics. No "processed object" records should be added afterwards. */
    void closeSimulationResult(@NotNull ObjectReferenceType simulationResultRef, OperationResult result)
            throws ObjectNotFoundException;
}
