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

import static com.evolveum.midpoint.util.MiscUtil.castSafely;

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
            return castSafely(value, clazz);
    }

    public static <T> T getRealValue(PrismValue prismValue, Class<T> clazz) throws SchemaException {
        if (prismValue == null) {
            return null;
        } else {
            Object realValue = prismValue.getRealValue();
            if (realValue == null) {
                throw new SchemaException("Real value of 'null' embedded in " + prismValue);
            } else {
                return castSafely(realValue, clazz);
            }
        }
    }

}
