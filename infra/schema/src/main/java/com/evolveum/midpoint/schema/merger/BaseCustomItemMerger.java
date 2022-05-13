/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * Most probably temporary code.
 */
public abstract class BaseCustomItemMerger<T extends Item<?, ?>> implements ItemMerger {

    @Override
    public void merge(@NotNull PrismValue target, @NotNull PrismValue source) {
        throw new UnsupportedOperationException(); // TODO reconsider this method
    }

    @Override
    public void merge(
            @NotNull ItemName itemName,
            @NotNull PrismContainerValue<?> targetParent,
            @NotNull PrismContainerValue<?> sourceParent) throws ConfigurationException, SchemaException {
        //noinspection unchecked
        T targetItem = (T) targetParent.findItem(itemName);
        //noinspection unchecked
        T sourceItem = (T) sourceParent.findItem(itemName);
        // some shortcuts first
        if (sourceItem == null || sourceItem.hasNoValues()) {
            // nothing to do here
        } else if (targetItem == null) {
            //noinspection unchecked
            targetParent.add(sourceItem.clone());
        } else if (targetItem.hasNoValues()) {
            for (PrismValue clonedSourceItemValue : sourceItem.getClonedValues()) {
                //noinspection unchecked,rawtypes
                ((Item) targetItem).add(clonedSourceItemValue);
            }
        } else {
            mergeInternal(targetItem, sourceItem);
        }
    }

    protected abstract void mergeInternal(@NotNull T target, @NotNull T source)
            throws ConfigurationException, SchemaException;
}
