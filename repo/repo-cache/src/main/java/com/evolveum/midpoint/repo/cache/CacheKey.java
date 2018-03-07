/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CacheKey {

    private Class<? extends ObjectType> type;
    private String oid;

    public CacheKey(Class<? extends ObjectType> type, String oid) {
        this.type = type;
        this.oid = oid;
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public String getOid() {
        return oid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheKey cacheKey = (CacheKey) o;

        if (type != null ? !type.equals(cacheKey.type) : cacheKey.type != null) return false;
        return oid != null ? oid.equals(cacheKey.oid) : cacheKey.oid == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (oid != null ? oid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CacheKey{");
        sb.append(type.getSimpleName());
        sb.append("[").append(oid).append("]}");
        return sb.toString();
    }
}
