/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.cache;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.caching.AbstractThreadLocalCache;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class LocalObjectCache extends AbstractThreadLocalCache {

    private final Map<String, PrismObject<? extends ObjectType>> data = new ConcurrentHashMap<>();

    public PrismObject<? extends ObjectType> get(String oid) {
        return data.get(oid);
    }

    public void put(String oid, PrismObject<? extends ObjectType> object) {
        data.put(oid, object);
    }

    public void remove(String oid) {
        data.remove(oid);
    }

    @Override
    public String description() {
        return "O:" + data.size();
    }

    @Override
    protected int getSize() {
        return data.size();
    }
}
