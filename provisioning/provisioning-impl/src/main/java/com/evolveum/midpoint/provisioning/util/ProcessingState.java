/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.util;

import com.evolveum.midpoint.provisioning.impl.sync.SkipProcessingException;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * TODO
 */
@Experimental
public class ProcessingState {

    /**
     * True if no further processing (in provisioning module) should be done with this item.
     *
     * It is probably malformed or not applicable. But we need to carry this change through up to
     * the task that will record its presence and process it (usually by recording an exception).
     */
    private boolean skipFurtherProcessing;

    private Throwable exceptionEncountered;

    private boolean preprocessed;

    public ProcessingState() {
    }

    public ProcessingState(ProcessingState processingState) {
        this.skipFurtherProcessing = processingState.skipFurtherProcessing;
        this.exceptionEncountered = processingState.exceptionEncountered;
    }

    public void setSkipFurtherProcessing(Throwable t) {
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
                ", preprocessed=" + preprocessed +
                '}';
    }

    public void checkSkipProcessing() throws SkipProcessingException {
        if (skipFurtherProcessing) {
            throw new SkipProcessingException();
        }
    }

    public boolean isPreprocessed() {
        return preprocessed;
    }

    public void setPreprocessed() {
        this.preprocessed = true;
    }
}
