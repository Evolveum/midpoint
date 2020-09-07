/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common;

import java.util.Locale;

import org.apache.commons.lang3.Validate;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LocalizationMessageSource implements MessageSource {

    private final LocalizationService localizationService;

    public LocalizationMessageSource(LocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @Override
    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        String msg = localizationService.translate(code, args, locale);
        if (msg == null) {
            return defaultMessage;
        }

        return msg;
    }

    @Override
    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        String msg = localizationService.translate(code, args, locale);
        if (msg == null) {
            throw new NoSuchMessageException("Message code '" + code + "' was not found");
        }

        return msg;
    }

    @Override
    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        Validate.notNull(resolvable, "Message source resolvable must not be null");

        for (String code : resolvable.getCodes()) {
            String msg = localizationService.translate(code, resolvable.getArguments(), locale);
            if (msg != null) {
                return msg;
            }
        }

        if (resolvable.getDefaultMessage() != null) {
            return resolvable.getDefaultMessage();
        }

        throw new NoSuchMessageException("Can't resolve message: " + resolvable);
    }
}
