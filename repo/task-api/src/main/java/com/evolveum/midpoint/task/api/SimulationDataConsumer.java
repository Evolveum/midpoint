/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
