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

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LocalizationServiceImpl implements LocalizationService {

    private static final Trace LOG = TraceManager.getTrace(LocalizationServiceImpl.class);

    private List<MessageSource> sources = new ArrayList<>();

    public void init() {
        URL url = buildMidpointHomeLocalizationFolderUrl();
        ClassLoader classLoader = new URLClassLoader(new URL[]{url}, null);

        sources.add(buildSource("Midpoint", classLoader));
        sources.add(buildSource(SchemaConstants.BUNDLE_NAME, classLoader));
        sources.add(buildSource("localization/Midpoint", null));
        sources.add(buildSource(SchemaConstants.SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH, null));

        // model security messages as fallback
        ResourceBundleMessageSource modelSecurity = new ResourceBundleMessageSource();
        modelSecurity.setBasename("com.evolveum.midpoint.security");
        sources.add(modelSecurity);

        // spring security messages as a fallback
        ResourceBundleMessageSource springSecurity = new ResourceBundleMessageSource();
        springSecurity.setBasename("org.springframework.security.messages");
        sources.add(springSecurity);
    }

    @Override
    public String translate(String key, Object[] params, Locale locale) {
        return translate(key, params, locale, null);
    }

    @Override
    public String translate(String key, Object[] params, Locale locale, String defaultMessage) {
        Object[] translated = translateParams(params, locale);

        for (MessageSource source : sources) {
            try {
                String value = source.getMessage(key, translated, locale);
                if (StringUtils.isNotEmpty(value)) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Resolved key {} to value {} using message source {}", new Object[]{key, value, source});
                    }

                    return value;
                }
            } catch (NoSuchMessageException ex) {
                // nothing to do
            }
        }

        return defaultMessage;
    }

    @Override
    public String translate(LocalizableMessage msg, Locale locale) {
        String translated = translate(msg.getKey(), msg.getArgs(), locale);
        if (StringUtils.isNotEmpty(translated)) {
            return translated;
        }

        if (msg.getFallbackLocalizableMessage() != null) {
            translated = translate(msg.getFallbackLocalizableMessage(), locale);

            if (StringUtils.isNotEmpty(translated)) {
                return translated;
            }
        }

        return msg.getFallbackMessage();
    }

    private ResourceBundleMessageSource buildSource(String basename, ClassLoader classLoader) {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setDefaultEncoding(StandardCharsets.UTF_8.name());
        source.setFallbackToSystemLocale(false);
        source.setBasename(basename);

        if (classLoader == null) {
            classLoader = LocalizationServiceImpl.class.getClassLoader();
        }
        source.setBundleClassLoader(classLoader);

        return source;
    }

    private URL buildMidpointHomeLocalizationFolderUrl() {
        String midpointHome = System.getProperty("midpoint.home");

        File file = new File(midpointHome, "localization");
        try {
            return file.toURI().toURL();
        } catch (IOException ex) {
            throw new SystemException("Couldn't transform localization folder file to url", ex);
        }
    }

    private Object[] translateParams(Object[] params, Locale locale) {
        if (params == null) {
            return null;
        }

        Object[] translated = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param instanceof Object[]) {
                param = translateParams((Object[]) param, locale);
            } else if (param instanceof LocalizableMessage) {
                LocalizableMessage msg = (LocalizableMessage) param;
                param = translate(msg, locale);
            }

            translated[i] = param;
        }

        return translated;
    }
}
