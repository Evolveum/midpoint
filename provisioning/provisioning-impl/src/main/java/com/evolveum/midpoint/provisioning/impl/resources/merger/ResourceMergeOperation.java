/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resources.merger;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Implements the actual merging of two configuration objects of the same type.
 *
 * Currently hard-wired to objects of {@link ResourceType}. In the future, we may generalize it to handle any suitable objects.
 */
public class ResourceMergeOperation {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceMergeOperation.class);

    @NotNull private final ResourceType target;
    @NotNull private final ResourceType source;

    @NotNull private final ItemMerger rootMerger = new GenericItemMerger(
            null,
            createPathMap(
                    Map.of(
                            ResourceType.F_NAME, RequiredItemMerger.INSTANCE
                            // connectorRef, connectorConfiguration - default
                            // TODO other ones
                    )));

    public ResourceMergeOperation(
            @NotNull ResourceType target,
            @NotNull ResourceType source) {
        this.target = target;
        this.source = source;
    }

    public void execute() throws ConfigurationException, SchemaException {
        rootMerger.merge(target.asPrismContainerValue(), source.asPrismContainerValue());
    }

    static boolean hasValue(PrismContainerValue<?> pcv, @NotNull ItemName itemName) {
        Item<?, ?> item = pcv.findItem(itemName);
        return item != null && item.hasAnyValue();
    }

//    void copyAllValuesOf(@NotNull ItemName itemName) throws SchemaException {
//        Collection<? extends PrismValue> sourceItemValues = getSourceItemValues(itemName);
//        if (sourceItemValues.isEmpty()) {
//            LOGGER.trace("Would copy values of {} but there are none", itemName);
//        } else {
//            LOGGER.trace("Copying all {} value(s) of {}", sourceItemValues.size(), itemName);
//            //noinspection unchecked
//            target.asPrismContainerValue().findOrCreateItem(itemName)
//                    .addAll(CloneUtil.cloneCollectionMembersWithoutIds(sourceItemValues));
//        }
//    }

    private Collection<? extends PrismValue> getSourceItemValues(@NotNull ItemName itemName) {
        Item<?, ?> item = source.asPrismContainerValue().findItem(itemName);
        return item != null ? item.getValues() : List.of();
    }

    private static PathKeyedMap<ItemMerger> createPathMap(Map<ItemPath, ItemMerger> sourceMap) {
        PathKeyedMap<ItemMerger> newMap = new PathKeyedMap<>();
        newMap.putAll(sourceMap);
        return newMap;
    }
}
