/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemName;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Selects a value from multivalued item (property, container, reference). A typical use is to select
 * among PrismContainerValues by checking some sub-item ("key") value.
 *
 * TODO Find a better name. "ValueMatcher" is already used in a different context.
 *
 */
@FunctionalInterface
public interface ValueSelector<V extends PrismValue> extends Predicate<V> {

    /**
     * Matches PrismContainerValue if it has single-valued sub-item named "itemName" with the value of "expectedValue"
     * (or if the sub-item is not present and expectedValue is null).
     */
    static <C extends Containerable> ValueSelector<PrismContainerValue<C>> itemEquals(ItemName itemName, Object expectedValue) {
        return containerValue -> {
            Item<?, ?> item = containerValue.findItem(itemName);
            Object itemValue = item != null ? item.getRealValue() : null;
            return Objects.equals(itemValue, expectedValue);
        };
    }
}
