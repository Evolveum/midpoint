/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.ArrayList;
import java.util.List;

public class MidpointMcpSearchResult {

    private String type;
    private String query;
    /** {@code simple}, {@code advancedQuery}, or {@code mql}. */
    private String usedQueryMode;
    /** MQL filter used or generated; null for simple name-prefix mode. */
    private String translatedMql;
    private int limit;
    private int offset;
    private int totalCount;
    private List<MidpointMcpSearchItem> items = new ArrayList<>();

    /** Shadow searches: {@code repository} or {@code resource}. */
    private String source;

    /** Shadow searches: whether results include live resource-fetched data. */
    private Boolean fetched;

    /** Echo of shadow {@code searchMode} when applicable. */
    private String searchMode;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUsedQueryMode() {
        return usedQueryMode;
    }

    public void setUsedQueryMode(String usedQueryMode) {
        this.usedQueryMode = usedQueryMode;
    }

    public String getTranslatedMql() {
        return translatedMql;
    }

    public void setTranslatedMql(String translatedMql) {
        this.translatedMql = translatedMql;
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

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<MidpointMcpSearchItem> getItems() {
        return items;
    }

    public void setItems(List<MidpointMcpSearchItem> items) {
        this.items = items;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Boolean getFetched() {
        return fetched;
    }

    public void setFetched(Boolean fetched) {
        this.fetched = fetched;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }
}
