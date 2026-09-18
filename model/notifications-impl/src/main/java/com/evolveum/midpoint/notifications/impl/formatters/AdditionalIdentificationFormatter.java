/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.function.Function;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public final class AdditionalIdentificationFormatter implements PropertiesFormatter<VisualizationItem> {

    private static final Trace LOGGER = TraceManager.getTrace(AdditionalIdentificationFormatter.class);

    private final PropertiesFormatter<VisualizationItem> propertiesFormatter;
    private final IndentationGenerator indentationGenerator;
    private final LocalizationService localizationService;

    public AdditionalIdentificationFormatter(PropertiesFormatter<VisualizationItem> propertiesFormatter,
            IndentationGenerator indentationGenerator, LocalizationService localizationService) {
        this.propertiesFormatter = propertiesFormatter;
        this.indentationGenerator = indentationGenerator;
        this.localizationService = localizationService;
    }

    @Override
    public String formatProperties(Collection<VisualizationItem> items, int nestingLevel, FormattingContext context) {
        LOGGER.trace("Formatting the properties: {}", items);
        if (items.isEmpty()) {
            return "";
        }
        final String baseIndentation = this.indentationGenerator.indentation(nestingLevel);
        final int propertiesNestingLevel = nestingLevel + 1;
        var formatingResult = baseIndentation
                + this.localizationService.translate(
                        "AdditionalIdentificationFormatter.additionalIdentification", new Object[0], context.locale(),
                        "Additional identification (not modified data)")
                + ":\n"
                + this.propertiesFormatter.formatProperties(items, propertiesNestingLevel, context);
        LOGGER.trace("Properties formatting ends up with result: {}", formatingResult);
        return formatingResult;
    }

    @Override
    public <U extends VisualizationItem> String formatProperties(Collection<U> items,
            Function<U, Collection<? extends VisualizationItemValue>> valuesExtractor, int nestingLevel, FormattingContext context) {
        throw new UnsupportedOperationException("Generic version of this method is not supported by this "
                + "implementation.");
    }

    @Override
    public FormattingContext defaultFormattingContext() {
        return new FormattingContext(this.localizationService.getDefaultLocale());
    }

}
