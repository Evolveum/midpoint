/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.util;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.resource.loader.IStringResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * @author Viliam Repan (lazyman)
 */
public class Utf8BundleStringResourceLoader implements IStringResourceLoader {

    private final String bundleName;

    private ClassLoader classLoader;

    public Utf8BundleStringResourceLoader(String bundleName) {
        this(bundleName, null);
    }

    public Utf8BundleStringResourceLoader(String bundleName, ClassLoader classLoader) {
        this.bundleName = bundleName;
        this.classLoader = classLoader;
    }

    @Override
    public String loadStringResource(Class<?> clazz, String key, Locale locale, String style, String variation) {
        return loadStringResource((Component) null, key, locale, style, variation);
    }

    @Override
    public String loadStringResource(Component component, String key, Locale locale, String style, String variation) {
        if (locale == null) {
            locale = Session.exists() ? Session.get().getLocale() : Locale.getDefault();
        }

        ResourceBundle.Control control = new UTF8Control();
        try {
            if (classLoader == null) {
                return ResourceBundle.getBundle(bundleName, locale, control).getString(key);
            } else {
                return ResourceBundle.getBundle(bundleName, locale, classLoader, control).getString(key);
            }
        } catch (MissingResourceException ex) {
            if (classLoader != null) {
                return null;
            }

            try {
                return ResourceBundle.getBundle(bundleName, locale,
                        Thread.currentThread().getContextClassLoader(), control).getString(key);
            } catch (MissingResourceException ex2) {
                return null;
            }
        }
    }

    private static class UTF8Control extends ResourceBundle.Control {

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                                        boolean reload) throws IllegalAccessException, InstantiationException,
                IOException {

            // The below is a copy of the default implementation.
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            ResourceBundle bundle = null;
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            } else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                try {
                    // Only this line is changed to make it to read properties files as UTF-8.
                    bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }
            return bundle;
        }
    }
}
