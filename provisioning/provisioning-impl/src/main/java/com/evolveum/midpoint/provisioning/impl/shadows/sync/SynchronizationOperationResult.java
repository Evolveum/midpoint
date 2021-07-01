/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *  EXPERIMENTAL
 */
@SuppressWarnings({ "unused" })
public class SynchronizationOperationResult {
    private final AtomicInteger changesProcessed = new AtomicInteger(0);
    private final AtomicInteger errors = new AtomicInteger(0);
    private volatile boolean suspendEncountered;
    private volatile boolean haltingErrorEncountered;
    private Throwable exceptionEncountered;             // FIXME this is a workaround for thresholds
    private volatile boolean taskSuspensionRequested;
    private boolean allChangesFetched;
    private LiveSyncToken initialToken;
    private LiveSyncToken tokenUpdatedTo;

    public int getChangesProcessed() {
        return changesProcessed.get();
    }

    public int getErrors() {
        return errors.get();
    }

    public boolean isSuspendEncountered() {
        return suspendEncountered;
    }

    public void setSuspendEncountered(boolean suspendEncountered) {
        this.suspendEncountered = suspendEncountered;
    }

    public boolean isHaltingErrorEncountered() {
        return haltingErrorEncountered;
    }

    public void setHaltingErrorEncountered(boolean haltingErrorEncountered) {
        this.haltingErrorEncountered = haltingErrorEncountered;
    }

    public Throwable getExceptionEncountered() {
        return exceptionEncountered;
    }

    public void setExceptionEncountered(Throwable exceptionEncountered) {
        this.exceptionEncountered = exceptionEncountered;
    }

    public boolean isTaskSuspensionRequested() {
        return taskSuspensionRequested;
    }

    public void setTaskSuspensionRequested(boolean taskSuspensionRequested) {
        this.taskSuspensionRequested = taskSuspensionRequested;
    }

    public boolean isAllChangesFetched() {
        return allChangesFetched;
    }

    public void setAllChangesFetched(boolean allChangesFetched) {
        this.allChangesFetched = allChangesFetched;
    }

    public LiveSyncToken getInitialToken() {
        return initialToken;
    }

    public void setInitialToken(LiveSyncToken initialToken) {
        this.initialToken = initialToken;
    }

    public LiveSyncToken getTokenUpdatedTo() {
        return tokenUpdatedTo;
    }

    public void setTokenUpdatedTo(LiveSyncToken tokenUpdatedTo) {
        this.tokenUpdatedTo = tokenUpdatedTo;
    }

    @Override
    public String toString() {
        return "changesProcessed=" + changesProcessed +
                ", errors=" + errors +
                ", suspendEncountered=" + suspendEncountered +
                ", haltingErrorEncountered=" + haltingErrorEncountered +
                ", exceptionEncountered=" + exceptionEncountered +
                ", taskSuspensionRequested=" + taskSuspensionRequested +
                ", allChangesFetched=" + allChangesFetched +
                ", initialToken=" + initialToken +
                ", taskTokenUpdatedTo=" + tokenUpdatedTo;
    }

    @SuppressWarnings("UnusedReturnValue")
    public int incrementErrors() {
        return errors.incrementAndGet();
    }

    @SuppressWarnings("UnusedReturnValue")
    public int incrementChangesProcessed() {
        return changesProcessed.incrementAndGet();
    }
}
