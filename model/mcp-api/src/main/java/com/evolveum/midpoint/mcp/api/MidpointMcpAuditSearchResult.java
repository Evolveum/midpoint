/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MidpointMcpAuditSearchResult {

    /** {@code simple} or {@code advancedQuery}. */
    private String usedQueryMode;
    private MidpointMcpAuditEffectiveWindow effectiveWindow;
    private int totalCount;
    /** Number of records in this page ({@link #records}.size()). */
    private int count;
    private List<MidpointMcpAuditRecordSummary> records = new ArrayList<>();
    /** MQL filter from advanced mode (excluding mandatory time window), for debugging. */
    private String translatedQuery;
    private int limit;
    private int offset;

    public String getUsedQueryMode() {
        return usedQueryMode;
    }

    public void setUsedQueryMode(String usedQueryMode) {
        this.usedQueryMode = usedQueryMode;
    }

    public MidpointMcpAuditEffectiveWindow getEffectiveWindow() {
        return effectiveWindow;
    }

    public void setEffectiveWindow(MidpointMcpAuditEffectiveWindow effectiveWindow) {
        this.effectiveWindow = effectiveWindow;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<MidpointMcpAuditRecordSummary> getRecords() {
        return records;
    }

    public void setRecords(List<MidpointMcpAuditRecordSummary> records) {
        this.records = records != null ? records : new ArrayList<>();
    }

    public String getTranslatedQuery() {
        return translatedQuery;
    }

    public void setTranslatedQuery(String translatedQuery) {
        this.translatedQuery = translatedQuery;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
