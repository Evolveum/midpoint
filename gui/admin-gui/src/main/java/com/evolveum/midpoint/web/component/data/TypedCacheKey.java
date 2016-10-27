/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
