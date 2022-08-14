/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.identities;

import com.evolveum.midpoint.model.api.indexing.Normalization;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.evolveum.midpoint.model.impl.lens.identities.IndexingManager.normalizeValue;

/**
 * TODO any other responsibilities?
 */
class ItemNormalizer {

    private static final Trace LOGGER = TraceManager.getTrace(ItemNormalizer.class);

    @NotNull private final PrismContext prismContext = PrismContext.get();
    @NotNull private final Normalization normalization;

    ItemNormalizer(@NotNull Normalization normalization) {
        this.normalization = normalization;
    }

    Item<?, ?> createNormalizedItem(
            @NotNull ItemDefinition<?> originalItemDef,
            @NotNull Collection<PrismValue> originalValues,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        PrismProperty<String> normalizedItem =
                prismContext.itemFactory().createProperty(
                        normalization.getIndexItemName(),
                        normalization.getIndexItemDefinition());
        for (PrismValue originalValue : originalValues) {
            Object originalRealValue = originalValue.getRealValue();
            if (originalRealValue != null) {
                normalizedItem.addRealValue(
                        normalizeValue(originalRealValue, normalization, task, result));
            } else {
                LOGGER.warn("No real value in {} in {}", originalValue, originalItemDef);
            }
        }
        return normalizedItem;
    }
}
