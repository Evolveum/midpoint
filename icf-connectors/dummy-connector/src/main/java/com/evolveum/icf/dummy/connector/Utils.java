/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import java.util.List;
import java.util.Set;

import org.identityconnectors.framework.common.objects.Attribute;
import org.jetbrains.annotations.Nullable;

/**
 * @author lazyman
 * @author Radovan Semancik
 */
public class Utils {

    public static String getMandatoryStringAttribute(Set<Attribute> attributes, String attributeName) {
        String value = getAttributeSingleValue(attributes, attributeName, String.class);
        if (value == null) {
            throw new IllegalArgumentException("No value for mandatory attribute "+attributeName);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAttributeSingleValue(Set<Attribute> attributes, String attributeName, Class<T> type) {
        for (Attribute attr : attributes) {
            if (attributeName.equals(attr.getName())) {
                return getAttributeSingleValue(attr, type);
            }
        }
        return null;
    }

    public static <T> @Nullable T getAttributeSingleValue(Attribute attr, Class<T> type) {
        List<Object> values = attr.getValue();
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalArgumentException("Multiple values for single valued attribute " + attr.getName());
        }
        Object value = values.get(0);
        if (!(type.isAssignableFrom(value.getClass()))) {
            throw new IllegalArgumentException(
                    "Illegal value type %s for attribute %s, expecting type %s".formatted(
                            value.getClass().getName(), attr.getName(), type.getName()));
        }
        //noinspection unchecked
        return (T) value;
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNullArgument(Object object, String arg) {
        notNull(object, "Argument '" + arg + "' can't be null.");
    }

    public static void notEmpty(String value, String message) {
        notNull(value, message);

        if (value.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notEmptyArgument(String value, String arg) {
        notEmpty(value, "Argument '" + arg + "' can't be empty.");
    }
}
