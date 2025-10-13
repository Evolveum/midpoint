/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.constants;

public enum ServicePath {

    PEOPLE("/v1/people/"),
    MATCH_REQUESTS("/v1/matchRequests/"),
    MATCH_REQUESTS_WITH_REFERENCE_ID("/v1/matchRequests?referenceId=");

    private final String url;

    ServicePath(String envUrl) {
        this.url = envUrl;
    }

    public String getUrl() {
        return url;
    }
}


