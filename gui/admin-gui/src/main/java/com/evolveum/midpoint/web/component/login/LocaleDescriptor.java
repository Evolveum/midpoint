/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.login;

import com.evolveum.midpoint.web.util.WebMiscUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.Locale;
import java.util.Properties;

/**
 * @author lazyman
 */
public class LocaleDescriptor implements Serializable {

    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_FLAG = "flag";
    private static final String PROPERTY_LOCALE = "locale";
    private String name;
    private String flag;
    private Locale locale;

    public LocaleDescriptor(Properties properties) {
        Validate.notNull(properties);

        this.name = (String) properties.get(PROPERTY_NAME);
        this.flag = (String) properties.get(PROPERTY_FLAG);

        String locale = (String) properties.get(PROPERTY_LOCALE);
        if (StringUtils.isEmpty(locale)) {
            throw new IllegalStateException("Property file - locale descriptor doesn't contain property '"
                    + PROPERTY_LOCALE + "' with locale definition.");
        }
        this.locale = WebMiscUtil.getLocaleFromString(locale);
    }

    public LocaleDescriptor(String name, String flag, Locale locale) {
        this.flag = flag;
        this.locale = locale;
        this.name = name;
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
}
