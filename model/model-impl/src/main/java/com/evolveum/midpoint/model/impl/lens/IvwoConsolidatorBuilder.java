/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.impl.lens.projector.ValueMatcher;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Comparator;

public final class IvwoConsolidatorBuilder<V extends PrismValue, D extends ItemDefinition, I extends ItemValueWithOrigin<V, D>> {

    ItemPath itemPath;
    DeltaSetTriple<I> ivwoTriple;
    D itemDefinition;
    ItemDelta<V, D> aprioriItemDelta;
    PrismContainer<?> itemContainer;
    Item<V,D> existingItem; // alternative to using itemContainer
    ValueMatcher valueMatcher;
    Comparator<V> comparator;
    boolean addUnchangedValues;
    boolean existingItemKnown;
    boolean addUnchangedValuesExceptForNormalMappings;
    boolean isExclusiveStrong;
    String contextDescription;
    StrengthSelector strengthSelector;

    public IvwoConsolidatorBuilder<V, D, I> itemPath(ItemPath val) {
        itemPath = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> ivwoTriple(DeltaSetTriple<I> val) {
        ivwoTriple = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> itemDefinition(D val) {
        itemDefinition = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> aprioriItemDelta(ItemDelta<V, D> val) {
        aprioriItemDelta = val;
        return this;
    }

    // Alternative to existingItem
    public IvwoConsolidatorBuilder<V, D, I> itemContainer(PrismContainer<?> val) {
        itemContainer = val;
        return this;
    }

    // Alternative to itemContainer
    public IvwoConsolidatorBuilder<V, D, I> existingItem(Item<V, D> val) {
        existingItem = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> valueMatcher(ValueMatcher val) {
        valueMatcher = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> comparator(Comparator<V> val) {
        comparator = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> addUnchangedValues(boolean val) {
        addUnchangedValues = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> existingItemKnown(boolean val) {
        existingItemKnown = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> addUnchangedValuesExceptForNormalMappings(boolean val) {
        addUnchangedValuesExceptForNormalMappings = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> isExclusiveStrong(boolean val) {
        isExclusiveStrong = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> contextDescription(String val) {
        contextDescription = val;
        return this;
    }

    public IvwoConsolidatorBuilder<V, D, I> strengthSelector(StrengthSelector val) {
        strengthSelector = val;
        return this;
    }

    public IvwoConsolidator<V, D, I> build() throws SchemaException {
        return new IvwoConsolidator<>(this);
    }
}
