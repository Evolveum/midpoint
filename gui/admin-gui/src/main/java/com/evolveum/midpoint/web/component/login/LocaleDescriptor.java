/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.login;

import com.evolveum.midpoint.web.util.MiscUtil;
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
        this.locale = MiscUtil.getLocaleFromString(locale);
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
