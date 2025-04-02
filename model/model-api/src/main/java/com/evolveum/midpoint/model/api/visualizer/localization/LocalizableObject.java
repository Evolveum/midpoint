package com.evolveum.midpoint.model.api.visualizer.localization;

import java.util.Locale;

import com.evolveum.midpoint.common.LocalizationService;

/**
 * Represents object, which can be localized.
 *
 * Depending on the implementation, the localization may cause cascading "wrapping" of underlying localization parts
 * to the resulting form (specified by the {@code R} type parameter).
 *
 * @param <R> the type of the underlying object which could be translated.
 */
public interface LocalizableObject<R> {

    /**
     * Translate underlying localizable object.
     *
     * @param localizationService the service used for translation.
     * @param locale the locale to which the translation should be done.
     * @return Translated object.
     */
    R translate(LocalizationService localizationService, Locale locale);
}
