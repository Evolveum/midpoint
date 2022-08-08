/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import com.evolveum.midpoint.model.api.identities.IndexingItemConfiguration;
import com.evolveum.midpoint.model.api.identities.Normalization;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.model.impl.lens.identities.IndexingManager.normalizeValue;

class Normalizer {

    private static final Trace LOGGER = TraceManager.getTrace(Normalizer.class);

    @NotNull private final PrismContext prismContext = PrismContext.get();
    @NotNull private final Normalization normalization;

    Normalizer(@NotNull Normalization normalization) {
        this.normalization = normalization;
    }

    Item<?, ?> createNormalizedItem(
            @NotNull ItemDefinition<?> originalItemDef,
            @NotNull Collection<PrismValue> originalValues) {
        PrismProperty<String> normalizedItem =
                prismContext.itemFactory().createProperty(
                        normalization.getIndexItemName(),
                        normalization.getIndexItemDefinition());
        for (PrismValue originalValue : originalValues) {
            Object originalRealValue = originalValue.getRealValue();
            if (originalRealValue != null) {
                normalizedItem.addRealValue(
                        normalizeValue(originalRealValue, normalization));
            } else {
                LOGGER.warn("No real value in {} in {}", originalValue, originalItemDef);
            }
        }
        return normalizedItem;
    }
}
