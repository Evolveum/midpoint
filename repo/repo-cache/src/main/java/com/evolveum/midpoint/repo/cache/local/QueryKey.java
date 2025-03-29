/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache.local;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Key for repository query cache. The query is stored as a clone, in order to make sure it won't be
 * changed during the lifetime of the cache entry.
 */
public class QueryKey<T extends ObjectType> {

    @NotNull private final Class<T> type;
    private final ObjectQuery query;
    private Integer cachedHashCode;

    public QueryKey(@NotNull Class<T> type, ObjectQuery query) {
        this.type = type;
        this.query = query != null ? query.clone() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryKey<?> that)) {
            return false;
        }
        if (!Objects.equals(this.type, that.type)) {
            return false;
        }
        var thisQuery = this.query;
        var thatQuery = that.query;
        return thisQuery != null ? thisQuery.equals(thatQuery, false) : thatQuery == null;
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == null) {
            cachedHashCode = Objects.hash(type, query);
        }
        return cachedHashCode;
    }

    @NotNull public Class<T> getType() {
        return type;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "QueryKey{" +
                "type=" + type.getSimpleName() +
                ", query=" + query +
                '}';
    }
}
