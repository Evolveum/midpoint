/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;

public class ReferenceTreeDelta
        extends ItemTreeDelta<PrismReferenceValue, PrismReferenceDefinition, PrismReference, ReferenceTreeDeltaValue> {

    public ReferenceTreeDelta(@NotNull PrismReferenceDefinition definition) {
        super(definition);
    }

    @Override
    protected String debugDumpShortName() {
        return "RTD";
    }

    public static ReferenceTreeDelta from(ReferenceDelta delta) {
        ReferenceTreeDelta result = new ReferenceTreeDelta(delta.getDefinition());

        result.addDeltaValues(delta.getValuesToAdd(), ModificationType.ADD, ReferenceTreeDeltaValue::from);
        result.addDeltaValues(delta.getValuesToReplace(), ModificationType.REPLACE, ReferenceTreeDeltaValue::from);
        result.addDeltaValues(delta.getValuesToDelete(), ModificationType.DELETE, ReferenceTreeDeltaValue::from);

        return result;
    }
}
