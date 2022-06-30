/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.key;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;

import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.schema.merger.BaseItemMerger;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * A {@link NaturalKey} implementation that uses a simple list of constituent items to compare container values.
 *
 * No specific key merging is provided.
 */
public class DefaultNaturalKeyImpl implements NaturalKey {

    /** Items that constitute the natural key. */
    @NotNull private final Collection<QName> constituents;

    private DefaultNaturalKeyImpl(@NotNull Collection<QName> constituents) {
        this.constituents = constituents;
    }

    public static DefaultNaturalKeyImpl of(QName... constituents) {
        return new DefaultNaturalKeyImpl(List.of(constituents));
    }

    @Override
    public boolean valuesMatch(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue) {
        for (QName keyConstituent : constituents) {
            Item<?, ?> targetKeyItem = targetValue.findItem(ItemName.fromQName(keyConstituent));
            Item<?, ?> sourceKeyItem = sourceValue.findItem(ItemName.fromQName(keyConstituent));
            if (areNotEquivalent(targetKeyItem, sourceKeyItem)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void mergeMatchingKeys(PrismContainerValue<?> targetValue, PrismContainerValue<?> sourceValue) {
        // No-op in this general case
    }

    private boolean areNotEquivalent(Item<?, ?> targetKeyItem, Item<?, ?> sourceKeyItem) {
        if (targetKeyItem != null && targetKeyItem.hasAnyValue()) {
            return !targetKeyItem.equals(sourceKeyItem, BaseItemMerger.VALUE_COMPARISON_STRATEGY);
        } else {
            return sourceKeyItem != null && sourceKeyItem.hasAnyValue();
        }
    }
}
