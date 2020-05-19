package com.evolveum.axiom.concepts;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public interface Identifiable<I> {

    I identifier();


    static <K,V extends Identifiable<K>> Map<K,V> identifierMap(Iterable<? extends V> identifiables) {
        Builder<K,V> builder = ImmutableMap.builder();
        putAll(builder, identifiables);
        return builder.build();
    }

    static <K,V extends Identifiable<K>> void putAll(Builder<K,V> map, Iterable<? extends V> identifiables) {
        for (V v : identifiables) {
            map.put(v.identifier(), v);
        }
    }

    static <K,V extends Identifiable<K>> void putAll(Map<K,V> map, Iterable<? extends V> identifiables) {
        for (V v : identifiables) {
            map.put(v.identifier(), v);
        }
    }

}
