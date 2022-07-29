/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * The target i.e. the focus into which the output of mappings will be put.
 *
 * @param <F> type of the object
 */
abstract class Target<F extends FocusType> {

    /**
     * Current focus.
     *
     * - For pre-mappings this will be the empty object.
     * - TODO for clockwork: should we use current or new?
     */
    final PrismObject<F> focus;

    /** Focus definition. */
    @NotNull final PrismObjectDefinition<F> focusDefinition;

    @NotNull private final PathKeyedMap<ItemDefinition<?>> itemDefinitionMap;

    Target(
            PrismObject<F> focus,
            @NotNull PrismObjectDefinition<F> focusDefinition,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap) {
        this.focus = focus;
        this.focusDefinition = focusDefinition;
        this.itemDefinitionMap = itemDefinitionMap;
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
