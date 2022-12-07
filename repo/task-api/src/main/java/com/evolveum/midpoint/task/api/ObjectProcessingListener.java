/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;

import org.jetbrains.annotations.NotNull;

/**
 * Listens for two aspects of objects being processed during the task execution (on foreground or on background):
 *
 * - object changes (additions, modifications, deletions)
 * - object being processed but not changed
 *
 * See the specific methods.
 */
public interface ObjectProcessingListener {

    //List<> getMetricDefinitions();

    //getStorageStrategy();

    // addProcessedObject(PrismObject<?> before, PrismObject<?> after);

    /**
     * Called after a change was executed (or would-be executed if the execution mode was "real").
     *
     * TODO do we need to distinguish between repository-level deltas and above-repo-level ones (provisioning, task manager, ...)?
     *
     * @param delta The change in question
     * @param executed `true` if the change was really executed, `false` if it was not (i.e. it is only a simulation delta)
     */
    void onChangeExecuted(@NotNull ObjectDelta<?> delta, boolean executed, @NotNull OperationResult result);
}
