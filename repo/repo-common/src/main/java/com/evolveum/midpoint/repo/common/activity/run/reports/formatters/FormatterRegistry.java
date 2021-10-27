/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports.formatters;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParamsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.QualifiedItemProcessingOutcomeType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkBucketContentType;

import javax.xml.datatype.XMLGregorianCalendar;

@Experimental
public class FormatterRegistry {

    private static final LinkedHashMap<Class<?>, Formatter> FORMATTERS = new LinkedHashMap<>();

    static {
        FORMATTERS.put(AbstractWorkBucketContentType.class, new BucketContentFormatter());
        FORMATTERS.put(QualifiedItemProcessingOutcomeType.class, new QualifiedOutcomeFormatter());
        FORMATTERS.put(ObjectReferenceType.class, new ObjectReferenceFormatter());
        FORMATTERS.put(ParamsType.class, new ParamsFormatter());
        FORMATTERS.put(XMLGregorianCalendar.class, new XMLGregorianCalendarFormatter());
        FORMATTERS.put(Object.class, new GeneralFormatter());
        FORMATTERS.put(int.class, new GeneralFormatter());
        FORMATTERS.put(long.class, new GeneralFormatter());
        FORMATTERS.put(boolean.class, new GeneralFormatter());
        FORMATTERS.put(float.class, new GeneralFormatter());
        FORMATTERS.put(double.class, new GeneralFormatter());
    }

    public static @NotNull Formatter getFormatterFor(@NotNull ItemDefinition<?> definition) {
        Class<?> type = Objects.requireNonNull(
                PrismContext.get().getSchemaRegistry().determineClassForType(definition.getTypeName()),
                () -> "No class for " + definition);

        return FORMATTERS.entrySet().stream()
                .filter(entry -> entry.getKey().isAssignableFrom(type))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("No formatter for " + definition + " (" + type + ")"));
    }
}
