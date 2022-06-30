/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

public interface ItemMerger {

    /**
     * Merges all data about specific item (named `itemName`) from `source` container value to `target` one, according
     * to (its own) strategy.
     *
     * So, `source` is not modified; the `target` is.
     *
     * The implementation is responsible for setting origin information on all prism values copied from `source` to `target`.
     */
    void merge(@NotNull ItemName itemName, @NotNull PrismContainerValue<?> target, @NotNull PrismContainerValue<?> source)
            throws ConfigurationException, SchemaException;
}
