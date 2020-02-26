/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.query;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectGrouping;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.util.DebugUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ObjectPagingImpl implements ObjectPaging {

    private Integer offset;
    private Integer maxSize;
    @NotNull private final List<ObjectOrderingImpl> ordering = new ArrayList<>();
    private List<ObjectGroupingImpl> grouping = new ArrayList<>();

    private String cookie;

    private ObjectPagingImpl() {
    }

    private ObjectPagingImpl(Integer offset, Integer maxSize) {
        this.offset = offset;
        this.maxSize = maxSize;
    }

    private ObjectPagingImpl(Integer offset, Integer maxSize, ItemPath groupBy) {
        this.offset = offset;
        this.maxSize = maxSize;
        setGrouping(groupBy);
    }

    private ObjectPagingImpl(ItemPath orderBy, OrderDirection direction) {
        setOrdering(orderBy, direction);
    }

    private ObjectPagingImpl(ItemPath orderBy, OrderDirection direction, ItemPath groupBy) {
        setOrdering(orderBy, direction);
        setGrouping(groupBy);
    }

    private ObjectPagingImpl(Integer offset, Integer maxSize, ItemPath orderBy, OrderDirection direction) {
        this.offset = offset;
        this.maxSize = maxSize;
        setOrdering(orderBy, direction);
    }

    private ObjectPagingImpl(Integer offset, Integer maxSize, ItemPath orderBy, OrderDirection direction, ItemPath groupBy) {
        this.offset = offset;
        this.maxSize = maxSize;
        setOrdering(orderBy, direction);

        setGrouping(groupBy);
    }

    private ObjectPagingImpl(ItemPath groupBy) {
        setGrouping(groupBy);
    }

    public static ObjectPaging createPaging(Integer offset, Integer maxSize){
        return new ObjectPagingImpl(offset, maxSize);
    }

    public static ObjectPaging createPaging(Integer offset, Integer maxSize, ItemPath orderBy, OrderDirection direction) {
        return new ObjectPagingImpl(offset, maxSize, orderBy, direction);
    }

    public static ObjectPaging createPaging(Integer offset, Integer maxSize, ItemPath groupBy) {
        return new ObjectPagingImpl(offset, maxSize, groupBy);
    }

    public static ObjectPaging createPaging(Integer offset, Integer maxSize, ItemPath orderBy, OrderDirection direction, ItemPath groupBy) {
        return new ObjectPagingImpl(offset, maxSize, orderBy, direction, groupBy);
    }

    public static ObjectPaging createPaging(Integer offset, Integer maxSize, List<ObjectOrdering> orderings) {
        ObjectPagingImpl paging = new ObjectPagingImpl(offset, maxSize);
        paging.setOrdering(orderings);
        return paging;
    }

    public static ObjectPaging createPaging(Integer offset, Integer maxSize, List<ObjectOrdering> orderings, List<ObjectGrouping> groupings) {
        ObjectPagingImpl paging = new ObjectPagingImpl(offset, maxSize);
        paging.setOrdering(orderings);
        paging.setGrouping(groupings);
        return paging;
    }

    public static ObjectPaging createPaging(ItemPath orderBy, OrderDirection direction) {
        return new ObjectPagingImpl(orderBy, direction);
    }

    public static ObjectPaging createPaging(ItemPath orderBy, OrderDirection direction, ItemPath groupBy) {
        return new ObjectPagingImpl(orderBy, direction, groupBy);
    }

    public static ObjectPaging createPaging(ItemPath groupBy) {
        return new ObjectPagingImpl(groupBy);
    }

    public static ObjectPaging createEmptyPaging(){
        return new ObjectPagingImpl();
    }

    // TODO rename to getPrimaryOrderingDirection
    public OrderDirection getDirection() {
        ObjectOrdering primary = getPrimaryOrdering();
        return primary != null ? primary.getDirection() : null;
    }

    // TODO rename to getPrimaryOrderingPath
    public ItemPath getOrderBy() {
        ObjectOrdering primary = getPrimaryOrdering();
        return primary != null ? primary.getOrderBy() : null;
    }

    public ObjectOrdering getPrimaryOrdering() {
        if (hasOrdering()) {
            return ordering.get(0);
        } else {
            return null;
        }
    }

    public ItemPath getGroupBy(){
        ObjectGrouping primary = getPrimaryGrouping();
        return primary != null ? primary.getGroupBy() : null;
    }

    public ObjectGrouping getPrimaryGrouping() {
        if (hasGrouping()) {
            return grouping.get(0);
        } else {
            return null;
        }
    }

    // TODO name?
    public List<? extends ObjectOrdering> getOrderingInstructions() {
        return ordering;
    }

    public List<? extends ObjectGrouping> getGroupingInstructions() {
        return grouping;
    }

    public boolean hasOrdering() {
        return !ordering.isEmpty();
    }

    public void setOrdering(ItemPath orderBy, OrderDirection direction) {
        this.ordering.clear();
        addOrderingInstruction(orderBy, direction);
    }

    public boolean hasGrouping() {
        return !grouping.isEmpty();
    }

    public void setGrouping(ItemPath groupBy) {
        this.grouping.clear();
        addGroupingInstruction(groupBy);
    }

    public void addOrderingInstruction(ItemPath orderBy, OrderDirection direction) {
        this.ordering.add(ObjectOrderingImpl.createOrdering(orderBy, direction));
    }

    @SuppressWarnings("NullableProblems")
    public void setOrdering(ObjectOrdering... orderings) {
        this.ordering.clear();
        for (ObjectOrdering ordering : orderings) {
            this.ordering.add((ObjectOrderingImpl) ordering);
        }
    }

    public void setOrdering(Collection<? extends ObjectOrdering> orderings) {
        this.ordering.clear();
        for (ObjectOrdering ordering : CollectionUtils.emptyIfNull(orderings)) {
            this.ordering.add((ObjectOrderingImpl) ordering);
        }
    }

    public void addGroupingInstruction(ItemPath groupBy) {
        this.grouping.add(ObjectGroupingImpl.createGrouping(groupBy));
    }

    public void setGrouping(ObjectGrouping... groupings) {
        this.grouping.clear();
        for (ObjectGrouping grouping : groupings) {
            this.grouping.add((ObjectGroupingImpl) grouping);
        }
    }

    public void setGrouping(Collection<ObjectGrouping> groupings) {
        this.grouping.clear();
        for (ObjectGrouping grouping : CollectionUtils.emptyIfNull(groupings)) {
            this.grouping.add((ObjectGroupingImpl) grouping);
        }
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean hasCookie() {
        return cookie != null;
    }

    /**
     * Returns the paging cookie. The paging cookie is used for optimization of paged searches.
     * The presence of the cookie may allow the data store to correlate queries and associate
     * them with the same server-side context. This may allow the data store to reuse the same
     * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
     * It is expected that the cookie returned from the search will be passed back in the options
     * when the next page of the same search is requested.
     *
     * It is OK to initialize a search without any cookie. If the datastore utilizes a re-usable
     * context it will return a cookie in a search response.
     */
    public String getCookie() {
        return cookie;
    }

    /**
     * Sets paging cookie. The paging cookie is used for optimization of paged searches.
     * The presence of the cookie may allow the data store to correlate queries and associate
     * them with the same server-side context. This may allow the data store to reuse the same
     * pre-computed data. We want this as the sorted and paged searches may be quite expensive.
     * It is expected that the cookie returned from the search will be passed back in the options
     * when the next page of the same search is requested.
     *
     * It is OK to initialize a search without any cookie. If the datastore utilizes a re-usable
     * context it will return a cookie in a search response.
     */
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public ObjectPaging clone() {
        ObjectPagingImpl clone = new ObjectPagingImpl();
        copyTo(clone);
        return clone;
    }

    private void copyTo(ObjectPagingImpl clone) {
        clone.offset = this.offset;
        clone.maxSize = this.maxSize;
        clone.ordering.clear();
        clone.ordering.addAll(this.ordering);

        if (this.grouping != null) {
            clone.grouping = new ArrayList<>(this.grouping);
        } else {
            clone.grouping = null;
        }

        clone.cookie = this.cookie;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append("Paging:");
        if (getOffset() != null) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("Offset: ").append(getOffset());
        }
        if (getMaxSize() != null) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("Max size: ").append(getMaxSize());
        }
        if (hasOrdering()) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("Ordering: ").append(ordering);
        }
        if (hasGrouping()) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("Grouping: ").append(grouping);
        }
        if (getCookie() != null) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 1);
            sb.append("Cookie: ").append(getCookie());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PAGING: ");
        if (getOffset() != null){
            sb.append("O: ");
            sb.append(getOffset());
            sb.append(",");
        }
        if (getMaxSize() != null){
            sb.append("M: ");
            sb.append(getMaxSize());
            sb.append(",");
        }
        if (hasOrdering()) {
            sb.append("ORD: ");
            sb.append(ordering);
            sb.append(", ");
        }
        if (hasGrouping()) {
            sb.append("GRP: ");
            sb.append(grouping);
            sb.append(", ");
        }
        if (getCookie() != null) {
            sb.append("C:");
            sb.append(getCookie());
        }

        return sb.toString();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        return equals(o, true);
    }

    public boolean equals(Object o, boolean exact) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectPagingImpl that = (ObjectPagingImpl) o;

        if (offset != null ? !offset.equals(that.offset) : that.offset != null) {
            return false;
        }
        if (maxSize != null ? !maxSize.equals(that.maxSize) : that.maxSize != null) {
            return false;
        }
        if (ordering.size() != that.ordering.size()) {
            return false;
        }
        for (int i = 0; i < ordering.size(); i++) {
            ObjectOrdering oo1 = this.ordering.get(i);
            ObjectOrdering oo2 = that.ordering.get(i);
            if (!oo1.equals(oo2, exact)) {
                return false;
            }
        }
        if (grouping.size() != that.grouping.size()) {
            return false;
        }
        for (int i = 0; i < grouping.size(); i++) {
            ObjectGrouping og1 = this.grouping.get(i);
            ObjectGrouping og2 = that.grouping.get(i);
            if (!og1.equals(og2, exact)) {
                return false;
            }
        }
        return cookie != null ? cookie.equals(that.cookie) : that.cookie == null;
    }

    @Override
    public int hashCode() {
        int result = offset != null ? offset.hashCode() : 0;
        result = 31 * result + (maxSize != null ? maxSize.hashCode() : 0);
        result = 31 * result + ordering.hashCode();
        result = 31 * result + (grouping != null ? grouping.hashCode() : 0);
        result = 31 * result + (cookie != null ? cookie.hashCode() : 0);
        return result;
    }
}
