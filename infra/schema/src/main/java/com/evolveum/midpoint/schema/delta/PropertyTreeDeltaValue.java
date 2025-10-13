/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.delta;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismPropertyValue;

public class PropertyTreeDeltaValue<T> extends ItemTreeDeltaValue<PrismPropertyValue<T>, PropertyTreeDelta<T>> {

    public PropertyTreeDeltaValue() {
        this(null, null);
    }

    public PropertyTreeDeltaValue(PrismPropertyValue<T> value, ModificationType modificationType) {
        super(value, modificationType);
    }

    @Override
    protected String debugDumpShortName() {
        return "PTDV";
    }
}
