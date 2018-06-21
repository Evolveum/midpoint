/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.common;

import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Looking for resource bundle in compressed war and compressed libraries inside war is pretty expensive.
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

        ResourceBundle bundle = super.getResourceBundle(basename, locale);
        locales.put(locale, bundle != null);

        return bundle;
    }
}
