/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

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
