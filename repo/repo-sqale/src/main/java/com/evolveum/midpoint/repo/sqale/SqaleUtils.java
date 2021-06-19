/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.lang.reflect.Field;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class SqaleUtils {

    /**
     * Returns version from midPoint object as a number.
     *
     * @throws IllegalArgumentException if the version is null or non-number
     */
    public static int objectVersionAsInt(ObjectType schemaObject) {
        String version = schemaObject.getVersion();
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Version must be a number: " + version);
        }
    }

    /**
     * Returns version from prism object as a number.
     *
     * @throws IllegalArgumentException if the version is null or non-number
     */
    public static int objectVersionAsInt(PrismObject<?> prismObject) {
        String version = prismObject.getVersion();
        try {
            return Integer.parseInt(version);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Version must be a number: " + version);
        }
    }

    /** Parametrized type friendly version of {@link Object#getClass()}. */
    public static <S> Class<S> getClass(S object) {
        //noinspection unchecked
        return (Class<S>) object.getClass();
    }

    public static String toString(Object object) {
        return new ToStringUtil(object).toString();
    }

    public static boolean isEnumDefinition(PrismPropertyDefinition<?> definition) {
        Collection<? extends DisplayableValue<?>> allowedValues = definition.getAllowedValues();
        return allowedValues != null && !allowedValues.isEmpty();
    }

    public static String extensionDateTime(@NotNull XMLGregorianCalendar dateTime) {
        //noinspection ConstantConditions
        return MiscUtil.asInstant(dateTime)
                .truncatedTo(ChronoUnit.MILLIS)
                .toString();
    }

    private static class ToStringUtil extends ReflectionToStringBuilder {

        @SuppressWarnings("DoubleBraceInitialization")
        private static final ToStringStyle STYLE = new ToStringStyle() {{
            setFieldSeparator(", ");
            setUseShortClassName(true);
            setUseIdentityHashCode(false);
        }};

        private ToStringUtil(Object object) {
            super(object, STYLE);
        }

        @Override
        protected boolean accept(Field field) {
            try {
                return super.accept(field) && field.get(getObject()) != null;
            } catch (IllegalAccessException e) {
                return super.accept(field);
            }
        }
    }
}
