/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

final class PlainTextPropertiesFormatter implements PropertiesFormatter<VisualizationItem> {

    private static final Trace LOGGER = TraceManager.getTrace(PlainTextPropertiesFormatter.class);

    private final IndentationGenerator indentationGenerator;
    private final PropertyFormatter propertyFormatter;

    public PlainTextPropertiesFormatter(IndentationGenerator indentationGenerator,
            PropertyFormatter propertyFormatter) {
        this.indentationGenerator = indentationGenerator;
        this.propertyFormatter = propertyFormatter;
    }

    @Override
    public <U extends VisualizationItem> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor, int nestingLevel) {
        LOGGER.trace("Formatting the properties: {}", items);
        if (items.isEmpty()) {
            return "";
        }

        final String indentation = this.indentationGenerator.indentation(nestingLevel);
        final String valuesIndentation = this.indentationGenerator.indentation(nestingLevel + 1);
        var formatingResult =  items.stream()
                .map(item -> {
                    final Collection<? extends VisualizationItemValue> values = valuesExtractor.apply(item);
                    final String formattedValues = this.propertyFormatter.itemValue(values, valuesIndentation);
                    final String formattedLabel = this.propertyFormatter.itemLabel(item.getName());
                    return indentation + formattedLabel + ":" + formattedValues;
                })
                .collect(Collectors.joining("\n"));
        LOGGER.trace("Properties formatting ends up with result: {}", formatingResult);
        return formatingResult;
    }

}
