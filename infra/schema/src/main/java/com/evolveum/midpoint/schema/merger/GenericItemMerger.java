/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.schema.merger.key.NaturalKey;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.schema.merger.GenericItemMerger.Kind.CONTAINER;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * The generic item merger that follows these rules:
 *
 * 1. Matching property and reference values are overridden.
 * 2. Matching container values are merged recursively (using configured mergers for children).
 *
 * What are _matching_ values?
 *
 * 1. For single-valued items the values at source and target sides are automatically considered matching.
 * 2. For multi-valued items with a natural key defined, the values having the same key are considered matching.
 * 3. For multi-valued items without a natural key, no values are matching.
 */
public class GenericItemMerger implements ItemMerger {

    private static final Trace LOGGER = TraceManager.getTrace(GenericItemMerger.class);

    /** Natural key for the current item. If null, there's no natural key defined. */
    @Nullable private final NaturalKey naturalKey;

    /** Mergers to be used for child items. */
    @NotNull private final PathKeyedMap<ItemMerger> childrenMergers;

    public GenericItemMerger(
            @Nullable NaturalKey naturalKey,
            @NotNull PathKeyedMap<ItemMerger> childrenMergers) {
        this.naturalKey = naturalKey;
        this.childrenMergers = childrenMergers;
    }

    public GenericItemMerger(@NotNull PathKeyedMap<ItemMerger> childrenMergers) {
        this(null, childrenMergers);
    }

    @Override
    public void merge(@NotNull PrismValue target, @NotNull PrismValue source) throws ConfigurationException, SchemaException {
        argCheck(target instanceof PrismContainerValue, "Non-PCV values are not supported (yet): %s", target);
        argCheck(source instanceof PrismContainerValue, "Non-PCV values are not supported (yet): %s", source);

        PrismContainerValue<?> targetPcv = (PrismContainerValue<?>) target;
        PrismContainerValue<?> sourcePcv = (PrismContainerValue<?>) source;
        for (QName qName : determineItemNames(targetPcv, sourcePcv)) {
            LOGGER.trace("Merging {}", qName);
            ItemName itemName = ItemName.fromQName(qName);
            ItemMerger merger =
                    Objects.requireNonNullElseGet(
                            childrenMergers.get(itemName),
                            () -> createDefaultSubMerger(itemName));
            merger.merge(itemName, targetPcv, sourcePcv);
        }
    }

    private ItemMerger createDefaultSubMerger(ItemName itemName) {
        return new GenericItemMerger(
                createSubChildMergersMap(itemName));
    }

    private PathKeyedMap<ItemMerger> createSubChildMergersMap(@NotNull ItemName itemName) {
        PathKeyedMap<ItemMerger> childMap = new PathKeyedMap<>();
        for (Map.Entry<ItemPath, ItemMerger> entry : childrenMergers.entrySet()) {
            ItemPath path = entry.getKey();
            if (path.startsWith(itemName)) {
                childMap.put(path.rest(), entry.getValue());
            }
        }
        return childMap;
    }

    private Set<QName> determineItemNames(PrismContainerValue<?> target, PrismContainerValue<?> source) {
        Set<QName> itemNames = new HashSet<>();
        itemNames.addAll(target.getItemNames());
        itemNames.addAll(source.getItemNames());
        childrenMergers.keySet().stream()
                .filter(ItemPath::isSingleName)
                .forEach(path -> itemNames.add(path.asSingleNameOrFail()));
        return itemNames;
    }

    @Override
    public void merge(
            @NotNull ItemName itemName,
            @NotNull PrismContainerValue<?> target,
            @NotNull PrismContainerValue<?> source)
            throws ConfigurationException, SchemaException {
        LOGGER.trace("Merging item {}", itemName);
        Item<?, ?> sourceItem = source.findItem(itemName);
        if (sourceItem == null || sourceItem.hasNoValues()) {
            LOGGER.trace("Nothing found at source; keeping target unchanged");
        } else {
            Item<?, ?> targetItem = target.findItem(itemName);
            if (targetItem == null || targetItem.hasNoValues()) {
                LOGGER.trace("Nothing found at target; copying source value(s) to the target");
                //noinspection unchecked
                target.add(
                        sourceItem.clone());
            } else {
                boolean isTargetItemSingleValued = isSingleValued(targetItem);
                boolean isSourceItemSingleValued = isSingleValued(sourceItem);
                stateCheck(isSourceItemSingleValued == isTargetItemSingleValued,
                        "Mismatch between the cardinality of source and target items: single=%s (source) vs single=%s (target)",
                        isSourceItemSingleValued, isTargetItemSingleValued);
                if (isSourceItemSingleValued) {
                    mergeSingleValuedItem(targetItem, sourceItem);
                } else {
                    mergeMultiValuedItem(targetItem, sourceItem);
                }
            }
        }
        LOGGER.trace("Finished merging item {}", itemName);
    }

    private void mergeSingleValuedItem(Item<?, ?> targetItem, Item<?, ?> sourceItem)
            throws SchemaException, ConfigurationException {
        Kind kind = Kind.of(targetItem, sourceItem);
        PrismValue targetValue = getSingleValue(targetItem);
        PrismValue sourceValue = getSingleValue(sourceItem);
        if (kind == CONTAINER) {
            LOGGER.trace("Merging matching container (single) values");
            merge(targetValue, sourceValue);
        } else {
            LOGGER.trace("Overriding non-container (single) value - i.e. keeping target item as is");
        }
    }

    private @NotNull PrismValue getSingleValue(Item<?, ?> item) {
        stateCheck(item.size() == 1, "Single-valued non-empty item with %s values: %s", item.size(), item);
        return item.getValues().get(0);
    }

    private void mergeMultiValuedItem(Item<?, ?> targetItem, Item<?, ?> sourceItem)
            throws SchemaException, ConfigurationException {
        Kind kind = Kind.of(targetItem, sourceItem);
        for (PrismValue sourceValue : sourceItem.getValues()) {
            LOGGER.trace("Going to merge source value: {}", sourceValue);
            if (kind == CONTAINER) {
                PrismContainerValue<?> sourcePcv = (PrismContainerValue<?>) sourceValue;
                PrismContainerValue<?> matchingTargetValue =
                        findMatchingTargetValue((PrismContainer<?>) targetItem, sourcePcv);
                if (matchingTargetValue != null) {
                    LOGGER.trace("Merging into matching target value: {}", matchingTargetValue);
                    mergeKey(matchingTargetValue, sourcePcv);
                    merge(matchingTargetValue, sourcePcv);
                } else {
                    LOGGER.trace("Adding to the target item (without ID)");
                    PrismContainerValue<?> sourcePcvClone = sourcePcv.clone();
                    sourcePcvClone.setId(null);
                    //noinspection unchecked
                    ((Item<PrismValue, ?>) targetItem).add(sourcePcvClone);
                }
            } else {
                LOGGER.trace("Adding to the target item");
                //noinspection unchecked
                ((Item<PrismValue, ?>) targetItem).add(
                        sourceValue.clone());
            }
        }
    }

    private PrismContainerValue<?> findMatchingTargetValue(
            PrismContainer<?> targetItem, PrismContainerValue<?> sourceValue) throws ConfigurationException {
        if (naturalKey == null) {
            return null;
        }
        for (PrismContainerValue<?> targetValue : targetItem.getValues()) {
            if (naturalKey.valuesMatch(targetValue, sourceValue)) {
                return targetValue;
            }
        }
        return null;
    }

    private void mergeKey(PrismContainerValue<?> matchingTargetValue, PrismContainerValue<?> sourcePcv)
            throws ConfigurationException {
        if (naturalKey != null) {
            naturalKey.mergeMatchingKeys(matchingTargetValue, sourcePcv);
        }
    }

    private boolean isSingleValued(@NotNull Item<?, ?> item) throws SchemaException {
        ItemDefinition<?> definition =
                MiscUtil.requireNonNull(
                        item.getDefinition(),
                        () -> "Cannot merge item without definition: " + item);
        return definition.isSingleValue();
    }

    enum Kind {
        PROPERTY, REFERENCE, CONTAINER;

        static @NotNull Kind of(@NotNull Item<?, ?> item) {
            if (item instanceof PrismProperty<?>) {
                return PROPERTY;
            } else if (item instanceof PrismReference) {
                return REFERENCE;
            } else if (item instanceof PrismContainer<?>) {
                return CONTAINER;
            } else {
                throw new IllegalArgumentException("Unknown item type: " + MiscUtil.getValueWithClass(item));
            }
        }

        static @NotNull Kind of(@NotNull Item<?, ?> targetItem, @NotNull Item<?, ?> sourceItem) {
            Kind target = of(targetItem);
            Kind source = of(sourceItem);
            argCheck(source == target,
                    "Mismatching kinds for target (%s) and source (%s) items: %s, %s",
                    target, source, targetItem, sourceItem);
            return target;
        }
    }
}
