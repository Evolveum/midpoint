/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.delta;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismReferenceValue;

public class ReferenceTreeDeltaValue extends ItemTreeDeltaValue<PrismReferenceValue, ReferenceTreeDelta> {

    public ReferenceTreeDeltaValue() {
        this(null, null);
    }

    public ReferenceTreeDeltaValue(PrismReferenceValue value, ModificationType modificationType) {
        super(value, modificationType);
    }

    @Override
    protected String debugDumpShortName() {
        return "RTDV";
    }
}
