/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.idmatch.constants;

public enum Channel {


    URL_PREFIX_MAIN_OPERATIONS("/v1/people/"),
    URL_PREFIX_GET_MATCH_REQUEST_MATCH_ID("/v1/matchRequests/"),
    URL_PREFIX_GET_MATCH_REQUEST_REFERENCE_ID("/v1/matchRequests?referenceId=");


    private final String url;

    Channel(String envUrl) {
        this.url = envUrl;
    }

    public String getUrl() {
        return url;
    }
}


