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
import com.evolveum.midpoint.prism.path.PathKeyedMap;

import java.util.Collection;

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

    public static <T> void putIntoOutputTripleMap(PathKeyedMap<DeltaSetTriple<T>> outputTripleMap,
            ItemPath outputPath, DeltaSetTriple<T> outputTriple) {
        if (outputTriple != null) {
            DeltaSetTriple<T> mapTriple = outputTripleMap.get(outputPath);
            if (mapTriple == null) {
                outputTripleMap.put(outputPath, outputTriple);
            } else {
                mapTriple.merge(outputTriple);
            }
        }
    }
}
