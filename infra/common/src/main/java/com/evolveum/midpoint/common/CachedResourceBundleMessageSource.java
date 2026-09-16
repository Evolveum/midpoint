/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common;

import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Looking for resource bundle in compressed JAR and compressed libraries inside JAR is pretty expensive.
 * Therefore this implementation caches information about whether bundle exists.
 *
 * @author Viliam Repan (lazyman).
 */
public class CachedResourceBundleMessageSource extends ResourceBundleMessageSource {

    private Map<String, Map<Locale, Boolean>> bundleExistenceMap = new HashMap<>();

    @Override
    protected ResourceBundle getResourceBundle(String basename, Locale locale) {
        Map<Locale, Boolean> locales = bundleExistenceMap.get(basename);
        if (locales == null) {
            locales = new HashMap<>();
            bundleExistenceMap.put(basename, locales);
        }

        Boolean exists = locales.get(locale);
        if (Boolean.FALSE.equals(exists)) {
            // we've already tried to find bundle, but it doesn't exist, so don't look for it
            return null;
        }

        ResourceBundle bundle;
        try {
            bundle = super.getResourceBundle(basename, locale);
        } catch (MissingResourceException ex) {
            // Since Spring 7.0.9 getResourceBundle() no longer swallows this exception for locales
            // unknown to the JVM (e.g. user-supplied language "cz"). Missing bundle is a normal
            // situation here, the next message source in LocalizationServiceImpl is tried.
            bundle = null;
        }
        locales.put(locale, bundle != null);

        return bundle;
    }
}
