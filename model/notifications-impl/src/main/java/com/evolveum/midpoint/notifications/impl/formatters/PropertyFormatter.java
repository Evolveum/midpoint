/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;

public final class PropertyFormatter {

    private final LocalizationService localizationService;
    private final String singleValuePrefix;
    private final String multiValuePrefix;

    public PropertyFormatter(LocalizationService localizationService, String singleValuePrefix,
            String multiValuePrefix) {
        this.localizationService = localizationService;
        this.singleValuePrefix = singleValuePrefix;
        this.multiValuePrefix = multiValuePrefix;
    }

    String itemLabel(Name itemName, FormattingContext context) {
        if (itemName.getDisplayName() != null) {
            return this.localizationService.translate(itemName.getDisplayName(), context.locale());
        } else if (itemName.getSimpleName() != null) {
            return this.localizationService.translate(itemName.getSimpleName(), context.locale());
        }
        return translate("PropertyFormatter.unknown", context, "Unknown");
    }

    String itemValue(Collection<? extends VisualizationItemValue> values, String indentation, FormattingContext context) {
        if (values.isEmpty()) {
            return "";
        }

        if (values.size() == 1) {
            return this.singleValuePrefix + this.localizationService.translate(values.iterator().next().getText(), context.locale());
        }

        return this.multiValuePrefix + values.stream()
                .map(value -> indentation + this.localizationService.translate(value.getText(), context.locale()))
                .collect(Collectors.joining(multiValuePrefix));
    }

    String translate(String key, FormattingContext context, String defaultMessage) {
        return this.localizationService.translate(key, new Object[0], context.locale(), defaultMessage);
    }

    FormattingContext defaultFormattingContext() {
        return new FormattingContext(this.localizationService.getDefaultLocale());
    }

}
