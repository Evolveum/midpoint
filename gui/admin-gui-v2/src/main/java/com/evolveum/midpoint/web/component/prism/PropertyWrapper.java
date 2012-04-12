/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PropertyWrapper implements ItemWrapper, Serializable {

    private PrismProperty property;
    private ValueStatus status;
    private List<ValueWrapper> values;

    public PropertyWrapper(PrismProperty property, ValueStatus status) {
        Validate.notNull(property, "Property must not be null.");
        Validate.notNull(status, "Property status must not be null.");

        this.property = property;
        this.status = status;
    }

    @Override
    public String getDisplayName() {
        return ContainerWrapper.getDisplayNameFromItem(property);
    }

    public ValueStatus getStatus() {
        return status;
    }

    public List<ValueWrapper> getValues() {
        if (values == null) {
            values = createValues();
        }
        return values;
    }

    PrismProperty getProperty() {
        return property;
    }

    private List<ValueWrapper> createValues() {
        List<ValueWrapper> values = new ArrayList<ValueWrapper>();

        for (PrismPropertyValue prismValue : (List<PrismPropertyValue>) property.getValues()) {
            values.add(new ValueWrapper(this, prismValue, ValueStatus.NOT_CHANGED));
        }

        if (values.isEmpty()) {
            values.add(new ValueWrapper(this, new PrismPropertyValue(null), ValueStatus.ADDED));
        }

        return values;
    }
}
