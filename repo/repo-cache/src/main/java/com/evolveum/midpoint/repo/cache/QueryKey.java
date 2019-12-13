/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Objects;

/**
 * Key for repository query cache. The query is stored as a clone, in order to make sure it won't be
 * changed during the lifetime of the cache entry.
 */
public class QueryKey {

    private final Class<? extends ObjectType> type;
    private final ObjectQuery query;
    private Integer cachedHashCode;

    <T extends ObjectType> QueryKey(Class<T> type, ObjectQuery query) {
        this.type = type;
        this.query = query.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof QueryKey)) {
            return false;
        } else {
            QueryKey queryKey = (QueryKey) o;
            return Objects.equals(type, queryKey.type) &&
                    Objects.equals(query, queryKey.query);
        }
    }

    @Override
    public int hashCode() {
        if (cachedHashCode == null) {
            cachedHashCode = Objects.hash(type, query);
        }
        return cachedHashCode;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "QueryKey{" +
                "type=" + type +
                ", query=" + query +
                '}';
    }
}
