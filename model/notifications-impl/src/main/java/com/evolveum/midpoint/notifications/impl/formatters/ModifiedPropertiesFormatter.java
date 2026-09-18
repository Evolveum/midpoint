/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

final class ModifiedPropertiesFormatter implements PropertiesFormatter<VisualizationDeltaItem> {

    private static final Trace LOGGER = TraceManager.getTrace(ModifiedPropertiesFormatter.class);

    private final PropertyFormatter propertyFormatter;
    private final IndentationGenerator indentationGenerator;

    ModifiedPropertiesFormatter(PropertyFormatter propertyFormatter,
            IndentationGenerator indentationGenerator) {
        this.propertyFormatter = propertyFormatter;
        this.indentationGenerator = indentationGenerator;
    }

    @Override
    public String formatProperties(Collection<VisualizationDeltaItem> propertiesDeltas, int nestingLevel, FormattingContext context) {
        LOGGER.trace("Formatting the properties: {}", propertiesDeltas);
        final String labelIndentation = this.indentationGenerator.indentation(nestingLevel);
        final String operationIndentation = this.indentationGenerator.indentation(nestingLevel + 1);
        final String valuesIndentation = this.indentationGenerator.indentation(nestingLevel + 2);

        final List<String> replacedProperties = new ArrayList<>();
        final List<String> addedOrDeletedProperties = new ArrayList<>();
        for (final VisualizationDeltaItem delta : propertiesDeltas) {
            if (isPropertyReplaced(delta)) {
                replacedProperties.add(formatReplacedValues(delta, labelIndentation, context));
            } else {
                addedOrDeletedProperties.add(formatValuesAdditionsAndDeletions(delta, labelIndentation,
                        operationIndentation, valuesIndentation, context));
            }
        }
        var formatingResult = Stream.of(replacedProperties, addedOrDeletedProperties)
                .flatMap(Collection::stream)
                .collect(Collectors.joining("\n"));
        LOGGER.trace("Properties formatting ends up with result: {}", formatingResult);
        return formatingResult;
    }

    @Override
    public <U extends VisualizationDeltaItem> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor, int nestingLevel, FormattingContext context) {
        throw new UnsupportedOperationException("Generic version of this method is not supported by this "
                + "implementation.");
    }

    private String formatValuesAdditionsAndDeletions(VisualizationDeltaItem delta, String labelIndentation,
            String operationIndentation, String valuesIndentation, FormattingContext context) {
        final String formattedAdditions = formatModifiedProperties(delta.getAddedValues(),
                this.propertyFormatter.translate("ModifiedPropertiesFormatter.addedValues", context, "Added values"),
                operationIndentation, valuesIndentation, context);
        final String formattedDeletions = formatModifiedProperties(delta.getDeletedValues(),
                this.propertyFormatter.translate("ModifiedPropertiesFormatter.deletedValues", context, "Deleted values"),
                operationIndentation, valuesIndentation, context);

        final String label;
        if (formattedAdditions.isEmpty() && formattedDeletions.isEmpty()) {
            return "";
        } else {
            label = labelIndentation + this.propertyFormatter.itemLabel(delta.getName(), context) + ":";
        }
        return Stream.of(label, formattedAdditions, formattedDeletions)
                // we don't want an empty additions or deletions to cause extra new line so filter them out.
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.joining("\n"));
    }

    private String formatModifiedProperties(Collection<? extends VisualizationItemValue> values,
            String operationLabel, String operationIndentation, String valuesIndentation, FormattingContext context) {
        final String formattedValues = this.propertyFormatter.itemValue(values, valuesIndentation, context);
        if (formattedValues.isEmpty()) {
            return  "";
        } else {
            return operationIndentation + operationLabel + ":" + formattedValues;
        }
    }

    private String formatReplacedValues(VisualizationDeltaItem delta, String labelIndentation, FormattingContext context) {
        return labelIndentation + this.propertyFormatter.itemLabel(delta.getName(), context) + ":"
                + this.propertyFormatter.itemValue(delta.getDeletedValues(), "", context) + " ->"
                + this.propertyFormatter.itemValue(delta.getAddedValues(), "", context);
    }

    private static boolean isPropertyReplaced(VisualizationDeltaItem delta) {
        return delta.getAddedValues().size() == 1 && delta.getDeletedValues().size() == 1 && delta.getUnchangedValues()
                .isEmpty();
    }

    @Override
    public FormattingContext defaultFormattingContext() {
        return this.propertyFormatter.defaultFormattingContext();
    }

}
