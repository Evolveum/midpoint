/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

/**
 * Listens for changes that are executed, or are to be executed.
 *
 * TEMPORARY - to be used only during the development of the "simulations" feature in 4.7.
 *
 * We may use other means (e.g. parallel to audit) to record the deltas-to-be-executed.
 */
@Experimental
public interface ChangeExecutionListener {

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
