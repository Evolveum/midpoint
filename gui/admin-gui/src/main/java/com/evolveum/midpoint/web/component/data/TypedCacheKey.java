/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.prism.query.ObjectQuery;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class TypedCacheKey implements Serializable {

    private ObjectQuery query;
    private Class type;

    public TypedCacheKey(ObjectQuery query, Class type) {
        this.query = query;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypedCacheKey cacheKey = (TypedCacheKey) o;

        if (query != null ? !query.equals(cacheKey.query) : cacheKey.query != null) return false;
        if (type != null ? !type.equals(cacheKey.type) : cacheKey.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = query != null ? query.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
