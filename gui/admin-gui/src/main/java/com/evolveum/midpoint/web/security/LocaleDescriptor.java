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

package com.evolveum.midpoint.web.security;

import java.io.Serializable;
import java.util.Locale;

/**
 * @author lazyman
 */
public class LocaleDescriptor implements Serializable, Comparable<LocaleDescriptor> {

    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_FLAG = "flag";
    private static final String PROPERTY_LOCALE = "locale";
    private static final String PROPERTY_DEFAULT = "def";

    private String name;
    private String flag;
    private Locale locale;
    private boolean def;

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
    public int compareTo(LocaleDescriptor o) {
        if (o == null) {
            return 0;
        }

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
        if (val != 0) {
            return val;
        }

        return 0;
    }

    private int compareStrings(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
    }
}
