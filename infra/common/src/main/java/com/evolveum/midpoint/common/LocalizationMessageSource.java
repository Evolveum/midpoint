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

import org.apache.commons.lang.Validate;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;

import java.util.Locale;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LocalizationMessageSource implements MessageSource {

    private LocalizationService localizationService;

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
