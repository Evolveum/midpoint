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
package com.evolveum.midpoint.model.impl.filter;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.text.Normalizer;
import java.text.Normalizer.Form;

/**
 * @author lazyman
 */
public class DiacriticsFilter extends AbstractFilter {

    private static final Trace LOGGER = TraceManager.getTrace(DiacriticsFilter.class);

    @Override
    public <T extends Object> PrismPropertyValue<T> apply(PrismPropertyValue<T> propertyValue) {
        Validate.notNull(propertyValue, "Node must not be null.");

        String text = getStringValue(propertyValue);
        if (StringUtils.isEmpty(text)) {
            return propertyValue;
        }

        String newValue = Normalizer.normalize(text, Form.NFD).replaceAll(
                "\\p{InCombiningDiacriticalMarks}+", "");
        propertyValue.setValue((T) newValue);

        return propertyValue;
    }
}
