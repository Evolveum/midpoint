/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;

/** Accepts simulation data. */
public interface SimulationDataConsumer {

    /**
     * Called after a chunk of data was created by a simulation. We intentionally do not define what that "chunk" is,
     * because we don't have appropriate vocabulary here - to talk about LensContext and so on.
     */
    void accept(@NotNull SimulationData data, @NotNull Task task, @NotNull OperationResult result);
}
