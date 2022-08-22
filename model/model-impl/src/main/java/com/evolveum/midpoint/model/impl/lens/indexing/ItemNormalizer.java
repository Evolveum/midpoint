/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.indexing;

import com.evolveum.midpoint.model.api.indexing.IndexedItemValueNormalizer;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.model.impl.lens.indexing.IndexingManager.normalizeValue;

/**
 * Normalizes a given item using provided {@link IndexedItemValueNormalizer}.
 *
 * TODO decide what to do with this class
 */
class ItemNormalizer {

    private static final Trace LOGGER = TraceManager.getTrace(ItemNormalizer.class);

    @NotNull private final PrismContext prismContext = PrismContext.get();
    @NotNull private final IndexedItemValueNormalizer valueNormalizer;

    ItemNormalizer(@NotNull IndexedItemValueNormalizer valueNormalizer) {
        this.valueNormalizer = valueNormalizer;
    }

    Item<?, ?> createNormalizedItem(
            @NotNull ItemDefinition<?> originalItemDef,
            @NotNull Collection<PrismValue> originalValues,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        //noinspection unchecked
        PrismProperty<String> normalizedItem =
                prismContext.itemFactory().createProperty(
                        valueNormalizer.getIndexItemName(),
                        (PrismPropertyDefinition<String>) valueNormalizer.getIndexItemDefinition());
        for (PrismValue originalValue : originalValues) {
            Object originalRealValue = originalValue.getRealValue();
            if (originalRealValue != null) {
                normalizedItem.addRealValue(
                        normalizeValue(originalRealValue, valueNormalizer, task, result));
            } else {
                LOGGER.warn("No real value in {} in {}", originalValue, originalItemDef);
            }
        }
        return normalizedItem;
    }
}
