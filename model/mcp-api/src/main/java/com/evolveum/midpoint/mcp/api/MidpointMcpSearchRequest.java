/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.mcp.api;

import java.util.List;

/**
 * Request for {@link MidpointMcpService#searchObjects}.
 * At most one of {@link #getQuery()}, {@link #getAdvancedQuery()}, or {@link #getMql()} may be set (non-blank / non-null).
 */
public class MidpointMcpSearchRequest {

    /** REST collection name (required). */
    private String type;
    /**
     * Simple search: for most REST types, prefix match on {@code name}; for {@code cases}, substring match on name,
     * description, or work item names (OR).
     */
    private String query;
    /** Structured query translated to MQL. */
    private MidpointMcpAdvancedQuerySpec advancedQuery;
    /** Expert raw MQL filter (no translation). */
    private String mql;
    private Integer limit;
    private Integer offset;
    private List<String> returnAttributes;

    /**
     * Shadow-only with repository search: when {@code true}, refreshes each hit from the resource (very expensive—one
     * connector read per result). Use only when explicitly needed; omit or {@code false} for repository snapshot data
     * (e.g. resolving a user's {@code linkRef} to a shadow). Ignored for {@code searchMode == resource} (already live).
     * Not part of MQL.
     */
    private Boolean fetch;

    /**
     * Shadow-only: {@code repository} (default) searches stored shadows; {@code resource} searches the resource backend.
     * Not part of MQL.
     */
    private String searchMode;

    /** Required when {@code searchMode == resource}: resource OID to scope the search. */
    private String resourceOid;

    /**
     * Shadow resource search: {@link com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType} name
     * (e.g. {@code ACCOUNT}). Use with {@code resourceOid}; either this or {@link #objectClass} should be set.
     */
    private String shadowKind;

    /** Optional intent when {@link #shadowKind} is set. */
    private String shadowIntent;

    /**
     * Shadow resource search: object class QName string (e.g. URI or {@code ns:local}). Alternative to {@link #shadowKind}.
     */
    private String objectClass;

    /**
     * Shadow repository search only: when {@code true} with {@link #resourceOid}, builds an OR of all
     * {@code schemaHandling} object types on that resource (kind/intent) and ANDs it with the rest of the query.
     * Mutually exclusive with {@link #shadowKind}, {@link #shadowIntent}, and {@link #objectClass}. Not used with
     * {@code searchMode == resource}.
     */
    private Boolean expandResourceObjectTypes;

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

    public MidpointMcpAdvancedQuerySpec getAdvancedQuery() {
        return advancedQuery;
    }

    public void setAdvancedQuery(MidpointMcpAdvancedQuerySpec advancedQuery) {
        this.advancedQuery = advancedQuery;
    }

    public String getMql() {
        return mql;
    }

    public void setMql(String mql) {
        this.mql = mql;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public List<String> getReturnAttributes() {
        return returnAttributes;
    }

    public void setReturnAttributes(List<String> returnAttributes) {
        this.returnAttributes = returnAttributes;
    }

    public Boolean getFetch() {
        return fetch;
    }

    public void setFetch(Boolean fetch) {
        this.fetch = fetch;
    }

    public String getSearchMode() {
        return searchMode;
    }

    public void setSearchMode(String searchMode) {
        this.searchMode = searchMode;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public String getShadowKind() {
        return shadowKind;
    }

    public void setShadowKind(String shadowKind) {
        this.shadowKind = shadowKind;
    }

    public String getShadowIntent() {
        return shadowIntent;
    }

    public void setShadowIntent(String shadowIntent) {
        this.shadowIntent = shadowIntent;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public Boolean getExpandResourceObjectTypes() {
        return expandResourceObjectTypes;
    }

    public void setExpandResourceObjectTypes(Boolean expandResourceObjectTypes) {
        this.expandResourceObjectTypes = expandResourceObjectTypes;
    }
}
