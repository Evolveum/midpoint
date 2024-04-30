/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway.item;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;

public class TreePropertyDelta<T>
        extends TreeItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>, PrismProperty<T>, TreePropertyDeltaValue<T>> {

    public TreePropertyDelta(@NotNull PrismPropertyDefinition<T> definition) {
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
