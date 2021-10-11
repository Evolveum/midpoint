/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class LocalQueryCache extends AbstractThreadLocalCache {

    private final Map<QueryKey, SearchResultList> data = new ConcurrentHashMap<>();

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

    @Override
    protected int getSize() {
        return data.size();
    }

    public Iterator<Map.Entry<QueryKey, SearchResultList>> getEntryIterator() {
        return data.entrySet().iterator();
    }
}
