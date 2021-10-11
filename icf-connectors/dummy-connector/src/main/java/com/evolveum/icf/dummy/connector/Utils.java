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
                List<Object> values = attr.getValue();
                if (values == null || values.isEmpty()) {
                    return null;
                }
                if (values.size()>1) {
                    throw new IllegalArgumentException("Multiple values for single valued attribute "+attributeName);
                }
                if (!(type.isAssignableFrom(values.get(0).getClass()))) {
                    throw new IllegalArgumentException("Illegal value type "+values.get(0).getClass().getName()+" for attribute "+attributeName+", expecting type "+type.getClass().getName());
                }
                return (T)values.get(0);
            }
        }
        return null;
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
