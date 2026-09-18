/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;

public interface PropertiesFormatter<T extends VisualizationItem> {
    default String formatProperties(Collection<T> items, int nestingLevel) {
        return formatProperties(items, nestingLevel, defaultFormattingContext());
    }

    default String formatProperties(Collection<T> items, int nestingLevel, FormattingContext context) {
        return formatProperties(items, VisualizationItem::getNewValues, nestingLevel, context);
    }

    <U extends T> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor,
            int nestingLevel, FormattingContext context);
    FormattingContext defaultFormattingContext();
}
