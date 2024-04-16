/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.impl.GenericItemMerger;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Implements the actual merging of two objects ({@link Containerable}) of the same type.
 *
 * @param <C> type of objects to be merged
 */
public class BaseMergeOperation<C extends Containerable> {

    @NotNull private final C target;
    @NotNull private final C source;
    @NotNull private final GenericItemMerger rootMerger;

    public BaseMergeOperation(@NotNull C target, @NotNull C source, @NotNull GenericItemMerger rootMerger) {
        this.target = target;
        this.source = source;
        this.rootMerger = rootMerger;
    }

    public void execute() throws ConfigurationException, SchemaException {
        rootMerger.mergeContainerValues(
                target.asPrismContainerValue(),
                source.asPrismContainerValue());
    }

    protected static boolean hasValue(PrismContainerValue<?> pcv, @NotNull ItemName itemName) {
        Item<?, ?> item = pcv.findItem(itemName);
        return item != null && item.hasAnyValue();
    }

    protected static PathKeyedMap<ItemMerger> createPathMap(Map<ItemPath, ItemMerger> sourceMap) {
        PathKeyedMap<ItemMerger> newMap = new PathKeyedMap<>();
        newMap.putAll(sourceMap);
        return newMap;
    }

    /**
     * A convenience method that merges two values without origin marking or custom config.
     * Use as a starting point when you just need to merge two {@link Containerable} values.
     */
    public static <C extends Containerable> C merge(@Nullable C target, @Nullable C source)
            throws SchemaException, ConfigurationException {
        if (target == null) {
            return source;
        } else if (source == null) {
            return target;
        } else {
            var clone = CloneUtil.cloneCloneable(target);
            new BaseMergeOperation<>(clone, source, new GenericItemMerger(null, createPathMap(Map.of())))
                    .execute();
            return clone;
        }
    }
}
