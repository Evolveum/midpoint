/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Describes the "error state" of the current task part execution.
 *
 * Very experimental. TODO rethink this
 */
@Experimental
public class ErrorState {

    /**
     * TODO
     */
    @NotNull private final AtomicReference<Throwable> permanentErrorEncounteredRef = new AtomicReference<>();

    public Throwable getPermanentErrorException() {
        return permanentErrorEncounteredRef.get();
    }

    public void setPermanentErrorException(@NotNull Throwable reason) {
        permanentErrorEncounteredRef.set(reason);
    }

    public boolean isPermanentErrorEncountered() {
        return getPermanentErrorException() != null;
    }
}
