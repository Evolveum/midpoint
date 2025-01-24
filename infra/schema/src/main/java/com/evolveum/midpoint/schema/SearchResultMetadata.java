/*
 * Copyright (C) 2014-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.ShortDumpable;

/**
 * @author semancik
 */
public class SearchResultMetadata
        extends AbstractFreezable
        implements Serializable, DebugDumpable, ShortDumpable, Cloneable {

    @Serial private static final long serialVersionUID = 1L;

    private String pagingCookie;
    private Integer approxNumberOfAllResults;
    private boolean partialResults = false;

    /**
     * Returns the paging cookie. The paging cookie is used for optimization of paged searches.
     * The presence of the cookie may allow the data store to correlate queries and associate
     * them with the same server-side context. This may allow the data store to reuse the same
     * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
     * It is expected that the cookie returned from the search will be passed back in the options
     * when the next page of the same search is requested.
     */
    public String getPagingCookie() {
        return pagingCookie;
    }

    /**
     * Sets paging cookie. The paging cookie is used for optimization of paged searches.
     * The presence of the cookie may allow the data store to correlate queries and associate
     * them with the same server-side context. This may allow the data store to reuse the same
     * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
     * It is expected that the cookie returned from the search will be passed back in the options
     * when the next page of the same search is requested.
     */
    public void setPagingCookie(String pagingCookie) {
        checkMutable();
        this.pagingCookie = pagingCookie;
    }

    public SearchResultMetadata pagingCookie(String pagingCookie) {
        checkMutable();
        this.pagingCookie = pagingCookie;
        return this;
    }

    /**
     * Returns the approximate number of all results that would be returned for the
     * filter if there was no paging limitation. This property is optional and it is
     * informational only. The implementation should return it only if it is extremely
     * cheap to get the information (e.g. if it is part of the response anyway).
     * The number may be approximate. The intended use of this value is to optimize presentation
     * of the data (e.g. to set approximate size of scroll bars, page counts, etc.)
     */
    public Integer getApproxNumberOfAllResults() {
        return approxNumberOfAllResults;
    }

    public void setApproxNumberOfAllResults(Integer approxNumberOfAllResults) {
        checkMutable();
        this.approxNumberOfAllResults = approxNumberOfAllResults;
    }

    public SearchResultMetadata approxNumberOfAllResults(Integer approxNumberOfAllResults) {
        checkMutable();
        this.approxNumberOfAllResults = approxNumberOfAllResults;
        return this;
    }

    /**
     * Flag indicating whether the search returned partial results.
     * If set to false then all the results requested by the query were returned.
     * If set to true then only some results requested by the query were returned.
     */
    public boolean isPartialResults() {
        return partialResults;
    }

    public void setPartialResults(boolean partialResults) {
        checkMutable();
        this.partialResults = partialResults;
    }

    public SearchResultMetadata partialResults(boolean partialResults) {
        checkMutable();
        this.partialResults = partialResults;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SearchResultMetadata that = (SearchResultMetadata) o;
        return partialResults == that.partialResults
                && Objects.equals(pagingCookie, that.pagingCookie)
                && Objects.equals(approxNumberOfAllResults, that.approxNumberOfAllResults);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pagingCookie, approxNumberOfAllResults, partialResults);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SearchResultMetadata(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        if (pagingCookie != null) {
            sb.append("pagingCookie=").append(pagingCookie).append(",");
        }
        if (approxNumberOfAllResults != null) {
            sb.append("approxNumberOfAllResults=").append(approxNumberOfAllResults).append(",");
        }
        if (partialResults) {
            sb.append("partialResults=true,");
        }
        if (pagingCookie != null || approxNumberOfAllResults != null || partialResults) {
            sb.setLength(sb.length() - 1);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilder(SearchResultMetadata.class, indent);
        sb.append("(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public SearchResultMetadata clone() {
        SearchResultMetadata clone = new SearchResultMetadata();
        clone.pagingCookie = pagingCookie;
        clone.approxNumberOfAllResults = approxNumberOfAllResults;
        clone.partialResults = partialResults;
        return clone;
    }
}
