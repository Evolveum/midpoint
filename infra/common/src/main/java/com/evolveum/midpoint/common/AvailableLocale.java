/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Reads files:
 * * localization/locale.properties from classpath
 * * locale.properties from _midpoint.home_
 *
 * Loads available locales, format of properties is:
 * <ISO_2_CHAR_LANG>.name=
 * <ISO_2_CHAR_LANG>.flag=      # used for css of flag
 * <ISO_2_CHAR_LANG>.default=true
 *
 * e.g.
 * en.name=English
 * en.flag=en
 * en.default=true
 *
 * where default key is optional, and it's default locale used for midpoint.
 * If not specified {@link Locale#getDefault()} is used
 */
public class AvailableLocale {

    public static final List<LocaleDescriptor> AVAILABLE_LOCALES;

    private static final Trace LOGGER = TraceManager.getTrace(AvailableLocale.class);

    private static final String LOCALIZATION_DESCRIPTOR = "localization/locale.properties";

    private static final String PROP_NAME = ".name";
    private static final String PROP_FLAG = ".flag";
    private static final String PROP_DEFAULT = ".default";

    static {
        String midpointHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
        File file = new File(midpointHome, LOCALIZATION_DESCRIPTOR);

        Resource[] localeDescriptorResources = new Resource[] {
                new FileSystemResource(file),
                new ClassPathResource(LOCALIZATION_DESCRIPTOR)
        };

        List<LocaleDescriptor> locales = new ArrayList<>();
        for (Resource resource : localeDescriptorResources) {
            if (!resource.isReadable()) {
                continue;
            }

            try {
                LOGGER.debug("Found localization descriptor {}.", resource.getURL());
                locales = loadLocaleDescriptors(resource);

                break;
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load localization", ex);
            }
        }

        Collections.sort(locales);

        AVAILABLE_LOCALES = Collections.unmodifiableList(locales);
    }

    private static List<LocaleDescriptor> loadLocaleDescriptors(Resource resource) throws IOException {
        List<LocaleDescriptor> locales = new ArrayList<>();

        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);

            Map<String, Map<String, String>> localeMap = new HashMap<>();
            //noinspection unchecked,rawtypes
            Set<String> keys = (Set) properties.keySet();
            for (String key : keys) {
                String[] array = key.split("\\.");
                if (array.length != 2) {
                    continue;
                }

                String locale = array[0];
                Map<String, String> map = localeMap.computeIfAbsent(locale, k -> new HashMap<>());

                map.put(key, properties.getProperty(key));
            }

            for (String key : localeMap.keySet()) {
                Map<String, String> localeDefinition = localeMap.get(key);
                if (!localeDefinition.containsKey(key + PROP_NAME)
                        || !localeDefinition.containsKey(key + PROP_FLAG)) {
                    continue;
                }

                LocaleDescriptor descriptor = new LocaleDescriptor(
                        localeDefinition.get(key + PROP_NAME),
                        localeDefinition.get(key + PROP_FLAG),
                        localeDefinition.get(key + PROP_DEFAULT),
                        getLocaleFromString(key)
                );
                locales.add(descriptor);
            }
        }

        return locales;
    }

    private static Locale getLocaleFromString(String localeString) {
        if (localeString == null) {
            return null;
        }
        localeString = localeString.trim();
        if (localeString.equalsIgnoreCase("default")) {
            return Locale.getDefault();
        }

        // Extract language
        int languageIndex = localeString.indexOf('_');
        String language;
        if (languageIndex == -1) {
            // No further "_" so is "{language}" only
            return new Locale(localeString, "");
        } else {
            language = localeString.substring(0, languageIndex);
        }

        // Extract country
        int countryIndex = localeString.indexOf('_', languageIndex + 1);
        String country;
        if (countryIndex == -1) {
            // No further "_" so is "{language}_{country}"
            country = localeString.substring(languageIndex + 1);
            return new Locale(language, country);
        } else {
            // Assume all remaining is the variant so is
            // "{language}_{country}_{variant}"
            country = localeString.substring(languageIndex + 1, countryIndex);
            String variant = localeString.substring(countryIndex + 1);
            return new Locale(language, country, variant);
        }
    }

    public static boolean containsLocale(Locale locale) {
        if (locale == null) {
            return false;
        }

        for (LocaleDescriptor descriptor : AVAILABLE_LOCALES) {
            if (locale.equals(descriptor.getLocale())) {
                return true;
            }
        }

        return false;
    }

    public static Locale getBestMatchingLocale(Locale target) {
        final List<Locale> locales = AVAILABLE_LOCALES.stream().map(l -> l.getLocale()).toList();

        // Step 1: Look for an exact match
        for (Locale locale : locales) {
            if (locale.equals(target)) {
                return locale;
            }
        }

        // Step 2: Look for a match on language only
        for (Locale locale : locales) {
            if (locale.getLanguage().equals(target.getLanguage())) {
                return locale;
            }
        }

        return null;
    }

    public static Locale getDefaultLocale() {
        for (LocaleDescriptor descriptor : AVAILABLE_LOCALES) {
            if (descriptor.isDefault()) {
                return descriptor.getLocale();
            }
        }

        return Locale.getDefault();
    }

    public static class LocaleDescriptor implements Serializable, Comparable<LocaleDescriptor> {

        private final String name;
        private final String flag;
        private final Locale locale;
        private final boolean def;

        public LocaleDescriptor(String name, String flag, String def, Locale locale) {
            this.flag = flag;
            this.locale = locale;
            this.name = name;
            this.def = Boolean.parseBoolean(def);
        }

        public String getFlag() {
            return flag;
        }

        public Locale getLocale() {
            return locale;
        }

        public String getName() {
            return name;
        }

        public boolean isDefault() {
            return def;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (o == null || getClass() != o.getClass()) {return false;}
            LocaleDescriptor that = (LocaleDescriptor) o;
            return def == that.def && Objects.equals(name, that.name) && Objects.equals(flag, that.flag) && Objects.equals(locale, that.locale);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, flag, locale, def);
        }

        @Override
        public int compareTo(@NotNull LocaleDescriptor o) {
            Locale other = o.getLocale();

            int val = compareStrings(locale.getCountry(), other.getCountry());
            if (val != 0) {
                return val;
            }

            val = compareStrings(locale.getLanguage(), other.getLanguage());
            if (val != 0) {
                return val;
            }

            val = compareStrings(locale.getVariant(), other.getVariant());
            return val;
        }

        private int compareStrings(String s1, String s2) {
            if (s1 == null || s2 == null) {
                return 0;
            }

            return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
        }
    }
}
