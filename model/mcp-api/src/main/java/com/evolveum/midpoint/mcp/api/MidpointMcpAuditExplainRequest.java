/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/**
 * Explain a single audit record. {@code id} is normally {@code eventIdentifier}; numeric-only ids are resolved as
 * repository {@code repoId}.
 */
public class MidpointMcpAuditExplainRequest {

    private String id;
    private Boolean includeDelta;
    private Boolean includeResult;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getIncludeDelta() {
        return includeDelta;
    }

    public void setIncludeDelta(Boolean includeDelta) {
        this.includeDelta = includeDelta;
    }

    public Boolean getIncludeResult() {
        return includeResult;
    }

    public void setIncludeResult(Boolean includeResult) {
        this.includeResult = includeResult;
    }
}
