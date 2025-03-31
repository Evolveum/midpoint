/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.local;

import java.util.Objects;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Key for single-type repository query cache. The query is stored as a clone, in order to make sure it won't be
 * changed during the lifetime of the cache entry.
 */
public class SingleTypeQueryKey {

    private final ObjectQuery query;
    private Integer cachedHashCode;

    public SingleTypeQueryKey(ObjectQuery query) {
        this.query = query != null ? query.clone() : null;
    }

    public <T extends ObjectType> QueryKey<T> toQueryKey(Class<T> type) {
        return new QueryKey<>(type, query);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof SingleTypeQueryKey that)) {
            return false;
        } else {
            var thisQuery = this.query;
            var thatQuery = that.query;
            return thisQuery != null ? thisQuery.equals(thatQuery, false) : thatQuery == null;
        }
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == null) {
            cachedHashCode = Objects.hash(query);
        }
        return cachedHashCode;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "QueryKey{" +
                "query=" + query +
                '}';
    }
}
