/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Target in the "full mode". It is always the focal object, e.g., a user.
 */
public class FullInboundsTarget<F extends FocusType> extends InboundsTarget<F> {

    @NotNull private final LensContext<F> lensContext;

    public FullInboundsTarget(
            @NotNull LensContext<F> lensContext,
            @Nullable PrismObject<F> focus,
            @NotNull PrismObjectDefinition<F> focusDefinition,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap,
            @NotNull ItemPath targetPathPrefix) {
        super(focus.getValue(), focusDefinition, itemDefinitionMap, targetPathPrefix);
        this.lensContext = lensContext;
    }

    @Override
    boolean isFocusBeingDeleted() {
        return lensContext.getFocusContext().isDelete();
    }

    FullInboundsTarget<F> withTargetPathPrefix(ItemPath targetPathPrefix) {
        //noinspection unchecked
        return new FullInboundsTarget<>(
                lensContext,
                (PrismObject<F>) targetPcv.asContainerable().asPrismObject(),
                (PrismObjectDefinition<F>) targetDefinition,
                itemDefinitionMap,
                targetPathPrefix);
    }
}
