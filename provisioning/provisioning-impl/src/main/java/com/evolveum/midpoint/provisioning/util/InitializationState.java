/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

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
     * The error state when the initializable object was created (meaning created by its Java constructor).
     */
    @NotNull private final ErrorState initialErrorState;

    /**
     * The current error state. It determines further fate of this initializable object. For example, objects marked
     * as {@link ErrorState#ERROR} are only minimally processed. Objects marked as {@link ErrorState#NOT_APPLICABLE} are ignored even more.
     */
    @NotNull private ErrorState currentErrorState;

    /**
     * Was an exception encountered during earlier processing OR the initialization of this object?
     *
     * Is not null if and only if {@link #currentErrorState} is {@link ErrorState#ERROR}.
     */
    private Throwable exceptionEncountered;

    /**
     * State in the life cycle.
     */
    @NotNull private LifecycleState lifecycleState;

    private InitializationState(@NotNull ErrorState initialErrorState, Throwable initialException) {
        this.initialErrorState = initialErrorState;
        this.currentErrorState = initialErrorState;
        this.exceptionEncountered = initialException;
        this.lifecycleState = LifecycleState.CREATED;
        checkErrorStateCompatibleWithException();
    }

    private void checkErrorStateCompatibleWithException() {
        if (currentErrorState == ErrorState.ERROR) {
            stateCheck(exceptionEncountered != null, "Error state without an exception");
        } else {
            stateCheck(exceptionEncountered == null, "Exception without error state");
        }
    }

    public static InitializationState fromSuccess() {
        return new InitializationState(ErrorState.OK, null);
    }

    public static InitializationState fromUcfErrorState(UcfErrorState errorState, Throwable preInitException) {
        if (preInitException != null) {
            return new InitializationState(ErrorState.ERROR, preInitException);
        } else if (errorState.isError()) {
            return new InitializationState(ErrorState.ERROR, errorState.getException());
        } else {
            return InitializationState.fromSuccess();
        }
    }

    public static InitializationState fromPreviousState(InitializationState previousState) {
        return new InitializationState(previousState.currentErrorState, previousState.exceptionEncountered);
    }

    public void recordInitializationFailed(@NotNull Throwable t) {
        lifecycleState = LifecycleState.INITIALIZATION_FAILED; // The previous state can be initializing or initialized
        currentErrorState = ErrorState.ERROR;
        exceptionEncountered = t;
    }

    public void moveFromCreatedToInitializing() {
        stateCheck(lifecycleState == LifecycleState.CREATED, "Unexpected lifecycle state: %s", lifecycleState);
        lifecycleState = LifecycleState.INITIALIZING;
    }

    public void moveFromInitializingToInitialized() {
        stateCheck(lifecycleState == LifecycleState.INITIALIZING, "Unexpected lifecycle state: %s", lifecycleState);
        lifecycleState = LifecycleState.INITIALIZED;
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
        if (currentErrorState != ErrorState.ERROR) {
            currentErrorState = ErrorState.NOT_APPLICABLE;
        } else {
            // Keeping the error state (and exception) as is.
        }
    }

    @Override
    public String toString() {
        return lifecycleState + ": " + initialErrorState + " -> " + currentErrorState +
                (exceptionEncountered != null ? " (e=" + MiscUtil.getClassWithMessage(exceptionEncountered) + ")" : "");
    }

    public Throwable getExceptionEncountered() {
        return exceptionEncountered;
    }

    public boolean isAfterInitialization() {
        return lifecycleState == LifecycleState.INITIALIZED || lifecycleState == LifecycleState.INITIALIZATION_FAILED;
    }

    // Beware, the error state can be anything!
    public boolean isInitialized() {
        return lifecycleState == LifecycleState.INITIALIZED;
    }

    // Useful during initialization.
    public boolean isInitialStateOk() {
        return initialErrorState == ErrorState.OK;
    }

    public boolean isNotApplicable() {
        return currentErrorState == ErrorState.NOT_APPLICABLE;
    }

    public boolean isOk() {
        return currentErrorState == ErrorState.OK;
    }

    public boolean isError() {
        checkErrorStateCompatibleWithException();
        return currentErrorState == ErrorState.ERROR;
    }

    public void checkAfterInitialization() {
        stateCheck(lifecycleState == LifecycleState.INITIALIZED || lifecycleState == LifecycleState.INITIALIZATION_FAILED,
                "Lifecycle state is not INITIALIZED/INITIALIZATION_FAILED: %s", lifecycleState);
    }

    public enum ErrorState {
        /**
         * No restrictions for further processing are known (yet). After successful initialization,
         * the object can be used for full processing.
         */
        OK,

        /**
         * The object is in error state. The further processing is limited. Usually,
         * only actions related to statistics keeping and error reporting should be done.
         */
        ERROR,

        /**
         * The object is without errors, but it is nevertheless not applicable for further processing.
         * (From the higher level view it is simply not relevant. From the lower level view it may miss some
         * crucial information - like a repository shadow.)
         *
         * Usually this state means that it should be passed further only for statistics-keeping purposes.
         */
        NOT_APPLICABLE
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
         * Initialization completed successfully.
         * Not applicable objects also fall into this category.
         */
        INITIALIZED,

        /**
         * Initialization ends with a failure. (Should be a separate state?)
         */
        INITIALIZATION_FAILED
    }
}
