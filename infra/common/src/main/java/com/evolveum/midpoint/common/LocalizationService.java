/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface LocalizationService {

    String translate(String key, Object[] params, Locale locale);

    String translate(String key, Object[] params, Locale locale, String defaultMessage);

    String translate(LocalizableMessage msg, Locale locale);

    String translate(LocalizableMessage msg, Locale locale, String defaultMessage);

    default String translate(LocalizableMessage msg) {
        return translate(msg, getDefaultLocale());
    }

    String translate(PolyString polyString, Locale locale, boolean allowOrig);

    default String translate(PolyString polyString) {
        return translate(polyString, getDefaultLocale(), true);
    }

    default String translate(PolyString polyString, boolean allowOrig) {
        return translate(polyString, getDefaultLocale(), allowOrig);
    }

    /**
     * Fills-in message and localizedMessage based on userFriendlyMessage, if needed.
     */
    <T extends CommonException> T translate(T e);

    @NotNull
    Locale getDefaultLocale();
}
