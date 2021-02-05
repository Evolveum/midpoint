/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.provisioning.impl.sync.SkipProcessingException;
import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Represents state of processing of given internal provisioning object: a change or a fetched object - at
 * both "resource objects" or "shadow cache" levels.
 */
@Experimental
public class ProcessingState {

    /**
     * True if no further processing (in provisioning module) should be done with this item.
     *
     * The most frequent reason is that the item could not be initialized (a.k.a. pre-processed) successfully.
     * The second option (applicable to changes only) is that the change was fetched but later was observed to be
     * not relevant.
     *
     * In all these cases we want to provide the item to the requesting task (like import, reconciliation, live sync,
     * async update), in order to be correctly acted upon w.r.t. statistics, error handing, and so on.
     */
    private boolean skipFurtherProcessing;

    /**
     * Was an exception encountered during the initialization?
     *
     * If not null, {@link #skipFurtherProcessing} must be true. (If null, it may be true or false.)
     */
    private Throwable exceptionEncountered;

    /**
     * Was this item successfully initialized?
     *
     * If not, it is not safe to use it for regular processing. Only to pass it up the call stack.
     */
    private boolean initialized;

    public ProcessingState() {
    }

    public ProcessingState(ProcessingState processingState) {
        this.skipFurtherProcessing = processingState.skipFurtherProcessing;
        this.exceptionEncountered = processingState.exceptionEncountered;
        this.initialized = false;
    }

    public static ProcessingState success() {
        return new ProcessingState();
    }

    public static ProcessingState error(Throwable exception) {
        ProcessingState state = new ProcessingState();
        state.skipFurtherProcessing = true;
        state.exceptionEncountered = exception;
        return state;
    }

    public static ProcessingState fromUcfErrorState(UcfErrorState errorState) {
        if (errorState.isError()) {
            return ProcessingState.error(errorState.getException());
        } else {
            return ProcessingState.success();
        }
    }

    public void recordException(Throwable t) {
        skipFurtherProcessing = true;
        if (t instanceof SkipProcessingException) {
            if (t.getCause() != null) {
                exceptionEncountered = t.getCause();
            } else {
                // "Success" skipping. The item is simply not applicable. For example, the object has been deleted
                // in the meanwhile. We probably want to increase counters, but we do not want to process this as an error.
                // BEWARE: Be sure not to clear the exception. It can be set from earlier processing!
            }
        } else {
            exceptionEncountered = t;
        }
    }

    public boolean isSkipFurtherProcessing() {
        return skipFurtherProcessing;
    }

    @Override
    public String toString() {
        return "ProcessingState{" +
                "skipFurtherProcessing=" + skipFurtherProcessing +
                ", e=" + exceptionEncountered +
                ", initialized=" + initialized +
                '}';
    }

    public void checkSkipProcessing() throws SkipProcessingException {
        if (skipFurtherProcessing) {
            throw new SkipProcessingException();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized() {
        assert !skipFurtherProcessing;
        assert exceptionEncountered == null;
        this.initialized = true;
    }

    public Throwable getExceptionEncountered() {
        return exceptionEncountered;
    }
}
