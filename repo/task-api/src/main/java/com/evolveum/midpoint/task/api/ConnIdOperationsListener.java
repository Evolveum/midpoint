/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.reporting.ConnIdOperation;

import org.jetbrains.annotations.NotNull;

/**
 * Obtains information about ConnId operations executed.
 */
public interface ConnIdOperationsListener {

    /**
     * Called when an operation starts.
     */
    default void onConnIdOperationStart(@NotNull ConnIdOperation operation) {
    }

    /**
     * Called when an operation ends (successfully or not).
     */
    default void onConnIdOperationEnd(@NotNull ConnIdOperation operation) {
    }

    /**
     * Called when an operation is suspended, i.e. the control goes to the client code.
     *
     * For example, when a object is being passed to the handler during iterative search.
     */
    default void onConnIdOperationSuspend(@NotNull ConnIdOperation operation) {
    }

    /**
     * Called when an operation is resumed, i.e. the control goes back to the operation.
     *
     * For example, when a object handling is finished during iterative search and the search continues.
     */
    default void onConnIdOperationResume(@NotNull ConnIdOperation operation) {
    }
}
