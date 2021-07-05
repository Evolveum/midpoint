/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.sync;

import com.evolveum.midpoint.provisioning.api.LiveSyncToken;

/**
 *  EXPERIMENTAL
 */
public class SynchronizationOperationResult {

    /**
     * True if all changes from the resource were fetched.
     * (They might or might not be completely processed, though.)
     */
    private boolean allChangesFetched;

    /**
     * True if all fetched changes were processed (i.e. positively acknowledged).
     */
    private boolean allFetchedChangesProcessed;

    private LiveSyncToken initialToken;
    private LiveSyncToken tokenUpdatedTo;

    boolean isAllChangesFetched() {
        return allChangesFetched;
    }

    void setAllChangesFetched() {
        this.allChangesFetched = true;
    }

    boolean isAllFetchedChangesProcessed() {
        return allFetchedChangesProcessed;
    }

    void setAllFetchedChangesProcessed() {
        this.allFetchedChangesProcessed = true;
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

    public void setTokenUpdatedTo(LiveSyncToken value) {
        this.tokenUpdatedTo = value;
    }

    @Override
    public String toString() {
        return "allChangesFetched=" + allChangesFetched +
                ", allFetchedChangesProcessed=" + allFetchedChangesProcessed +
                ", initialToken=" + initialToken +
                ", tokenUpdatedTo=" + tokenUpdatedTo;
    }
}
