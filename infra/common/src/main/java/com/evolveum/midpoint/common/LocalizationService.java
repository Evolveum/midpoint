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

import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.CommonException;

import java.util.Locale;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface LocalizationService {

    String translate(String key, Object[] params, Locale locale);

    String translate(String key, Object[] params, Locale locale, String defaultMessage);

    String translate(LocalizableMessage msg, Locale locale);

    default String translate(LocalizableMessage msg) {
        return translate(msg, Locale.getDefault());
    }

    /**
     * Fills-in message and localizedMessage based on userFriendlyMessage, if needed.
     */
    <T extends CommonException> T translate(T e);
}
