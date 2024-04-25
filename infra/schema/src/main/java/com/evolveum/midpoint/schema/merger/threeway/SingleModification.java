/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.threeway;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;

/**
 * TODO DOC
 */
public record SingleModification(@NotNull PrismValue value, @NotNull ModificationType type, @NotNull ItemDelta<?, ?> source) {

    @NotNull ItemDelta<?, ?> toSingleItemDelta() {
        return createSingleItemDelta(source, value, type);
    }

    public static ItemDelta<?, ?> createSingleItemDelta(
            ItemDelta<?, ?> fullItemDelta, PrismValue value, ModificationType modificationType) {

        ItemDelta delta = fullItemDelta.clone();
        delta.clear();

        switch (modificationType) {
            case ADD -> delta.addValueToAdd(value.clone());
            case DELETE -> delta.addValueToDelete(value.clone());
            case REPLACE -> delta.addValueToReplace(value.clone());
        }

        return delta;
    }
}
