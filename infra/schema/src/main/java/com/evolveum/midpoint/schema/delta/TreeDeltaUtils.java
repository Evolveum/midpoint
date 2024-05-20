/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.delta;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;

public class TreeDeltaUtils {

    public static void populateItemDelta(@NotNull ItemDelta delta, @NotNull ItemTreeDeltaValue value) {
        ModificationType modificationType = value.getModificationType();
        PrismValue prismValue = value.getValue();
        if (modificationType == null || prismValue == null) {
            return;
        }

        PrismValue cloned = prismValue.clone();

        switch (modificationType) {
            case ADD -> delta.addValueToAdd(cloned);
            case DELETE -> delta.addValueToDelete(cloned);
            case REPLACE -> delta.addValueToReplace(cloned);
        }
    }
}
