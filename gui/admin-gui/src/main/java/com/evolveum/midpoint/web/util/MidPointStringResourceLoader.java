/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.common.LocalizationService;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.resource.loader.IStringResourceLoader;

import java.util.Locale;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointStringResourceLoader implements IStringResourceLoader {

    private LocalizationService resourceLoader;

    public MidPointStringResourceLoader(LocalizationService resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public String loadStringResource(Class<?> clazz, String key, Locale locale, String style, String variation) {
        return loadStringResource((Component) null, key, locale, style, variation);
    }

    @Override
    public String loadStringResource(Component component, String key, Locale locale, String style, String variation) {
        if (resourceLoader == null) {
            // Just for tests
            return key;
        }

        if (locale == null) {
            locale = Session.exists() ? Session.get().getLocale() : Locale.getDefault();
        }

        return resourceLoader.translate(key, null, locale);
    }
}
