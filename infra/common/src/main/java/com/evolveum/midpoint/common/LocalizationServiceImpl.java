/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageList;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationArgumentType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
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
import java.util.stream.Collectors;

/**
 * @author Viliam Repan (lazyman).
 * @author Radovan Semancik
 */
public class LocalizationServiceImpl implements LocalizationService {

    private static final Trace LOG = TraceManager.getTrace(LocalizationServiceImpl.class);

    private List<MessageSource> sources = new ArrayList<>();

    private Locale overrideLocale = null; // for tests

    public void init() {
        URL url = buildMidpointHomeLocalizationFolderUrl();
        ClassLoader classLoader = new URLClassLoader(new URL[]{url}, null);

        sources.add(buildSource("Midpoint", classLoader));
        sources.add(buildSource(SchemaConstants.BUNDLE_NAME, classLoader));
        sources.add(buildSource("localization/Midpoint", null));
        sources.add(buildSource(SchemaConstants.SCHEMA_LOCALIZATION_PROPERTIES_RESOURCE_BASE_PATH, null));
        sources.add(buildSource(MidpointConfiguration.MIDPOINT_SYSTEM_PROPERTIES_BASE_PATH, null));

        // model security messages as fallback
        ResourceBundleMessageSource modelSecurity = new CachedResourceBundleMessageSource();
        modelSecurity.setBasename("com.evolveum.midpoint.security");
        sources.add(modelSecurity);

        // spring security messages as a fallback
        ResourceBundleMessageSource springSecurity = new CachedResourceBundleMessageSource();
        springSecurity.setBasename("org.springframework.security.messages");
        sources.add(springSecurity);
    }

    public Locale getOverrideLocale() {
        return overrideLocale;
    }

    public void setOverrideLocale(Locale overrideLocale) {
        this.overrideLocale = overrideLocale;
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
                        LOG.trace("Resolved key {} to value {} using message source {}", key, value, source);
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
    public String translate(LocalizableMessage msg, Locale locale, String defaultMessage) {
        String rv = translate(msg, locale);
        return rv != null ? rv : defaultMessage;
    }

    @Override
    public String translate(LocalizableMessage msg, Locale locale) {
        if (msg == null) {
            return null;
        } else if (msg instanceof SingleLocalizableMessage) {
            return translate((SingleLocalizableMessage) msg, locale);
        } else if (msg instanceof LocalizableMessageList) {
            return translate((LocalizableMessageList) msg, locale);
        } else {
            throw new AssertionError("Unsupported localizable message type: " + msg);
        }
    }

    // todo deduplicate with similar method in WebComponentUtil
    public String translate(LocalizableMessageList msgList, Locale locale) {
        String separator = translateIfPresent(msgList.getSeparator(), locale);
        String prefix = translateIfPresent(msgList.getPrefix(), locale);
        String suffix = translateIfPresent(msgList.getPostfix(), locale);
        return msgList.getMessages().stream()
                .map(m -> translate(m, locale))
                .collect(Collectors.joining(separator, prefix, suffix));
    }

    private String translateIfPresent(LocalizableMessage msg, Locale locale) {
        return msg != null ? translate(msg, locale) : "";
    }

    public String translate(SingleLocalizableMessage msg, Locale locale) {
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
        ResourceBundleMessageSource source = new CachedResourceBundleMessageSource();
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
        String midpointHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);

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
            } else if (param instanceof PolyString) {
                param = translate((PolyString) param, locale, true);
            } else if (param instanceof PolyStringType) {
                param = translate(((PolyStringType) param).toPolyString(), locale, true);
            } else if (param instanceof PolyStringTranslationType) {
                param = translate((PolyStringTranslationType)param, locale);
            } else if (param instanceof PolyStringTranslationArgumentType) {
                param = translate((PolyStringTranslationArgumentType)param, locale);
            }

            translated[i] = param;
        }

        return translated;
    }

    @Override
    public <T extends CommonException> T translate(T e) {
        if (e == null) {
            return null;
        }
        if (e.getUserFriendlyMessage() == null) {
            return e;
        }
        if (e.getTechnicalMessage() == null) {
            e.setTechnicalMessage(translate(e.getUserFriendlyMessage(), Locale.US));
        }
        if (e.getLocalizedUserFriendlyMessage() == null) {
            e.setLocalizedUserFriendlyMessage(translate(e.getUserFriendlyMessage(), Locale.getDefault()));
        }
        return e;
    }

    @Override
    public String translate(PolyString polyString, Locale locale, boolean allowOrig) {
        if (polyString == null) {
            return null;
        }
        if (polyString.getLang() != null) {
            String value = polyString.getLang().get(locale.getLanguage());
            if (value != null) {
                return value;
            }
        }
        if (polyString.getTranslation() != null) {
            String value = translate(polyString.getTranslation(), locale);
            if (value != null){
                return value;
            }
        }
        if (allowOrig) {
            return polyString.getOrig();
        } else {
            return null;
        }
    }

    private String translate(PolyStringTranslationType polyStringTranslation, Locale locale) {
        String key = polyStringTranslation.getKey();
        if (StringUtils.isEmpty(key)) {
            return key;
        }
        List<PolyStringTranslationArgumentType> arguments = polyStringTranslation.getArgument();
        if (arguments == null) {
            return translate(key, null, locale, polyStringTranslation.getFallback());
        } else {
            return translate(key, arguments.toArray(), locale, polyStringTranslation.getFallback());
        }
    }

    private String translate(PolyStringTranslationArgumentType polyStringTranslationArgument, Locale locale) {
        String value = polyStringTranslationArgument.getValue();
        if (value != null) {
            return value;
        }
        PolyStringTranslationType translation = polyStringTranslationArgument.getTranslation();
        if (translation != null) {
            return translate(translation, locale);
        }
        return null;
    }

    @NotNull
    @Override
    public Locale getDefaultLocale() {
        if (overrideLocale == null) {
            return Locale.getDefault();
        } else {
            return overrideLocale;
        }
    }
}
