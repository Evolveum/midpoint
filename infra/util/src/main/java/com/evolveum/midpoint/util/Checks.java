/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util;

import org.springframework.lang.NonNull;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.google.common.base.Strings;

public class Checks {

    private Checks() {
        throw new UnsupportedOperationException("Utility class");
    }


    /**
     * Throws SchemaException if test failed.
     *
     * @param test test
     * @param template String template for formatting arguments see {@link Strings#lenientFormat(String, Object...)} for formatting.
     * @param info Arguments for exception message
     * @throws SchemaException Throws exception with formatted string if test is false
     */
    public static final void checkSchema(boolean test, String template, Object... info) throws SchemaException {
        if (!test) {
            throw new SchemaException(Strings.lenientFormat(template, info));
        }
    }

    /**
     *
     * @param <T> Class of object
     * @param obj object to be tested for null
     * @param template String template for formatting arguments see {@link Strings#lenientFormat(String, Object...)} for formatting.
     * @param info Arguments for exception message
     * @return Object if not null
     * @throws SchemaException
     */
    @NonNull
    public static final <T> T checkSchemaNotNull(T obj, String template, Object... info) throws SchemaException {
        if (obj == null) {
            throw new SchemaException(Strings.lenientFormat(template, info));
        }
        return obj;
    }
}
