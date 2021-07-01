/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * TODO
 */
@Experimental
public class UcfFetchChangesResult {

    /**
     * Set to true when all changes from the resource were fetched. This is meant to let the caller know that
     * it can update the token in the task (if token values are not known to be "precise".)
     *
     * Note that finalToken value might or might not be present; depending on the connector implementation.
     */
    private final boolean allChangesFetched;

    private final UcfSyncToken finalToken;

    public boolean isAllChangesFetched() {
        return allChangesFetched;
    }

    public UcfSyncToken getFinalToken() {
        return finalToken;
    }

    public UcfFetchChangesResult(boolean allChangesFetched, UcfSyncToken finalToken) {
        this.allChangesFetched = allChangesFetched;
        this.finalToken = finalToken;
    }
}
