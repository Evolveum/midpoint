/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public final class AdditionalIdentificationFormatter implements PropertiesFormatter<VisualizationItem> {

    private static final Trace LOGGER = TraceManager.getTrace(AdditionalIdentificationFormatter.class);

    private final PropertiesFormatter<VisualizationItem> propertiesFormatter;
    private final IndentationGenerator indentationGenerator;

    public AdditionalIdentificationFormatter(PropertiesFormatter<VisualizationItem> propertiesFormatter,
            IndentationGenerator indentationGenerator) {
        this.propertiesFormatter = propertiesFormatter;
        this.indentationGenerator = indentationGenerator;
    }

    @Override
    public String formatProperties(Collection<VisualizationItem> items, int nestingLevel) {
        LOGGER.trace("Formatting the properties: {}", items);
        if (items.isEmpty()) {
            return "";
        }
        final String baseIndentation = this.indentationGenerator.indentation(nestingLevel);
        final int propertiesNestingLevel = nestingLevel + 1;
        var formatingResult = baseIndentation + "Additional identification (not modified data):\n"
                + this.propertiesFormatter.formatProperties(items, propertiesNestingLevel);
        LOGGER.trace("Properties formatting ends up with result: {}", formatingResult);
        return formatingResult;
    }

    @Override
    public <U extends VisualizationItem> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor, int nestingLevel) {
        throw new UnsupportedOperationException("Generic version of this method is not supported by this "
                + "implementation.");
    }

}
