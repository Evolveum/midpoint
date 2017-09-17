/*
 * Copyright (c) 2010-2017 Evolveum
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
        if (locale == null) {
            locale = Session.exists() ? Session.get().getLocale() : Locale.getDefault();
        }

        return resourceLoader.translate(key, null, locale, key);
    }
}
