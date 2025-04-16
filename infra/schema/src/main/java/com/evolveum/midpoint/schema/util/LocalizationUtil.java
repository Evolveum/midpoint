/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageArgumentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageListType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SingleLocalizableMessageType;

public class LocalizationUtil {

    public static final Trace LOGGER = TraceManager.getTrace(LocalizationUtil.class);

    public static LocalizableMessageType createLocalizableMessageType(LocalizableMessage message) {
        return createLocalizableMessageType(message, null);
    }

    public static LocalizableMessageType createLocalizableMessageType(LocalizableMessage message, Function<LocalizableMessage, String> resolveKeys) {
        if (message == null) {
            return null;
        } else if (message instanceof SingleLocalizableMessage) {
            return createLocalizableMessageType((SingleLocalizableMessage) message, resolveKeys);
        } else if (message instanceof LocalizableMessageList) {
            return createLocalizableMessageType((LocalizableMessageList) message, resolveKeys);
        } else {
            throw new AssertionError("Unsupported localizable message type: " + message);
        }
    }

    @NotNull
    private static LocalizableMessageListType createLocalizableMessageType(@NotNull LocalizableMessageList messageList, Function<LocalizableMessage, String> resolveKeys) {
        LocalizableMessageListType rv = new LocalizableMessageListType();
        messageList.getMessages().forEach(message -> {
            if (rv.getMessage() != null) {rv.getMessage().add(createLocalizableMessageType(message, resolveKeys));}
        });
        rv.setSeparator(createLocalizableMessageType(messageList.getSeparator(), resolveKeys));
        rv.setPrefix(createLocalizableMessageType(messageList.getPrefix(), resolveKeys));
        rv.setPostfix(createLocalizableMessageType(messageList.getPostfix(), resolveKeys));
        return rv;
    }

    @NotNull
    private static SingleLocalizableMessageType createLocalizableMessageType(@NotNull SingleLocalizableMessage message, Function<LocalizableMessage, String> resolveKeys) {
        SingleLocalizableMessageType rv = new SingleLocalizableMessageType();
        rv.setKey(message.getKey());
        if (message.getArgs() != null) {
            for (Object argument : message.getArgs()) {
                LocalizableMessageArgumentType messageArgument;
                if (argument instanceof LocalizableMessage) {
                    messageArgument = new LocalizableMessageArgumentType()
                            .localizable(createLocalizableMessageType(((LocalizableMessage) argument), resolveKeys));
                } else {
                    messageArgument = new LocalizableMessageArgumentType().value(argument != null ? argument.toString() : null);
                }
                rv.getArgument().add(messageArgument);
            }
        }
        if (message.getFallbackLocalizableMessage() != null) {
            rv.setFallbackLocalizableMessage(createLocalizableMessageType(message.getFallbackLocalizableMessage(), resolveKeys));
        }

        if (StringUtils.isBlank(message.getFallbackMessage()) && resolveKeys != null) {
            rv.setFallbackMessage(resolveKeys.apply(message));
        } else {
            rv.setFallbackMessage(message.getFallbackMessage());
        }
        return rv;
    }

    public static LocalizableMessageType createForFallbackMessage(String fallbackMessage) {
        return new SingleLocalizableMessageType().fallbackMessage(fallbackMessage);
    }

    public static LocalizableMessageType createForKey(String key) {
        return new SingleLocalizableMessageType().key(key);
    }

    public static LocalizableMessage toLocalizableMessage(@NotNull LocalizableMessageType message) {
        if (message instanceof SingleLocalizableMessageType) {
            return toLocalizableMessage((SingleLocalizableMessageType) message);
        } else if (message instanceof LocalizableMessageListType) {
            return toLocalizableMessage((LocalizableMessageListType) message);
        } else {
            throw new AssertionError("Unknown LocalizableMessageType type: " + message);
        }
    }

    public static LocalizableMessage toLocalizableMessage(@NotNull SingleLocalizableMessageType message) {
        LocalizableMessage fallbackLocalizableMessage;
        if (message.getFallbackLocalizableMessage() != null) {
            fallbackLocalizableMessage = toLocalizableMessage(message.getFallbackLocalizableMessage());
        } else {
            fallbackLocalizableMessage = null;
        }
        if (message.getKey() == null && message.getFallbackMessage() == null) {
            return fallbackLocalizableMessage;
        } else {
            return new LocalizableMessageBuilder()
                    .key(message.getKey())
                    .args(convertLocalizableMessageArguments(message.getArgument()))
                    .fallbackMessage(message.getFallbackMessage())
                    .fallbackLocalizableMessage(fallbackLocalizableMessage)
                    .build();
        }
    }

    private static LocalizableMessage toLocalizableMessage(@NotNull LocalizableMessageListType messageList) {
        LocalizableMessageListBuilder builder = new LocalizableMessageListBuilder();
        for (LocalizableMessageType m : messageList.getMessage()) {
            builder.addMessage(toLocalizableMessage(m));
        }

        if (messageList.getSeparator() != null) {
            builder = builder.separator(toLocalizableMessage(messageList.getSeparator()));
        }

        if (messageList.getPostfix() != null) {
            builder = builder.postfix(toLocalizableMessage(messageList.getPostfix()));
        }

        if (messageList.getPrefix() != null) {
            builder = builder.postfix(toLocalizableMessage(messageList.getPostfix()));
        }
        return builder.build();

    }

    private static List<Object> convertLocalizableMessageArguments(List<LocalizableMessageArgumentType> arguments) {
        List<Object> rv = new ArrayList<>();
        for (LocalizableMessageArgumentType argument : arguments) {
            if (argument.getLocalizable() != null) {
                rv.add(toLocalizableMessage(argument.getLocalizable()));
            } else {
                rv.add(argument.getValue());        // may be null
            }
        }
        return rv;
    }

    /**
     * Returns locale object for provided string using {@link LocaleUtils#toLocale(String)}.
     * If the input string is null, or the conversion fails, null is returned.
     */
    public static Locale toLocale(String str) {
        return toLocale(str, null);
    }

    /**
     * Returns locale object for provided string using {@link LocaleUtils#toLocale(String)}.
     * If the input string is null, or the conversion fails, provided defaultLocale is returned.
     */
    public static Locale toLocale(String str, Locale defaultLocale) {
        if (str == null) {
            return defaultLocale;
        }

        try {
            return LocaleUtils.toLocale(str);
        } catch (Exception ex) {
            LOGGER.debug("Error occurred while creating locale object for input '"
                    + str + "': " + ex.getMessage());
        }

        return defaultLocale;
    }

    public static <T extends Enum> String createKeyForEnum(T value) {
        if (value == null) {
            return null;
        }

        return value.getClass().getSimpleName() + "." + value.name();
    }
}
