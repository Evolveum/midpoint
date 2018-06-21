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

import com.evolveum.midpoint.common.filter.Filter;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Igor Farinic
 */
public abstract class AbstractFilter implements Filter {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractFilter.class);
    private List<Object> parameters;

    @Override
    public List<Object> getParameters() {
        if (parameters == null) {
            parameters = new ArrayList<>();
        }
        return parameters;
    }

    @Override
    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    protected <T extends Object> String getStringValue(PrismPropertyValue<T> propertyValue) {
        Object value = propertyValue.getValue();
        String text = null;
        if (value != null) {
            if (value instanceof String) {
                text = (String) value;
            } else {
                LOGGER.warn("Trying to get String value from value that is not String type but '"
                        + value.getClass() + "'.");
            }
        }

        return text;
    }
}
