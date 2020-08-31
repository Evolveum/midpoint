/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.path;

import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * Special case of a map that has ItemPath as a key.
 *
 * The main issue with path-keyed maps is that comparing item paths using equals/hashCode is
 * unreliable. UniformItemPath was conceived as way to improve that, but even it does not solve
 * this issue completely.
 *
 * An alternative design to this class would be to use some wrapper class for ItemPath that would
 * provide equals() method with the same semantics as equivalent(). But what about hashCode then?
 *
 * This map does _not_ support null keys. Also, collections returned by keySet(), values(), entrySet()
 * are not modifiable.
 */
@Experimental
public class PathKeyedMap<T> implements Map<ItemPath, T>, Serializable {

    private final Map<ItemPath, T> internalMap = new HashMap<>();

    @Override
    public int size() {
        return internalMap.size();
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof ItemPath &&
                ItemPathCollectionsUtil.containsEquivalent(internalMap.keySet(), (ItemPath) key);
    }

    @Override
    public boolean containsValue(Object value) {
        return internalMap.containsValue(value);
    }

    @Override
    public T get(Object key) {
        if (key instanceof ItemPath) {
            for (Map.Entry<ItemPath, T> entry : internalMap.entrySet()) {
                if (entry.getKey().equivalent((ItemPath) key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    @Nullable
    @Override
    public T put(ItemPath key, T value) {
        Objects.requireNonNull(key);
        for (ItemPath existingKey : internalMap.keySet()) {
            if (existingKey.equivalent(key)) {
                return internalMap.put(existingKey, value);
            }
        }
        return internalMap.put(key, value);
    }

    @Override
    public T remove(Object key) {
        if (key instanceof ItemPath) {
            for (ItemPath existingKey : internalMap.keySet()) {
                if (existingKey.equivalent((ItemPath) key)) {
                    return internalMap.remove(existingKey);
                }
            }
        }
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends ItemPath, ? extends T> m) {
        for (Entry<? extends ItemPath, ? extends T> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        internalMap.clear();
    }

    @NotNull
    @Override
    public Set<ItemPath> keySet() {
        return Collections.unmodifiableSet(internalMap.keySet());
    }

    @NotNull
    @Override
    public Collection<T> values() {
        return Collections.unmodifiableCollection(internalMap.values());
    }

    @NotNull
    @Override
    public Set<Entry<ItemPath, T>> entrySet() {
        return Collections.unmodifiableSet(internalMap.entrySet());
    }
}
