/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * @author Pavol Mederly
 */
public class QueryKey {

    private Class<? extends ObjectType> type;
    private QueryType query;        // consider using ObjectQuery here
    private ObjectSearchStrategyType searchStrategy;

    public <T extends ObjectType> QueryKey(Class<T> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy, PrismContext prismContext) {
        this.type = type;
        try {
            this.query = query != null ? prismContext.getQueryConverter().createQueryType(query) : null;
        } catch (SchemaException e) {
            throw new SystemException(e);
        }
        this.searchStrategy = searchStrategy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryKey queryKey = (QueryKey) o;

        if (query != null ? !query.equals(queryKey.query) : queryKey.query != null) return false;
        if (type != null ? !type.equals(queryKey.type) : queryKey.type != null) return false;
        if (searchStrategy != null ? !searchStrategy.equals(queryKey.searchStrategy) : queryKey.searchStrategy != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (query != null ? query.hashCode() : 0);
        result = 31 * result + (searchStrategy != null ? searchStrategy.hashCode() : 0);
        return result;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    @Override
    public String toString() {
        return "AbstractQueryKey{" +
                "type=" + type +
                ", query=" + query +
                ", searchStrategy=" + searchStrategy +
                '}';
    }
}
