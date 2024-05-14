/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
