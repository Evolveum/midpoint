package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;

public interface PropertiesFormatter<T extends VisualizationItem> {
    default String formatProperties(Collection<T> items, int nestingLevel) {
        return formatProperties(items, VisualizationItem::getNewValues, nestingLevel);
    }

    <U extends T> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor,
            int nestingLevel);
}
