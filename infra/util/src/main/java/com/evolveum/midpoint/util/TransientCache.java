/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util;

import javax.annotation.concurrent.ThreadSafe;

import com.evolveum.midpoint.util.annotation.Experimental;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache that is transient but can be declared final in serializable objects.
 * See https://stackoverflow.com/a/26785954/5810524
 *
 * EXPERIMENTAL
 */
@Experimental
@ThreadSafe
public class TransientCache<K, V> implements Serializable {

    private transient final Map<K, V> cache = new ConcurrentHashMap<>();

    public void invalidate() {
        cache.clear();
    }

    public V get(K name) {
        return cache.get(name);
    }

    public void put(K name, V value) {
        cache.put(name, value);
    }

    // used to provide a new object when deserializing
    private Object readResolve() {
        return new TransientCache<>();
    }
}
