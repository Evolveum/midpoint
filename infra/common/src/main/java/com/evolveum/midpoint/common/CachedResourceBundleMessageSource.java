/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import org.springframework.context.support.ResourceBundleMessageSource;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Looking for resource bundle in compressed JAR and compressed libraries inside JAR is pretty expensive.
 * Therefore this implementation caches information about whether bundle exists.
 *
 * Optionally (see {@link #setEscapeSingleQuotes(boolean)}) it also escapes single quotes in messages
 * before they are formatted with arguments, see {@link #escapeSingleQuotes(String)}.
 *
 * @author Viliam Repan (lazyman).
 */
public class CachedResourceBundleMessageSource extends ResourceBundleMessageSource {

    private Map<String, Map<Locale, Boolean>> bundleExistenceMap = new HashMap<>();

    private boolean escapeSingleQuotes;

    public boolean isEscapeSingleQuotes() {
        return escapeSingleQuotes;
    }

    /**
     * If set, single quotes in messages are treated as plain characters (e.g. apostrophes in "Couldn't"
     * or "n'est", quotes around argument in "'{0}'"), the same way Wicket StringResourceModel treats them.
     * This way the same localization value works for both GUI (Wicket) and {@link LocalizationService}.
     */
    public void setEscapeSingleQuotes(boolean escapeSingleQuotes) {
        this.escapeSingleQuotes = escapeSingleQuotes;
    }

    /**
     * Called only when message is formatted with arguments, messages without arguments are returned as is.
     */
    @Override
    protected MessageFormat createMessageFormat(String msg, Locale locale) {
        return super.createMessageFormat(escapeSingleQuotes ? escapeSingleQuotes(msg) : msg, locale);
    }

    /**
     * Escapes single quotes outside of format elements ({...}) for {@link MessageFormat},
     * so they are displayed instead of starting a quoted (not substituted) section.
     *
     * Similar to Wicket StringResourceModel.escapeQuotes(), but already escaped quotes ('') are kept as they are,
     * so values like "Policy rule ''{0}'' violation" are still formatted correctly.
     */
    private static String escapeSingleQuotes(String msg) {
        if (msg == null || msg.indexOf('\'') < 0) {
            return msg;
        }

        StringBuilder sb = new StringBuilder(msg.length() + 10);
        int depth = 0;
        for (int i = 0; i < msg.length(); i++) {
            char ch = msg.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
            }

            if (ch == '\'' && depth == 0) {
                sb.append("''");
                if (i + 1 < msg.length() && msg.charAt(i + 1) == '\'') {
                    // already escaped quote, keep it as is
                    i++;
                }
            } else {
                sb.append(ch);
            }
        }

        return sb.toString();
    }

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
