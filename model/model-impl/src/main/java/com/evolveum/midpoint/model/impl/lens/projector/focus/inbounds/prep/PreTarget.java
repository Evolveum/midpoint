/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds.prep;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.PathKeyedMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.jetbrains.annotations.NotNull;

class PreTarget<F extends FocusType> extends Target<F> {

    PreTarget(
            PrismObject<F> focus,
            @NotNull PrismObjectDefinition<F> focusDefinition,
            @NotNull PathKeyedMap<ItemDefinition<?>> itemDefinitionMap) {
        super(focus, focusDefinition, itemDefinitionMap);
    }

    @Override
    boolean isFocusBeingDeleted() {
        return false; // No focus yet
    }
}
