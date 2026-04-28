/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/**
 * Audit search for {@link MidpointMcpService#searchAudit}. At most one of {@link #query} or
 * {@link #advancedQuery} may be set.
 */
public class MidpointMcpAuditSearchRequest {

    /** Inclusive lower bound (ISO-8601). Optional if {@code to} is set; default window applies if both absent. */
    private String from;
    /** Inclusive upper bound (ISO-8601). Optional if {@code from} is set; default window applies if both absent. */
    private String to;

    /** Simple free-text search; mutually exclusive with {@link #advancedQuery}. */
    private String query;

    /** Structured filters over the MCP audit path vocabulary; mutually exclusive with {@link #query}. */
    private MidpointMcpAdvancedQuerySpec advancedQuery;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public MidpointMcpAdvancedQuerySpec getAdvancedQuery() {
        return advancedQuery;
    }

    public void setAdvancedQuery(MidpointMcpAdvancedQuerySpec advancedQuery) {
        this.advancedQuery = advancedQuery;
    }
}
