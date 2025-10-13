/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.delta;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;

public class PropertyTreeDelta<T>
        extends ItemTreeDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>, PrismProperty<T>, PropertyTreeDeltaValue<T>> {

    public PropertyTreeDelta(@NotNull PrismPropertyDefinition<T> definition) {
        super(definition);
    }

    @Override
    protected String debugDumpShortName() {
        return "PTD";
    }

    @Override
    public PropertyTreeDeltaValue<T> createNewValue() {
        return new PropertyTreeDeltaValue<>();
    }
}
