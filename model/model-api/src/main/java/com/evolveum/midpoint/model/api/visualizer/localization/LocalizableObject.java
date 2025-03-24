package com.evolveum.midpoint.model.api.visualizer.localization;

import java.util.Locale;

import com.evolveum.midpoint.common.LocalizationService;

public interface LocalizableObject<R> {
    R translate(LocalizationService localizationService, Locale locale);
}
