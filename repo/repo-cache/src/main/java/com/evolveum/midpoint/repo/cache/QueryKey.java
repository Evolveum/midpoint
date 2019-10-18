/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author Pavol Mederly
 */
public class QueryKey {

    private Class<? extends ObjectType> type;
    private ObjectQuery query;

    public <T extends ObjectType> QueryKey(Class<T> type, ObjectQuery query) {
        this.type = type;
        this.query = query;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueryKey queryKey = (QueryKey) o;

        if (query != null ? !query.equals(queryKey.query) : queryKey.query != null) return false;
        if (type != null ? !type.equals(queryKey.type) : queryKey.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (query != null ? query.hashCode() : 0);
        return result;
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
