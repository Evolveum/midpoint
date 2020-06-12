/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class DeltaSetTripleUtil {
    public static <T> void diff(Collection<T> valuesOld, Collection<T> valuesNew, DeltaSetTriple<T> triple) {
        if (valuesOld == null && valuesNew == null) {
            // No values, no change -> empty triple
            return;
        }
        if (valuesOld == null) {
            triple.getPlusSet().addAll(valuesNew);
            return;
        }
        if (valuesNew == null) {
            triple.getMinusSet().addAll(valuesOld);
            return;
        }
        for (T val : valuesOld) {
            if (valuesNew.contains(val)) {
                triple.getZeroSet().add(val);
            } else {
                triple.getMinusSet().add(val);
            }
        }
        for (T val : valuesNew) {
            if (!valuesOld.contains(val)) {
                triple.getPlusSet().add(val);
            }
        }
    }

    /**
     * Compares two (unordered) collections and creates a triple describing the differences.
     */
    public static <V extends PrismValue> PrismValueDeltaSetTriple<V> diffPrismValueDeltaSetTriple(Collection<V> valuesOld, Collection<V> valuesNew, PrismContext prismContext) {
        PrismValueDeltaSetTriple<V> triple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
        diff(valuesOld, valuesNew, triple);
        return triple;
    }

    public static <V extends PrismValue> PrismValueDeltaSetTriple<V> allToZeroSet(Collection<V> values, PrismContext prismContext) {
        PrismValueDeltaSetTriple<V> triple = prismContext.deltaFactory().createPrismValueDeltaSetTriple();
        triple.addAllToZeroSet(values);
        return triple;
    }

    public static <T> DeltaSetTriple<? extends T> find(Map<? extends ItemPath, DeltaSetTriple<? extends T>> tripleMap, ItemPath path) {
        List<Map.Entry<? extends ItemPath, DeltaSetTriple<? extends T>>> matching = tripleMap.entrySet().stream()
                .filter(e -> path.equivalent(e.getKey()))
                .collect(Collectors.toList());
        if (matching.isEmpty()) {
            return null;
        } else if (matching.size() == 1) {
            return matching.get(0).getValue();
        } else {
            throw new IllegalStateException("Multiple matching entries for key '" + path + "' in " + tripleMap);
        }
    }
}
