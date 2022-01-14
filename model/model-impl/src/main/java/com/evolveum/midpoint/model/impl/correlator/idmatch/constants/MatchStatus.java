package com.evolveum.midpoint.model.impl.correlator.idmatch.constants;

public enum MatchStatus {


    PENDING("/match/api/1/v1/matchRequests?status=pending"),
    RESOLVED("/match/api/1/v1/matchRequests?status=resolved");


    private final String url;

    MatchStatus(String envUrl) {
        this.url = envUrl;
    }

    public String getUrl() {
        return url;
    }

}
