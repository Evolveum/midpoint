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
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides common functionality for the majority for (non-trivial) item mergers.
 */
public abstract class BaseItemMerger<T extends Item<?, ?>> implements ItemMerger {

    /** We ignore value metadata when comparing (because inherited values do have them) */
    @NotNull public static final ParameterizedEquivalenceStrategy VALUE_COMPARISON_STRATEGY =
            EquivalenceStrategy.DATA.exceptForValueMetadata();

    private static final Trace LOGGER = TraceManager.getTrace(BaseItemMerger.class);

    /** Marks any values inherited from "source" to "target" with appropriate (presumably source-related) origin. */
    @Nullable protected final OriginMarker originMarker;

    protected BaseItemMerger(@Nullable OriginMarker originMarker) {
        this.originMarker = originMarker;
    }

    @Override
    public void merge(
            @NotNull ItemName itemName,
            @NotNull PrismContainerValue<?> targetParent,
            @NotNull PrismContainerValue<?> sourceParent) throws ConfigurationException, SchemaException {

        LOGGER.trace("Merging item {}", itemName);
        //noinspection unchecked
        T targetItem = (T) targetParent.findItem(itemName);
        //noinspection unchecked
        T sourceItem = (T) sourceParent.findItem(itemName);
        // some shortcuts first
        if (sourceItem == null || sourceItem.hasNoValues()) {
            LOGGER.trace(" -> Nothing found at source; keeping target unchanged");
        } else if (targetItem == null) {
            LOGGER.trace(" -> Target item not found; copying source item to the target");
            targetParent.add(
                    createMarkedClone(sourceItem));
        } else if (targetItem.hasNoValues()) {
            LOGGER.trace(" -> Target item found empty; copying source item value(s) to the target");
            for (PrismValue sourceValue : sourceItem.getValues()) {
                //noinspection unchecked,rawtypes
                ((Item) targetItem).add(
                        createMarkedClone(sourceValue));
            }
        } else {
            LOGGER.trace(" -> Combining {} from source and target", itemName);
            mergeInternal(targetItem, sourceItem);
        }
        LOGGER.trace("Finished merging item {}", itemName);
    }

    private Item<?, ?> createMarkedClone(Item<?, ?> sourceItem) throws SchemaException {
        Item<?, ?> clone = sourceItem.clone();
        if (originMarker != null) {
            for (PrismValue value : clone.getValues()) {
                originMarker.mark(value);
            }
        }
        return clone;
    }

    <PV extends PrismValue> PV createMarkedClone(PV sourceValue) throws SchemaException {
        //noinspection unchecked
        PV clone = (PV) sourceValue.clone();
        if (clone instanceof PrismContainerValue<?>) {
            ((PrismContainerValue<?>) clone).setId(null);
        }
        if (originMarker != null) {
            originMarker.mark(clone);
        }
        return clone;
    }

    protected <C extends Containerable> C createMarkedClone(C sourceValue) throws SchemaException {
        //noinspection unchecked
        return (C) createMarkedClone(sourceValue.asPrismContainerValue())
                .asContainerable();
    }

    protected abstract void mergeInternal(@NotNull T target, @NotNull T source)
            throws ConfigurationException, SchemaException;
}
