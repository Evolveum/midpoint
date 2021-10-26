/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports.formatters;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Collection;
import java.util.List;

/**
 * Formatter for {@link XMLGregorianCalendar} objects.
 */
public class XMLGregorianCalendarFormatter implements Formatter {

    @Override
    public @NotNull List<String> formatHeader(@NotNull ItemDefinition<?> def) {
        String name = def.getItemName().getLocalPart();
        return List.of(name, name + "-millis");
    }

    @Override
    public @NotNull List<String> formatValue(Object v) {
        if (!(v instanceof XMLGregorianCalendar)) {
            return List.of("", "");
        } else {
            XMLGregorianCalendar value = (XMLGregorianCalendar) v;
            return List.of(
                    String.valueOf(value),
                    String.valueOf(XmlTypeConverter.toMillis(value)));
        }
    }

    @Override
    public @NotNull List<String> formatMultiValue(Collection<?> values) {
        return List.of(
                String.format("%d values?", values.size()));
    }
}
