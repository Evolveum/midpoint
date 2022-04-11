/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.route;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * TEMPORARY
 */
public class ItemRouteResolver {

    private List<PrismValue> currentValues;
    @NotNull private final ItemRoute route;
    private int currentSegmentNumber;
    private ItemRouteSegment currentSegment;

    private ItemRouteResolver(Containerable object, @NotNull ItemRoute route) {
        this.currentValues = List.of(object.asPrismContainerValue());
        this.route = route;
    }

    public static @NotNull List<PrismValue> resolve(@Nullable Containerable containerable, @NotNull ItemRoute route) throws SchemaException {
        if (containerable == null) {
            return List.of();
        } else if (route.isEmpty()) {
            return List.of(containerable.asPrismContainerValue());
        } else {
            return new ItemRouteResolver(containerable, route)
                    .resolve();
        }
    }

    private List<PrismValue> resolve() throws SchemaException {
        for (;;) {
            if (currentValues == null || currentSegmentNumber >= route.size()) {
                return currentValues;
            }
            currentSegment = route.get(currentSegmentNumber);

            List<PrismValue> valuesForPath = resolvePath(currentValues);
            currentValues = currentSegment.filter(valuesForPath);
            currentSegmentNumber++;
        }
    }

    private List<PrismValue> resolvePath(@NotNull List<PrismValue> currentValues) {
        ItemPath path = currentSegment.path;
        if (currentValues.isEmpty()) {
            return List.of();
        } else {
            List<PrismValue> resolvedValues = new ArrayList<>();
            for (Object current : currentValues) {
                if (current instanceof PrismContainerValue<?>) {
                    resolvedValues.addAll(
                            ((PrismContainerValue<?>) current).getAllValues(path));
                } else {
                    throw new IllegalStateException("Cannot find '" + path + "' in '" + current + "'");
                }
            }
            return resolvedValues;
        }
    }
}
