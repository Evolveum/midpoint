/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The target for the inbounds processing i.e. the object (focus, assignment, ...) into which the output of mappings will be put.
 *
 * @param <T> type of the object
 */
public abstract class InboundsTarget<T extends Containerable> {

    /**
     * Current target object.
     *
     * - For pre-mappings this will be the empty object.
     * - For full processing, this will always be the target focus object (the "new" version).
     */
    @Nullable final PrismContainerValue<T> targetPcv;

    /** Focus definition. It should be a CTD instead, but the mapping evaluation expects PCD for now. */
    @NotNull final PrismContainerDefinition<T> targetDefinition;

    @NotNull final PathKeyedMap<ItemDefinition<?>> itemDefinitionMap;

    /** Relative path of the default `$target` variable. TODO */
    @NotNull private final ItemPath targetPathPrefix;

    InboundsTarget(
            @Nullable PrismContainerValue<T> targetPcv,
            @NotNull PrismContainerDefinition<T> targetDefinition,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap,
            @NotNull ItemPath targetPathPrefix) {
        this.targetPcv = targetPcv;
        this.targetDefinition = targetDefinition;
        this.itemDefinitionMap = itemDefinitionMap;
        this.targetPathPrefix = targetPathPrefix;
    }

    @Nullable
    public T getTargetRealValue() {
        return targetPcv != null ? targetPcv.asContainerable() : null;
    }

    @NotNull ItemPath getTargetPathPrefix() {
        return targetPathPrefix;
    }

    /**
     * Returns true if the focus object is being deleted. Not applicable to pre-mappings.
     */
    abstract boolean isFocusBeingDeleted();

    void addItemDefinition(ItemPath itemPath, ItemDefinition<?> itemDefinition) {
        itemDefinitionMap.put(
                itemPath.stripVariableSegment(),
                itemDefinition);
    }
}
