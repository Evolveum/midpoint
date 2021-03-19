package com.evolveum.concepts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

class KnownImmutables {

    private static final Set<Class<?>> KNOWN_IMMUTABLES = ImmutableSet.<Class<?>>builder()

            .add(Void.class, Class.class, String.class, Character.class)
            .add(Byte.class, Short.class, Integer.class, Long.class, BigInteger.class)
            .add(Float.class, Double.class, BigDecimal.class)

            .add(byte.class, short.class, int.class, long.class)
            .add(float.class, double.class)

            // FIXME: Add common classes from java.time.*

            // Collections.empty*  (they are private)
            .add(Collections.emptyList().getClass())
            .add(Collections.emptySet().getClass())
            .add(Collections.emptyMap().getClass())

            // Collections.singleton* classes (they are private)
            .add(Collections.singleton("").getClass())
            .add(Collections.singletonList("").getClass())
            .add(Collections.singletonMap("", "").getClass())

            .add(QName.class)
            .build();

    static boolean isImmutable(Object object) {
        if (object instanceof Immutable) {
            return true;
        }
        if (object instanceof MutationBehaviourAware<?>) {
            return ((MutationBehaviourAware<?>) object).mutable();
        }
        if (object instanceof String) {
            return true;
        }
        if (object instanceof ImmutableCollection<?>) {
            return true;
        }
        if (object instanceof ImmutableMap<?,?>) {
            return true;
        }
        if (object instanceof ImmutableMultimap<?,?>) {
            return true;
        }
        return KNOWN_IMMUTABLES.contains(object.getClass());
    }

}
