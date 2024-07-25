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

public class LimitedInboundsTarget<T extends Containerable> extends InboundsTarget<T> {

    public LimitedInboundsTarget(
            @NotNull PrismContainerValue<T> target,
            @NotNull PrismContainerDefinition<T> targetDefinition,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap) {
        super(target, targetDefinition, itemDefinitionMap, ItemPath.EMPTY_PATH);
    }

    @Override
    boolean isFocusBeingDeleted() {
        return false; // No focus yet
    }
}
