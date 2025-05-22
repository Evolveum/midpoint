/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common;

import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;

/**
 * TODO implement use of multiline, lineSeparator, itemSeparators
 */
public class UserFriendlyPrettyPrinterOptions {

    public static final String DEFAULT_LINE_SEPARATOR = "\n";

    public static final String DEFAULT_INDENT = "  ";

    private static final ToStringStyle DEFAULT_TO_STRING_STYLE = new UserFriendlyToStringStyle();

    private boolean multiline = true;

    private String lineSeparator = DEFAULT_LINE_SEPARATOR;

    private String indentation = DEFAULT_INDENT;

    private String itemSeparatorStart = "{";

    private String itemSeparatorEnd = "}";

    private boolean showOperationalItems = true;

    private boolean useLocalization = false;

    private ToStringStyle toStringStyle = DEFAULT_TO_STRING_STYLE;

    public String indentation() {
        return indentation;
    }

    public UserFriendlyPrettyPrinterOptions indentation(String indentation) {
        this.indentation = indentation;
        return this;
    }

    public boolean useLocalization() {
        return useLocalization;
    }

    public UserFriendlyPrettyPrinterOptions useLocalization(boolean useLocalization) {
        this.useLocalization = useLocalization;
        return this;
    }

    public boolean showOperational() {
        return showOperationalItems;
    }

    public UserFriendlyPrettyPrinterOptions showOperational(boolean showOperational) {
        this.showOperationalItems = showOperational;
        return this;
    }

    public String itemSeparatorEnd() {
        return itemSeparatorEnd;
    }

    public UserFriendlyPrettyPrinterOptions itemSeparatorEnd(String itemSeparatorEnd) {
        this.itemSeparatorEnd = itemSeparatorEnd;
        return this;
    }

    public String itemSeparatorStart() {
        return itemSeparatorStart;
    }

    public UserFriendlyPrettyPrinterOptions itemSeparatorStart(String itemSeparatorStart) {
        this.itemSeparatorStart = itemSeparatorStart;
        return this;
    }

    public String lineSeparator() {
        return lineSeparator;
    }

    public UserFriendlyPrettyPrinterOptions lineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    public boolean multiline() {
        return multiline;
    }

    public UserFriendlyPrettyPrinterOptions multiline(boolean multiline) {
        this.multiline = multiline;
        return this;
    }

    public ToStringStyle toStringStyle() {
        return toStringStyle;
    }

    public UserFriendlyPrettyPrinterOptions toStringStyle(@NotNull ToStringStyle toStringStyle) {
        this.toStringStyle = toStringStyle;
        return this;
    }

    private static class UserFriendlyToStringStyle extends ToStringStyle {

        public UserFriendlyToStringStyle() {
            setUseIdentityHashCode(false);
            setUseShortClassName(true);
        }

        @Override
        public void append(StringBuffer buffer, String fieldName, Object value, Boolean fullDetail) {
            if (value != null) {
                super.append(buffer, fieldName, value, fullDetail);
            }
        }

        @Override
        public void append(StringBuffer buffer, String fieldName, Object[] array, Boolean fullDetail) {
            if (array != null) {
                super.append(buffer, fieldName, array, fullDetail);
            }
        }
    }
}
