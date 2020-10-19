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
 * @see ParameterizedEquivalenceStrategy for detailed explanation of individual strategies.
 */

public interface EquivalenceStrategy {

    /**
     * Currently the highest level of recognition. Roughly corresponds to comparing serialized forms of the data.
     * Useful e.g. for comparing values for the purpose of XML editing.
     *
     * Ignores aspects that are not covered by serialization e.g. definitions, parent objects, origin, immutability flag, etc.
     */
    ParameterizedEquivalenceStrategy LITERAL = ParameterizedEquivalenceStrategy.literal();

    /**
     * Captures the data. Ignores minor serialization-related things that are not relevant for the parsed data,
     * like namespace prefixes. Also ignores the difference between null and default relation.
     *
     * Currently this is the default for equals/hashCode.
     */
    ParameterizedEquivalenceStrategy DATA = ParameterizedEquivalenceStrategy.data();

    /**
     * Captures the "real value" of the data: it is something that we consider equivalent so that
     * if prism values A and B have the same real value, we do not want to be both present
     * in the same multi-valued item (like assignment, roleMembershipRef, or whatever).
     *
     * However, in reality, we usually take container IDs (if both are present) into account,
     * so if PCV A1 and A2 have equal content but different IDs they are treated as different.
     * This is reflected in {@link #REAL_VALUE_CONSIDER_DIFFERENT_IDS} strategy.
     */
    ParameterizedEquivalenceStrategy REAL_VALUE = ParameterizedEquivalenceStrategy.realValue();

    /**
     * As {@link #REAL_VALUE} but taking different PCV IDs into account (if both are present).
     *
     * Currently this is the default for delta application. See {@link ParameterizedEquivalenceStrategy#FOR_DELTA_ADD_APPLICATION}
     * and {@link ParameterizedEquivalenceStrategy#FOR_DELTA_DELETE_APPLICATION}.
     *
     * It is not quite clear if this strategy is well-formed. Often we want to differentiate PCV IDs
     * but only on some levels, e.g. for assignments, but not on others, e.g. inside assignments. This
     * has to be sorted out.
     */
    ParameterizedEquivalenceStrategy REAL_VALUE_CONSIDER_DIFFERENT_IDS = ParameterizedEquivalenceStrategy.realValueConsiderDifferentIds();

    /**
     * This is something between {@link #DATA} and {@link #REAL_VALUE}: ignores
     * operational items and values, container IDs, value metadata (just like REAL_VALUE) but
     * takes reference filters and reference resolution options (time, integrity), as well as
     * item names into account (like DATA).
     *
     * It is not quite clear whether and when to use this strategy.
     */
    ParameterizedEquivalenceStrategy IGNORE_METADATA = ParameterizedEquivalenceStrategy.ignoreMetadata();

    @Deprecated // use DATA instead
    ParameterizedEquivalenceStrategy NOT_LITERAL = ParameterizedEquivalenceStrategy.data();

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
