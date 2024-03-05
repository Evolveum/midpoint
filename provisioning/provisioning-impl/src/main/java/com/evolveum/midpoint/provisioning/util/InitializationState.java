/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static com.evolveum.midpoint.provisioning.util.InitializationState.LifecycleState.*;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Represents state of initialization of given "initializable" internal provisioning object.
 *
 * Such objects are currently changes and resource objects found by a search. They exist on both
 * "resource objects" or "shadows" levels.
 */
@Experimental
public class InitializationState implements Serializable {

    /**
     * State of this initializable object (regarding the initialization itself).
     */
    @NotNull private LifecycleState lifecycleState;

    /**
     * The current error state. It determines further fate of this initializable object. For example, objects marked
     * as {@link ErrorState.Error} are only minimally processed. Objects marked as {@link ErrorState.NotApplicable}
     * are ignored even more.
     */
    @NotNull private ErrorState errorState;

    private InitializationState(@NotNull LifecycleState lifecycleState, @NotNull ErrorState errorState) {
        this.lifecycleState = lifecycleState;
        this.errorState = errorState;
    }

    public static @NotNull InitializationState created() {
        return new InitializationState(CREATED, ErrorState.ok());
    }

    public static InitializationState initialized(@NotNull ErrorState errorState) {
        return new InitializationState(INITIALIZED, errorState);
    }

    public void recordInitializationFailed(@NotNull Throwable t) {
        lifecycleState = INITIALIZED; // The previous state can be initializing or initialization complete.
        recordError(t);
    }

    public void recordError(@NotNull Throwable t) {
        recordError(
                ErrorState.error(t));
    }

    public void recordError(@NotNull ErrorState errorState) {
        this.errorState = errorState;
    }

    public void moveFromCreatedToInitializing() {
        stateCheck(lifecycleState == CREATED, "Unexpected lifecycle state: %s", lifecycleState);
        lifecycleState = INITIALIZING;
    }

    public void moveFromInitializingToInitialized() {
        stateCheck(lifecycleState == INITIALIZING, "Unexpected lifecycle state: %s", lifecycleState);
        lifecycleState = INITIALIZED;
    }

    /**
     * "Success" skipping. The item is simply not applicable. For example, the object has been deleted
     * in the meanwhile. We probably want to increase counters, but we do not want to process this as an error.
     *
     * What to do with previous exception (if there is any)? If we keep it, we must keep error state to be ERROR.
     * If we want to change error state to NOT_APPLICABLE, we must remove the exception. So? Currently we keep any previous
     * error.
     *
     * We intentionally do not mess with lifecycle status here. It is done separately.
     */
    public void recordNotApplicable() {
        if (!errorState.isError()) {
            errorState = ErrorState.notApplicable();
        } else {
            // Keeping the error state (and exception) as is.
        }
    }

    @Override
    public String toString() {
        return lifecycleState + ": " + errorState;
    }

    public @NotNull ErrorState getErrorState() {
        return errorState;
    }

    public Throwable getExceptionEncountered() {
        return errorState.getException();
    }

    // Beware, the error state can be anything!
    public boolean isInitialized() {
        return lifecycleState == INITIALIZED;
    }

    public boolean isOk() {
        return errorState.isOk();
    }

    public boolean isError() {
        return errorState.isError();
    }

    public boolean isNotApplicable() {
        return errorState.isNotApplicable();
    }

    public void checkInitialized() {
        stateCheck(isInitialized(), "Lifecycle state is not INITIALIZED: %s", lifecycleState);
    }

    public enum LifecycleState {
        /**
         * Object was crated but not yet initialized.
         */
        CREATED,

        /**
         * Initialization is in progress.
         */
        INITIALIZING,

        /**
         * Initialization completed (successfully or not).
         * This means that errored and not applicable objects also fall into this category.
         */
        INITIALIZED,
    }
}
