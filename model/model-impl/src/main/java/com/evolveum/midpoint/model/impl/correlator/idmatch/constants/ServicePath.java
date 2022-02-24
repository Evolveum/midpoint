/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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


