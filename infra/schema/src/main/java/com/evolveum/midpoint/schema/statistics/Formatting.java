/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Describes formatting at abstract level.
 */
public abstract class Formatting {

    private static final Trace LOGGER = TraceManager.getTrace(Formatting.class);

    final List<Column> columns = new ArrayList<>();

    String[] getColumnLabelsAsArray() {
        return columns.stream()
                .map(column -> column.label)
                .toArray(String[]::new);
    }

    static class Column {
        final String label;
        final Alignment alignment;
        private final String formatString;
        private final String defaultValue;

        private Column(String label, Alignment alignment, String formatString, String defaultValue) {
            this.label = label;
            this.alignment = alignment;
            this.formatString = formatString;
            this.defaultValue = defaultValue;
        }

        public String format(Object value) {
            if (value == null) {
                return defaultValue;
            } else if (formatString == null) {
                return String.valueOf(value);
            } else {
                try {
                    return String.format(Locale.US, formatString, value);
                } catch (Throwable t) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't apply formatting '{}' to {}", t,
                            formatString, MiscUtil.getValueWithClass(value));
                    return "! " + value;
                }
            }
        }
    }

    void addColumn(String label, Alignment alignment, String formatter) {
        columns.add(new Column(label, alignment, formatter, ""));
    }

    public enum Alignment {
        LEFT, RIGHT
    }

    public boolean isNiceNumbersFormatting() {
        return false;
    }

    public abstract String apply(Data data);
}
