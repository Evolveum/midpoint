/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * Accepts an information about an object processed in the simulation mode.
 */
public interface SimulationProcessedObjectListener {

    /**
     * Called after an object was processed by the simulation.
     */
    void onObjectProcessedBySimulation(
            @NotNull SimulationProcessedObject processedObject,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException;
}
