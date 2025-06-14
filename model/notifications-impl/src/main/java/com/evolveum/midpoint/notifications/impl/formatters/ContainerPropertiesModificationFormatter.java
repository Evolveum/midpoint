/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

final class ContainerPropertiesModificationFormatter implements PropertiesFormatter<VisualizationDeltaItem> {

    private static final Trace LOGGER = TraceManager.getTrace(ContainerPropertiesModificationFormatter.class);

    private final PropertiesFormatter<VisualizationItem> propertiesFormatter;
    private final IndentationGenerator indentationGenerator;
    private final PropertiesFormatter<VisualizationDeltaItem> modifiedPropertiesFormatter;

    public ContainerPropertiesModificationFormatter(PropertiesFormatter<VisualizationItem> propertiesFormatter,
            IndentationGenerator indentationGenerator,
            PropertiesFormatter<VisualizationDeltaItem> modifiedPropertiesFormatter) {
        this.propertiesFormatter = propertiesFormatter;
        this.indentationGenerator = indentationGenerator;
        this.modifiedPropertiesFormatter = modifiedPropertiesFormatter;
    }

    @Override
    public String formatProperties(Collection<VisualizationDeltaItem> items, int nestingLevel) {
        LOGGER.trace("Formatting the properties: {}", items);
        if (items.isEmpty()) {
            return "";
        }
        final List<VisualizationDeltaItem> addedProperties = new ArrayList<>();
        final List<VisualizationDeltaItem> deletedProperties = new ArrayList<>();
        final List<VisualizationDeltaItem> modifiedProperties = new ArrayList<>();
        for (final VisualizationDeltaItem delta : items) {
            if (propertyWillBeAdded(delta)) {
                addedProperties.add(delta);
            } else if (propertyWillBeRemoved(delta)) {
                deletedProperties.add(delta);
            } else {
                modifiedProperties.add(delta);
            }

        }
        final String baseIndentation = this.indentationGenerator.indentation(nestingLevel);
        final int propertiesNestingLevel = nestingLevel + 1;

        var formatingResult = Stream.of(
                prefixIfNotEmpty(baseIndentation, "Added properties:\n", formatAddedProperties(addedProperties,
                        propertiesNestingLevel)),
                prefixIfNotEmpty(baseIndentation, "Deleted properties:\n", formatDeletedProperties(deletedProperties,
                        propertiesNestingLevel)),
                prefixIfNotEmpty(baseIndentation, "Modified properties:\n", formatModifiedProperties(modifiedProperties,
                        propertiesNestingLevel)))
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.joining("\n"));
        LOGGER.trace("Properties formatting ends up with result: {}", formatingResult);
        return formatingResult;
    }

    @Override
    public <U extends VisualizationDeltaItem> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor, int nestingLevel) {
        throw new UnsupportedOperationException("Generic version of this method is not supported by this "
                + "implementation.");
    }

    private String formatModifiedProperties(Collection<VisualizationDeltaItem> modifiedProperties, int nestingLevel) {
        if (modifiedProperties.isEmpty()) {
            return "";
        }
        return this.modifiedPropertiesFormatter.formatProperties(modifiedProperties, nestingLevel);
    }

    private String formatAddedProperties(Collection<VisualizationDeltaItem> addedProperties, int nestingLevel) {
        if (addedProperties.isEmpty()) {
            return "";
        }
        return this.propertiesFormatter.formatProperties(addedProperties, VisualizationDeltaItem::getAddedValues,
                nestingLevel);
    }

    private String formatDeletedProperties(Collection<VisualizationDeltaItem> deletedProperties, int nestingLevel) {
        if (deletedProperties.isEmpty()) {
            return "";
        }
        return this.propertiesFormatter.formatProperties(deletedProperties, VisualizationDeltaItem::getDeletedValues,
                nestingLevel);
    }

    private static String prefixIfNotEmpty(String indentation, String prefix, String value) {
        if (value.isEmpty()) {
            return "";
        }
        return indentation + prefix + value;
    }

    private static boolean propertyWillBeRemoved(VisualizationDeltaItem delta) {
        return delta.getUnchangedValues().isEmpty() && delta.getAddedValues().isEmpty();
    }

    private static boolean propertyWillBeAdded(VisualizationDeltaItem delta) {
        return delta.getUnchangedValues().isEmpty() && delta.getDeletedValues().isEmpty();
    }

}
