package com.evolveum.midpoint.notifications.impl.formatters;

import java.util.Collection;
import java.util.stream.Collectors;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItemValue;

public class PropertyFormatter {

    private final LocalizationService localizationService;
    private final String singleValuePrefix;
    private final String multiValuePrefix;

    public PropertyFormatter(LocalizationService localizationService, String singleValuePrefix,
            String multiValuePrefix) {
        this.localizationService = localizationService;
        this.singleValuePrefix = singleValuePrefix;
        this.multiValuePrefix = multiValuePrefix;
    }

    String itemLabel(Name itemName) {
        if (itemName.getDisplayName() != null) {
            return this.localizationService.translate(itemName.getDisplayName());
        } else if (itemName.getSimpleName() != null) {
            return this.localizationService.translate(itemName.getSimpleName());
        }
        // TODO translate
        return "Unknown";
    }

    String itemValue(Collection<? extends VisualizationItemValue> values, String indentation) {
        if (values.isEmpty()) {
            return "";
        }

        if (values.size() == 1) {
            return this.singleValuePrefix + this.localizationService.translate(values.iterator().next().getText());
        }

        return this.multiValuePrefix + values.stream()
                .map(value -> indentation + this.localizationService.translate(value.getText()))
                .collect(Collectors.joining(multiValuePrefix));
    }

}
