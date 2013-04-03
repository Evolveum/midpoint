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
            parameters = new ArrayList<Object>();
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
