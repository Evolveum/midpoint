/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
