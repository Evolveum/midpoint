/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
