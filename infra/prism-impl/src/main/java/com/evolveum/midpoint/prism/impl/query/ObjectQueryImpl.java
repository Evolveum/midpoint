/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.util.DebugUtil;

public class ObjectQueryImpl implements ObjectQuery {
    private static final long serialVersionUID = 1L;

    private ObjectFilter filter;
    private ObjectPaging paging;
    private boolean allowPartialResults = false;

    public ObjectFilter getFilter() {
        return filter;
    }

    public void setFilter(ObjectFilter filter) {
        this.filter = filter;
    }

    public void setPaging(ObjectPaging paging) {
        this.paging = paging;
    }

    public ObjectPaging getPaging() {
        return paging;
    }

    public boolean isAllowPartialResults() {
        return allowPartialResults;
    }

    public void setAllowPartialResults(boolean allowPartialResults) {
        this.allowPartialResults = allowPartialResults;
    }

    public static ObjectQuery createObjectQuery() {
        return new ObjectQueryImpl();
    }

    public static ObjectQuery createObjectQuery(ObjectFilter filter) {
        ObjectQueryImpl query = new ObjectQueryImpl();
        query.setFilter(filter);
        return query;
    }

    public static ObjectQuery createObjectQuery(XNodeImpl condition, ObjectFilter filter) {
        ObjectQueryImpl query = new ObjectQueryImpl();
        query.setFilter(filter);
        return query;
    }

    public static ObjectQuery createObjectQuery(ObjectPaging paging) {
        ObjectQueryImpl query = new ObjectQueryImpl();
        query.setPaging(paging);
        return query;
    }

    public static ObjectQuery createObjectQuery(ObjectFilter filter, ObjectPaging paging) {
        ObjectQueryImpl query = new ObjectQueryImpl();
        query.setFilter(filter);
        query.setPaging(paging);
        return query;
    }

    public ObjectQueryImpl clone() {
        ObjectQueryImpl clone = cloneEmpty();
        if (this.filter != null) {
            clone.filter = this.filter.clone();
        }
        return clone;
    }

    public ObjectQueryImpl cloneEmpty() {
        ObjectQueryImpl clone = new ObjectQueryImpl();
        if (this.paging != null) {
            clone.paging = this.paging.clone();
        }
        if (this.allowPartialResults) {
            clone.allowPartialResults = true;
        }
        return clone;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        DebugUtil.indentDebugDump(sb, indent);
        if (filter == null) {
            sb.append("Filter: null");
        } else {
            sb.append("Filter:");
            sb.append("\n");
            sb.append(filter.debugDump(indent + 1));
        }

        sb.append("\n");
        DebugUtil.indentDebugDump(sb, indent);
        if (paging == null) {
            sb.append("Paging: null");
        } else {
            sb.append(paging.debugDump(0));
        }

        if (allowPartialResults) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent);
            sb.append("Allow partial results");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Q{");
        if (filter != null) {
            sb.append(filter.toString());
            sb.append(",");
        } else {
            sb.append("null filter");
        }
        if (paging != null) {
            sb.append(paging.toString());
            sb.append(",");
        } else {
            sb.append("null paging");
        }
        if (allowPartialResults) {
            sb.append(",partial");
        }
        return sb.toString();
    }

    public void addFilter(ObjectFilter objectFilter) {
        if (objectFilter == null || objectFilter instanceof AllFilter) {
            // nothing to do
        } else if (filter == null || filter instanceof AllFilter) {
            setFilter(objectFilter);
        } else {
            setFilter(AndFilterImpl.createAnd(objectFilter, filter));
        }
    }

    // use when offset/maxSize is expected
    public Integer getOffset() {
        if (paging == null) {
            return null;
        }
//        if (paging.getCookie() != null) {
//            throw new UnsupportedOperationException("Paging cookie is not supported here.");
//        }
        return paging.getOffset();
    }

    // use when offset/maxSize is expected
    public Integer getMaxSize() {
        if (paging == null) {
            return null;
        }
//        if (paging.getCookie() != null) {
//            throw new UnsupportedOperationException("Paging cookie is not supported here.");
//        }
        return paging.getMaxSize();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return equals(o, true);
    }

    public boolean equivalent(Object o) {
        return equals(o, false);
    }

    public boolean equals(Object o, boolean exact) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectQueryImpl that = (ObjectQueryImpl) o;

        if (allowPartialResults != that.allowPartialResults) {
            return false;
        }
        if (filter != null ? !filter.equals(that.filter, exact) : that.filter != null) {
            return false;
        }
        return paging != null ? paging.equals(that.paging, exact) : that.paging == null;

    }

    @Override
    public int hashCode() {
        int result = filter != null ? filter.hashCode() : 0;
        result = 31 * result + (paging != null ? paging.hashCode() : 0);
        result = 31 * result + (allowPartialResults ? 1 : 0);
        return result;
    }
}
