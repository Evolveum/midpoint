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

public class TreeReferenceDelta
        extends TreeItemDelta<PrismReferenceValue, PrismReferenceDefinition, PrismReference, TreeReferenceDeltaValue> {

    public TreeReferenceDelta(@NotNull PrismReferenceDefinition definition) {
        super(definition);
    }

    public static TreeReferenceDelta from(ReferenceDelta delta) {
        TreeReferenceDelta result = new TreeReferenceDelta(delta.getDefinition());

        result.addDeltaValues(delta.getValuesToAdd(), ModificationType.ADD, TreeReferenceDeltaValue::from);
        result.addDeltaValues(delta.getValuesToReplace(), ModificationType.REPLACE, TreeReferenceDeltaValue::from);
        result.addDeltaValues(delta.getValuesToDelete(), ModificationType.DELETE, TreeReferenceDeltaValue::from);

        return result;
    }
}
