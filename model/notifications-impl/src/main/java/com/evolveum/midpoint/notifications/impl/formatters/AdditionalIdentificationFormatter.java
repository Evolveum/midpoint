package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;

public final class AdditionalIdentificationFormatter implements PropertiesFormatter<VisualizationItem> {
    private final PropertiesFormatter<VisualizationItem> propertiesFormatter;
    private final IndentationGenerator indentationGenerator;

    public AdditionalIdentificationFormatter(PropertiesFormatter<VisualizationItem> propertiesFormatter,
            IndentationGenerator indentationGenerator) {
        this.propertiesFormatter = propertiesFormatter;
        this.indentationGenerator = indentationGenerator;
    }

    @Override
    public String formatProperties(Collection<VisualizationItem> items, int nestingLevel) {
        if (items.isEmpty()) {
            return "";
        }
        final String baseIndentation = this.indentationGenerator.indentation(nestingLevel);
        final int propertiesNestingLevel = nestingLevel + 1;
        return baseIndentation + "Additional identification (not modified data):\n"
                + this.propertiesFormatter.formatProperties(items, propertiesNestingLevel);
    }

    @Override
    public <U extends VisualizationItem> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor, int nestingLevel) {
        throw new UnsupportedOperationException("Generic version of this method is not supported by this "
                + "implementation.");
    }

}
