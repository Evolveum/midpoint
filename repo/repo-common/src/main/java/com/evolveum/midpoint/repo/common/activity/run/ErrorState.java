/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Describes the "error state" of the current activity run.
 *
 * Very experimental. TODO rethink this
 */
@Experimental
public class ErrorState {

    /**
     * TODO
     */
    @NotNull private final AtomicReference<Throwable> stoppingExceptionRef = new AtomicReference<>();

    public Throwable getStoppingException() {
        return stoppingExceptionRef.get();
    }

    public void setStoppingException(@NotNull Throwable reason) {
        stoppingExceptionRef.set(reason);
    }

    boolean wasStoppingExceptionEncountered() {
        return getStoppingException() != null;
    }
}
