/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

/**
 * Optional paging inside {@link MidpointMcpAdvancedQuerySpec}; when present, overrides top-level
 * {@link MidpointMcpSearchRequest#getLimit()}/{@link MidpointMcpSearchRequest#getOffset()}.
 */
public class MidpointMcpAdvancedPagingSpec {

    private Integer offset;
    private Integer limit;

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
