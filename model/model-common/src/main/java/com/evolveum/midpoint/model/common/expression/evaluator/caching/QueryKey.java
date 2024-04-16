/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class QueryKey {

    private final Class<? extends ObjectType> type;
    private final Collection<ObjectQuery> queries;
    private final ObjectSearchStrategyType searchStrategy;

    public <T extends ObjectType> QueryKey(
            Class<T> type, Collection<ObjectQuery> queries, ObjectSearchStrategyType searchStrategy) {
        this.type = type;
        this.queries = queries;
        this.searchStrategy = searchStrategy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        QueryKey queryKey = (QueryKey) o;
        return Objects.equals(type, queryKey.type)
                && Objects.equals(queries, queryKey.queries)
                && searchStrategy == queryKey.searchStrategy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, queries, searchStrategy);
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "AbstractQueryKey{" +
                "type=" + type +
                ", queries=" + queries +
                ", searchStrategy=" + searchStrategy +
                '}';
    }
}
