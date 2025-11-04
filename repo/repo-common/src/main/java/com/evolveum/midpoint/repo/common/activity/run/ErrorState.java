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
 * TODO rename to something like ImmediateStopState
 */
@Experimental
public class ErrorState {

    /** Holds details about the stop request, namely, what should be the run result of the activity. */
    @NotNull private final AtomicReference<ImmediateStopRequestDetails> stopRequestDetailsRef = new AtomicReference<>();

    ImmediateStopRequestDetails getImmediateStopRequest() {
        return stopRequestDetailsRef.get();
    }

    public void requestImmediateStop(@NotNull Throwable throwable) {
        requestImmediateStop(ActivityRunResult.fromException(throwable));
    }

    public void requestImmediateStop(@NotNull ActivityRunResult runResult) {
        stopRequestDetailsRef.set(
                new ImmediateStopRequestDetails(runResult));
    }

    public boolean wasImmediateStopRequested() {
        return stopRequestDetailsRef.get() != null;
    }

    @SuppressWarnings("WeakerAccess")
    public record ImmediateStopRequestDetails(@NotNull ActivityRunResult runResult) {
    }
}
