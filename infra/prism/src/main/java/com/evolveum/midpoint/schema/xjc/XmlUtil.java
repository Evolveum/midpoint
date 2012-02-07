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

package com.evolveum.midpoint.schema.xjc;

import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyValue;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author lazyman
 */
public final class XmlUtil {

    private XmlUtil() {
    }

    public static <T> List<T> getPropertyValues(PropertyContainer container, QName name, Class<T> clazz) {
        List<T> values = new ArrayList<T>();
        Property property = container.findProperty(name);
        if (property == null) {
            return values;
        }

        Set<PropertyValue<T>> set = property.getValues(clazz);
        if (set != null) {
            for (PropertyValue<T> value : set) {
                values.add(value.getValue());
            }
        }

        return values;
    }

    public static <T> T getPropertyValue(PropertyContainer container, QName name) {
        Property property = container.findProperty(name);
        if (property == null) {
            return null;
        }

        PropertyValue<Object> value = property.getValue();
        if (value == null) {
            return null;
        }

        return (T) value.getValue();
    }

    public static <T> void setPropertyValue(PropertyContainer container, QName name, T value) {
        Property property = container.findOrCreateProperty(name, value.getClass());
        property.setValue(new PropertyValue(value));
    }
}
