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

final class ModifiedPropertiesFormatter implements PropertiesFormatter<VisualizationDeltaItem> {

    private final PropertyFormatter propertyFormatter;
    private final IndentationGenerator indentationGenerator;

    ModifiedPropertiesFormatter(PropertyFormatter propertyFormatter,
            IndentationGenerator indentationGenerator) {
        this.propertyFormatter = propertyFormatter;
        this.indentationGenerator = indentationGenerator;
    }

    @Override
    public String formatProperties(Collection<VisualizationDeltaItem> propertiesDeltas, int nestingLevel) {
        final String labelIndentation = this.indentationGenerator.indentation(nestingLevel);
        final String operationIndentation = this.indentationGenerator.indentation(nestingLevel + 1);
        final String valuesIndentation = this.indentationGenerator.indentation(nestingLevel + 2);

        final List<String> replacedProperties = new ArrayList<>();
        final List<String> addedOrDeletedProperties = new ArrayList<>();
        for (final VisualizationDeltaItem delta : propertiesDeltas) {
            if (isPropertyReplaced(delta)) {
                replacedProperties.add(formatReplacedValues(delta, labelIndentation));
            } else {
                addedOrDeletedProperties.add(formatValuesAdditionsAndDeletions(delta, labelIndentation,
                        operationIndentation, valuesIndentation));
            }
        }
        return Stream.of(replacedProperties, addedOrDeletedProperties)
                .flatMap(Collection::stream)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public <U extends VisualizationDeltaItem> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor, int nestingLevel) {
        throw new UnsupportedOperationException("Generic version of this method is not supported by this "
                + "implementation.");
    }

    private String formatValuesAdditionsAndDeletions(VisualizationDeltaItem delta, String labelIndentation,
            String operationIndentation, String valuesIndentation) {
        final String formattedAdditions = formatModifiedProperties(delta.getAddedValues(), "Added values",
                operationIndentation, valuesIndentation);
        final String formattedDeletions = formatModifiedProperties(delta.getDeletedValues(), "Deleted values",
                operationIndentation, valuesIndentation);

        final String label;
        if (formattedAdditions.isEmpty() && formattedDeletions.isEmpty()) {
            return "";
        } else {
            label = labelIndentation + this.propertyFormatter.itemLabel(delta.getName()) + ":";
        }
        return Stream.of(label, formattedAdditions, formattedDeletions)
                // we don't want an empty additions or deletions to cause extra new line so filter them out.
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.joining("\n"));
    }

    private String formatModifiedProperties(Collection<? extends VisualizationItemValue> values,
            String operationLabel, String operationIndentation, String valuesIndentation) {
        final String formattedValues = this.propertyFormatter.itemValue(values, valuesIndentation);
        if (formattedValues.isEmpty()) {
            return  "";
        } else {
            return operationIndentation + operationLabel + ":" + formattedValues;
        }
    }

    private String formatReplacedValues(VisualizationDeltaItem delta, String labelIndentation) {
        return labelIndentation + this.propertyFormatter.itemLabel(delta.getName()) + ":"
                + this.propertyFormatter.itemValue(delta.getDeletedValues(), "") + " ->"
                + this.propertyFormatter.itemValue(delta.getAddedValues(), "");
    }

    private static boolean isPropertyReplaced(VisualizationDeltaItem delta) {
        return delta.getAddedValues().size() == 1 && delta.getDeletedValues().size() == 1 && delta.getUnchangedValues()
                .isEmpty();
    }

}
