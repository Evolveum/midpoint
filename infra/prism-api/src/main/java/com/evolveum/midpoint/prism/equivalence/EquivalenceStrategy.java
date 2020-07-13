/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.equivalence;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;

import java.util.Comparator;

/**
 * A strategy used to determine equivalence of prism items and values.
 *
 * This is quite generic interface. We expect that usually it will not be implemented directly, because comparing prism
 * structures is a complex undertaking. The usual approach will be using ParameterizedEquivalenceStrategy that contains
 * a set of parameters that drive equals/hashCode methods built into prism structures.
 *
 * However, if anyone would need the ultimate flexibility, he is free to implement this interface from scratch.
 *
 * (Note that not all methods in prism API accept this generic form of equivalence strategy. For example, diff(..) methods
 * are limited to ParameterizedEquivalenceStrategy at least for now.)
 *
 * @see ParameterizedEquivalenceStrategy for explanation of individual strategies.
 */

public interface EquivalenceStrategy {

    ParameterizedEquivalenceStrategy LITERAL = ParameterizedEquivalenceStrategy.literal();
    ParameterizedEquivalenceStrategy NOT_LITERAL = ParameterizedEquivalenceStrategy.notLiteral();
    ParameterizedEquivalenceStrategy IGNORE_METADATA = ParameterizedEquivalenceStrategy.ignoreMetadata();
    ParameterizedEquivalenceStrategy IGNORE_METADATA_CONSIDER_DIFFERENT_IDS = ParameterizedEquivalenceStrategy.ignoreMetadataConsiderDifferentIds();
    ParameterizedEquivalenceStrategy LITERAL_IGNORE_METADATA = ParameterizedEquivalenceStrategy.literalIgnoreMetadata();
    ParameterizedEquivalenceStrategy REAL_VALUE = ParameterizedEquivalenceStrategy.realValue();
    ParameterizedEquivalenceStrategy REAL_VALUE_CONSIDER_DIFFERENT_IDS = ParameterizedEquivalenceStrategy.realValueConsiderDifferentIds();

    boolean equals(Item<?,?> first, Item<?,?> second);
    boolean equals(PrismValue first, PrismValue second);

    int hashCode(Item<?,?> item);
    int hashCode(PrismValue value);

    default <V extends PrismValue> Comparator<V> prismValueComparator() {
        return (o1, o2) -> {
            if (o1.equals(o2, this)) {
                return 0;
            } else {
                return 1;
            }
        };
    }
}
