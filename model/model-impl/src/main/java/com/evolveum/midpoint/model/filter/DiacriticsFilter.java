/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.filter;

import com.evolveum.midpoint.prism.PropertyValue;
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
    public <T extends Object> PropertyValue<T> apply(PropertyValue<T> propertyValue) {
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
