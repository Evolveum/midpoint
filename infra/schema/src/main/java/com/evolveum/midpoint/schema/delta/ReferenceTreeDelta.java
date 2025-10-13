/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.delta;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;

public class ReferenceTreeDelta
        extends ItemTreeDelta<PrismReferenceValue, PrismReferenceDefinition, PrismReference, ReferenceTreeDeltaValue> {

    public ReferenceTreeDelta(@NotNull PrismReferenceDefinition definition) {
        super(definition);
    }

    @Override
    protected String debugDumpShortName() {
        return "RTD";
    }

    @Override
    public ReferenceTreeDeltaValue createNewValue() {
        return new ReferenceTreeDeltaValue();
    }
}
