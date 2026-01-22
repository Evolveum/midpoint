/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.delta;

import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;

public class DeltaScanner {

    public List<DeltaScannerResult> searchDelta(@NotNull ObjectDelta<?> delta, @NotNull ItemPath templatePath) {
        List<DeltaScannerResult> result = new ArrayList<>();

        PrismObject<?> object = delta.getObjectToAdd();
        if (object != null) {
            Collection<Item<?, ?>> items = object.getAllItems(templatePath);

            for (Item<?, ?> item : items) {
                ItemDelta itemDelta = item.createDelta();
                item.getValues().forEach(v -> {
                    itemDelta.addValueToAdd(v.clone());
                });

                result.add(
                        new DeltaScannerResult(
                                (Class) delta.getObjectTypeClass(),
                                itemDelta,
                                item.getPath(),
                                Map.of(ModificationType.ADD, item.getValues()),
                                null));
            }

            return result;
        }

        Collection<? extends ItemDelta<?, ?>> modifications = delta.getModifications();
        for (ItemDelta<?, ?> itemDelta : modifications) {
            ItemPath path = itemDelta.getPath();
            ItemPath namedOnly = path.namedSegmentsOnly();

            if (namedOnly.equivalent(templatePath)) {
                result.add(new DeltaScannerResult(
                        (Class) delta.getObjectTypeClass(),
                        itemDelta,
                        path,
                        createModificationMap(itemDelta),
                        itemDelta.getEstimatedOldValues()));

            } else if (namedOnly.isSubPath(templatePath)) {
                ItemPath remainder = templatePath.remainder(namedOnly);

                Collection<? extends PrismValue> deltaEstimatedOldValues = itemDelta.getEstimatedOldValues();
                Collection<? extends PrismValue> estimatedOldValues = new ArrayList<>();
                if (deltaEstimatedOldValues != null) {
                    for (PrismValue value : deltaEstimatedOldValues) {
                        Collection<Item<?, ?>> items = value.getAllItems(remainder);
                        if (!items.isEmpty()) {
                            for (Item<? extends PrismValue, ?> item : items) {
                                estimatedOldValues.addAll((List) item.getValues());
                            }
                        }
                    }
                }

                // we have to go deeper into delta values
                for (ModificationType type : ModificationType.values()) {
                    Collection<? extends PrismValue> values = itemDelta.getValues(type);
                    if (values != null) {
                        for (PrismValue value : values) {
                            Collection<Item<?, ?>> items = value.getAllItems(remainder);

                            for (Item<?, ?> item : items) {
                                result.add(
                                        new DeltaScannerResult(
                                                (Class) delta.getObjectTypeClass(),
                                                itemDelta,
                                                item.getPath(),
                                                Map.of(type, item.getValues()),
                                                estimatedOldValues));
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private Map<ModificationType, List<? extends PrismValue>> createModificationMap(ItemDelta<?, ?> delta) {
        Map<ModificationType, List<? extends PrismValue>> result = new HashMap<>();

        for (ModificationType type : ModificationType.values()) {
            Collection<? extends PrismValue> values = delta.getValues(type);
            if (values != null && !values.isEmpty()) {
                result.put(type, new ArrayList<>(values));
            }
        }

        return result;
    }
}
