/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.SimpleVisitable;
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.DebugDumpable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Radovan Semancik
 */
public interface DeltaMapTriple<K,V> extends DebugDumpable, Serializable, SimpleVisitable<Map.Entry<K, V>> {

    Map<K,V> getZeroMap();

    Map<K,V> getPlusMap();

    Map<K,V> getMinusMap();

    Map<K,V> getMap(PlusMinusZero plusMinusZero);

    boolean hasPlusMap();

    boolean hasZeroMap();

    boolean hasMinusMap();

    boolean isZeroOnly();

    void addToPlusMap(K key, V value);

    void addToMinusMap(K key, V value);

    void addToZeroMap(K key, V value);

    void addAllToPlusMap(Map<K, V> map);

    void addAllToMinusMap(Map<K, V> map);

    void addAllToZeroMap(Map<K, V> map);

    void addAllToMap(PlusMinusZero destination, Map<K, V> map);

    void clearPlusMap();

    void clearMinusMap();

    void clearZeroMap();

    int size();

    void merge(DeltaMapTriple<K, V> triple);

    /**
     * Returns all values, regardless of the internal sets.
     */
    Collection<K> unionKeySets();

    DeltaMapTriple<K,V> clone(Cloner<Entry<K, V>> cloner);

    boolean isEmpty();

    default void clear() {
        clearPlusMap();
        clearMinusMap();
        clearZeroMap();
    }
}
