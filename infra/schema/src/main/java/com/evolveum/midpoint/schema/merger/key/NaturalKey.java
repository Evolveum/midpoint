/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.key;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.merger.GenericItemMerger;
import com.evolveum.midpoint.util.exception.ConfigurationException;

/**
 * Describes how to handle a natural key of multi-valued items.
 *
 * Responsibilities:
 *
 * 1. _Matching_ values of a multi-valued item, so that the matching pairs can be appropriately merged.
 * 2. _Merging_ values of the key itself. For example, definitions of an attribute `ri:drink` with `ref`
 * of `drink` (unqualified) and `ri:drink` (qualified) have to be merged, and the result should use the
 * `ref` of `ri:drink`, i.e. the qualified version.
 *
 * (See also the description in {@link GenericItemMerger}, where this class is primarily used.)
 */
public interface NaturalKey {

    /**
     * Returns `true` if the target and source container values match on their natural key.
     * (I.e. they have to be merged together.)
     */
    boolean valuesMatch(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue) throws ConfigurationException;

    /**
     * Merges natural key value in target and in source (assuming they match according to {@link #valuesMatch(PrismContainerValue,
     * PrismContainerValue)}), i.e. updates the key in targetValue if necessary.
     */
    void mergeMatchingKeys(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue) throws ConfigurationException;
}
