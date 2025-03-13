package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;

final class PlainTextPropertiesFormatter implements PropertiesFormatter<VisualizationItem> {

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
        if (items.isEmpty()) {
            return "";
        }

        final String indentation = this.indentationGenerator.indentation(nestingLevel);
        final String valuesIndentation = this.indentationGenerator.indentation(nestingLevel + 1);
        return items.stream()
                .map(item -> {
                    final Collection<? extends VisualizationItemValue> values = valuesExtractor.apply(item);
                    final String formattedValues = this.propertyFormatter.itemValue(values, valuesIndentation);
                    final String formattedLabel = this.propertyFormatter.itemLabel(item.getName());
                    return indentation + formattedLabel + ":" + formattedValues;
                })
                .collect(Collectors.joining("\n"));
    }

}
