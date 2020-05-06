/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.helpers;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

/**
 * Utility methods related to processing data objects.
 */
public class ScriptingDataUtil {

    public static <T> T getRealValue(Object value, Class<T> clazz) throws SchemaException {
        if (value == null) {
            return null;
        } else if (value instanceof RawType) {
            return ((RawType) value).getParsedRealValue(clazz);
        } else if (value instanceof PrismValue) {
            return getRealValue((PrismValue) value, clazz);
        } else
            return cast(value, clazz);
    }

    public static <T> T getRealValue(PrismValue prismValue, Class<T> clazz) throws SchemaException {
        if (prismValue == null) {
            return null;
        } else {
            Object realValue = prismValue.getRealValue();
            if (realValue == null) {
                throw new SchemaException("Real value of 'null' embedded in " + prismValue);
            } else {
                return cast(realValue, clazz);
            }
        }
    }

    static <T> T cast(Object value, Class<T> expectedClass) throws SchemaException {
        if (value == null) {
            return null;
        } else if (!expectedClass.isAssignableFrom(value.getClass())) {
            throw new SchemaException("Expected '" + expectedClass.getName() + "' but got '" + value.getClass().getName() + "'");
        } else {
            //noinspection unchecked
            return (T) value;
        }
    }
}
