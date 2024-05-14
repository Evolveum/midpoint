/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
