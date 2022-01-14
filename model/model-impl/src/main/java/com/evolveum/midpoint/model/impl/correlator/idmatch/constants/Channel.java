package com.evolveum.midpoint.model.impl.correlator.idmatch.constants;

public enum Channel {


    URL_PREFIX_MAIN_OPERATIONS("/match/api/1/v1/people/"),
    URL_PREFIX_GET_MATCH_REQUEST_MATCH_ID("/match/api/1/v1/matchRequests/"),
    URL_PREFIX_GET_MATCH_REQUEST_REFERENCE_ID("/match/api/1/v1/matchRequests?referenceId=");


    private final String url;

    Channel(String envUrl) {
        this.url = envUrl;
    }

    public String getUrl() {
        return url;
    }
}


