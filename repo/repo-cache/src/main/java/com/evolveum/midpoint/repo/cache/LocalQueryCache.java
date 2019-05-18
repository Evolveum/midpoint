/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public class LocalQueryCache extends AbstractThreadLocalCache {
    private Map<QueryKey, SearchResultList> data = new HashMap<>();

    public SearchResultList get(QueryKey key) {
        return data.get(key);
    }

    public void put(QueryKey key, SearchResultList objects) {
        data.put(key, objects);
    }

    public void remove(QueryKey key) {
        data.remove(key);
    }

    @Override
    public String description() {
        return "Q:" + data.size();
    }

	public Iterator<Map.Entry<QueryKey, SearchResultList>> getEntryIterator() {
        return data.entrySet().iterator();
    }
}
