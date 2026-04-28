/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured advanced query shape used by object search (paths from {@code midpoint_describe_object_type_schema}) and
 * by audit search (separate audit path vocabulary; see MCP audit tool documentation). The server interprets {@code path}
 * values according to the active operation.
 */
public class MidpointMcpAdvancedQuerySpec {

    /** {@code and} or {@code or}; defaults to {@code and}. */
    private String combine;
    private List<MidpointMcpAdvancedFilterSpec> filters = new ArrayList<>();
    private List<MidpointMcpAdvancedOrderBySpec> orderBy = new ArrayList<>();
    private MidpointMcpAdvancedPagingSpec paging;

    public String getCombine() {
        return combine;
    }

    public void setCombine(String combine) {
        this.combine = combine;
    }

    public List<MidpointMcpAdvancedFilterSpec> getFilters() {
        return filters;
    }

    public void setFilters(List<MidpointMcpAdvancedFilterSpec> filters) {
        this.filters = filters != null ? filters : new ArrayList<>();
    }

    public List<MidpointMcpAdvancedOrderBySpec> getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(List<MidpointMcpAdvancedOrderBySpec> orderBy) {
        this.orderBy = orderBy != null ? orderBy : new ArrayList<>();
    }

    public MidpointMcpAdvancedPagingSpec getPaging() {
        return paging;
    }

    public void setPaging(MidpointMcpAdvancedPagingSpec paging) {
        this.paging = paging;
    }
}
