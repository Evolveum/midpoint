/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for "process change" request and its execution.
 *
 * These requests are ordered according to local sequence number in embedded Change.
 * See {@link RequestsBuffer}.
 */
public class ProcessChangeRequest implements Comparable<ProcessChangeRequest> {

    @NotNull private final Change change;
    private final ProvisioningContext globalContext;
    private final boolean simulate;
    private boolean successfullyProcessed;

    /**
     * True if the request was processed (successfully or not).
     */
    private volatile boolean done;

    public ProcessChangeRequest(@NotNull Change change, ProvisioningContext globalContext, boolean simulate) {
        Validate.notNull(change, "change");
        this.change = change;
        this.globalContext = globalContext;
        this.simulate = simulate;
    }

    @NotNull
    public Change getChange() {
        return change;
    }

    ProvisioningContext getGlobalContext() {
        return globalContext;
    }

    public boolean isSimulate() {
        return simulate;
    }

    public boolean isSuccessfullyProcessed() {
        return successfullyProcessed;
    }

    public void setSuccessfullyProcessed(boolean successfullyProcessed) {
        this.successfullyProcessed = successfullyProcessed;
    }

    /**
     * Called when the request was successfully processed.
     * (Or there was nothing to be processed at all.)
     */
    public void onSuccessfullyProcessed() {
    }

    /**
     * Called when there was a processing error, represented by the operation result.
     */
    public void onProcessingError(OperationResult result) {
        // Probably nothing to do here. The error is already recorded in operation result.
    }

    /**
     * Called when there was a processing error, represented by an exception (that was stored into
     * operation result as a fatal error).
     */
    public void onProcessingError(Throwable t, OperationResult result) {
        // Probably nothing to do here. The error is already recorded in operation result.
    }

    Object getPrimaryIdentifierRealValue() {
        return change.getPrimaryIdentifierRealValue();
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = true;
    }

    /**
     * Called on completion; after onSuccess/onError is called.
     *
     * @param workerTask Task in which this request was executed.
     * @param coordinatorTask Coordinator task. Might be null.
     * @param result Operation result specific to the execution of this request
     */
    public void onCompletion(@NotNull Task workerTask, @Nullable Task coordinatorTask, @NotNull OperationResult result) {
    }

    @Override
    public String toString() {
        return "ProcessChangeRequest{" +
                "change=" + change +
                ", done=" + done +
                ", success=" + successfullyProcessed +
                '}';
    }

    @Override
    public int compareTo(@NotNull ProcessChangeRequest o) {
        return Integer.compare(change.getLocalSequenceNumber(), o.getChange().getLocalSequenceNumber());
    }
}
