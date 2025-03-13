package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;

import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;

public interface AdditionalIdentificationFormatter {
    String formatAdditionalInformations(Collection<VisualizationItem> additionalProperties, int nestingLevel);
}
