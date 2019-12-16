/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint;

import com.evolveum.midpoint.repo.api.CacheInvalidationDetails;

/**
 * Provides additional information/hints for the particular cache to employ.
 *
 * EXPERIMENTAL
 * TODO probably change to CacheInvalidationEvent and enclose also type, OID, and clusterwide flag
 */
public class CacheInvalidationContext {

    private boolean fromRemoteNode;
    private CacheInvalidationDetails details;

    public CacheInvalidationContext(boolean fromRemoteNode, CacheInvalidationDetails details) {
        this.fromRemoteNode = fromRemoteNode;
        this.details = details;
    }

    public boolean isFromRemoteNode() {
        return fromRemoteNode;
    }

    public void setFromRemoteNode(boolean fromRemoteNode) {
        this.fromRemoteNode = fromRemoteNode;
    }

    public CacheInvalidationDetails getDetails() {
        return details;
    }

    public void setDetails(CacheInvalidationDetails details) {
        this.details = details;
    }

    @Override
    public String toString() {
        return "CacheInvalidationContext{" +
                "fromRemoteNode=" + fromRemoteNode +
                ", details=" + details +
                '}';
    }
}
