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

package com.evolveum.midpoint.web.component.objectform;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ProtectedStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author lazyman
 */
public class PropertyWrapper extends PropertyItem implements Comparable<PropertyWrapper> {

    private PrismProperty property;
    private ValueStatus status;
    private List<PropertyValueWrapper> values;

    public PropertyWrapper(PropertyPath path, PrismProperty property) {
        this(path, property, ValueStatus.NOT_CHANGED);
    }

    public PropertyWrapper(PropertyPath path, PrismProperty property, ValueStatus status) {
        super(path);
        Validate.notNull(property, "Property must not be null.");
        this.property = property;
        this.status = status;
    }

    public PrismProperty getProperty() {
        return property;
    }

    public ValueStatus getStatus() {
        return status;
    }

    public List<PropertyValueWrapper> getPropertyValueWrappers() {
        if (values != null) {
            return values;
        }

        values = new ArrayList<PropertyValueWrapper>();
        for (PrismPropertyValue value : (List<PrismPropertyValue>) property.getValues()) {
            values.add(new PropertyValueWrapper(this, value, ValueStatus.NOT_CHANGED));
        }

        if (values.isEmpty()) {
            Object realValue = null;
            if (ProtectedStringType.COMPLEX_TYPE.equals(property.getDefinition().getValueType())) {
                realValue = new ProtectedStringType();
            }
            values.add(new PropertyValueWrapper(this, new PrismPropertyValue(realValue), ValueStatus.ADDED));
        }

        return values;
    }

    @Override
    public String getDisplayName() {
        PrismPropertyDefinition definition = property.getDefinition();
        String displayName = definition.getDisplayName();
        if (StringUtils.isEmpty(displayName)) {
            displayName = definition.getName().getLocalPart();
        }
        return displayName;
    }

    public int compareTo(PropertyWrapper other) {
        return String.CASE_INSENSITIVE_ORDER.compare(getDisplayName(), other.getDisplayName());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(property.getName().getLocalPart());
        builder.append(", ");
        builder.append(status);
        builder.append("\n");
        for (PropertyValueWrapper wrapper : getPropertyValueWrappers()) {
            builder.append("\t");
            builder.append(wrapper.toString());
            builder.append("\n");
        }

        return builder.toString();
    }

    public void cleanup() {
        Collection<PrismPropertyValue<Object>> valuesToDelete = new ArrayList<PrismPropertyValue<Object>>();

        for (PropertyValueWrapper value : getPropertyValueWrappers()) {
            switch (value.getStatus()) {
                case ADDED:
                    if (!value.hasValueChanged()) {
                        valuesToDelete.add(value.getValue());
                    }
                    break;
                case DELETED:
                    valuesToDelete.add(value.getValue());
                    break;
            }
        }

        getProperty().deleteValues(valuesToDelete);
        values = null;
    }
}
