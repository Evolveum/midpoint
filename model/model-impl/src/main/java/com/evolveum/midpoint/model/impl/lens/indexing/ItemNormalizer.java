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
import java.util.HashSet;
import java.util.Set;

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

        // This intermediate set is here to avoid "Adding value to property XXX that already exists" warnings.
        Set<String> normalizedValues = new HashSet<>();
        for (PrismValue originalValue : originalValues) {
            Object originalRealValue = originalValue.getRealValue();
            if (originalRealValue != null) {
                normalizedValues.add(
                        valueNormalizer.normalize(originalRealValue, task, result));
            } else {
                LOGGER.warn("No real value in {} in {}", originalValue, originalItemDef);
            }
        }

        normalizedValues.forEach(normalizedItem::addRealValue);
        return normalizedItem;
    }
}
