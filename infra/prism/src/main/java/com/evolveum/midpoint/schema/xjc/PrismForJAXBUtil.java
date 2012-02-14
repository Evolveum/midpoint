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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author lazyman
 */
public final class PrismForJAXBUtil {

    private PrismForJAXBUtil() {
    }

    public static <T> List<T> getPropertyValues(PrismContainer container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        //PrismProperty property = container.findOrCreateProperty(name, clazz);
        PrismProperty property = container.findOrCreateProperty(name);
        return new PropertyArrayList<T>(property);

//        List<T> values = new ArrayList<T>();
//        PrismProperty property = container.findProperty(name);
//        if (property == null) {
//            return values;
//        }
//
//        Set<PropertyValue<T>> set = property.getValues(clazz);
//        if (set != null) {
//            for (PropertyValue<T> value : set) {
//                values.add(value.getValue());
//            }
//        }
//
//        return values;
    }

    public static <T> T getPropertyValue(PrismContainerValue container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findProperty(name);
        return getPropertyValue(property, clazz);
    }

    public static <T> T getPropertyValue(PrismContainer container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findProperty(name);
        return getPropertyValue(property, clazz);
    }

    private static <T> T getPropertyValue(PrismProperty property, Class<T> clazz) {
        if (property == null) {
            return null;
        }

        PrismPropertyValue<Object> value = property.getValue();
        if (value == null) {
            return null;
        }

        return (T) value.getValue();
    }

    public static <T> void setPropertyValue(PrismContainerValue container, QName name, T value) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismProperty property = container.findOrCreateProperty(name);
        property.setValue(new PrismPropertyValue(value));
    }

    public static <T> void setPropertyValue(PrismContainer container, QName name, T value) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismProperty property = container.findOrCreateProperty(name);
        property.setValue(new PrismPropertyValue(value));
    }
}
