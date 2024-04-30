/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismReferenceValue;

public class TreeReferenceDeltaValue extends TreeItemDeltaValue<PrismReferenceValue> {

    public TreeReferenceDeltaValue(PrismReferenceValue value, ModificationType modificationType) {
        super(value, modificationType);
    }

    public static TreeReferenceDeltaValue from(PrismReferenceValue value, ModificationType modificationType) {
        return new TreeReferenceDeltaValue(value, modificationType);
    }
}
