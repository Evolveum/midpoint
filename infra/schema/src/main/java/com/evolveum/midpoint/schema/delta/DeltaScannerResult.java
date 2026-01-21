/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.delta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public record DeltaScannerResult(

        /**
         * Object type class defined in object delta.
         */
        @NotNull Class<? extends ObjectType> objectType,

        /**
         * Root delta that contains prism values matching the search.
         */
        @NotNull ItemDelta<?, ?> delta,

        /**
         * Full exact item path from the root to the value(s) that matched the search.
         */
        @NotNull ItemPath fullPath,

        /**
         * Values that matched the search.
         */
        @NotNull Map<ModificationType, List<? extends PrismValue>> values,

        /**
         * Estimated old values that matched the search, if available.
         */
        Collection<? extends PrismValue> estimatedOldValues) {

    public boolean isPartial() {
        return !delta.getPath().equivalent(fullPath());
    }

    public @NotNull ItemDelta<?, ?> toDelta() {
        ItemDelta<?, ?> itemDelta = delta();

        if (!fullPath().equivalent(itemDelta.getPath())) {
            itemDelta = createItemDelta();
        }

        return itemDelta;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ItemDelta<?, ?> createItemDelta() {
        ItemPath namedPath = fullPath().namedSegmentsOnly();

        ItemDefinition<?> def = PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(objectType)
                .findItemDefinition(namedPath);

        ItemDelta delta = def.createEmptyDelta(fullPath());

        values().forEach((modification, values) -> {
            values.forEach(v -> delta.addValue(modification, v.clone()));
        });

        if (estimatedOldValues() != null) {
            List oldValues = new ArrayList();
            estimatedOldValues().forEach(v -> oldValues.add(v.clone()));
            delta.setEstimatedOldValues(oldValues);
        }

        return delta;
    }
}
