/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class LocalVersionCache extends AbstractThreadLocalCache {

    private final Map<String, String> data = new ConcurrentHashMap<>();

    public String get(String oid) {
        return data.get(oid);
    }

    public void put(String oid, String version) {
        data.put(oid, version);
    }

    public void remove(String oid) {
        data.remove(oid);
    }

    @Override
    public String description() {
        return "V:" + data.size();
    }

    @Override
    protected int getSize() {
        return data.size();
    }
}
