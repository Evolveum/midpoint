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

    public static final String DEFAULT_UI_INDENT = "&emsp;";

    private static final ToStringStyle DEFAULT_TO_STRING_STYLE = new UserFriendlyToStringStyle();

    /**
     * Whether to show full add object delta (i.e., the whole change) in the delta printer or simple name/oid/type of the object.
     */
    private boolean showFullAddObjectDelta = false;

    private boolean multiline = true;

    private String lineSeparator = DEFAULT_LINE_SEPARATOR;

    private String indentation = DEFAULT_INDENT;

    private String itemSeparator = ", ";

    private String containerSeparatorStart;

    private String containerSeparatorEnd;

    private String collectionSeparatorStart;

    private String collectionSeparatorEnd;

    private boolean showOperationalItems = true;

    private boolean useLocalization = false;

    private boolean showDeltaItemPath = true;

    private ToStringStyle toStringStyle = DEFAULT_TO_STRING_STYLE;

    public boolean showDeltaItemPath() {
        return showDeltaItemPath;
    }

    public UserFriendlyPrettyPrinterOptions showDeltaItemPath(boolean showDeltaItemPath) {
        this.showDeltaItemPath = showDeltaItemPath;
        return this;
    }

    public String indentation() {
        return indentation;
    }

    public UserFriendlyPrettyPrinterOptions defaultUIIndentation() {
        this.indentation = DEFAULT_UI_INDENT;
        return this;
    }

    public UserFriendlyPrettyPrinterOptions indentation(String indentation) {
        this.indentation = indentation;
        return this;
    }

    public boolean showFullAddObjectDelta() {
        return showFullAddObjectDelta;
    }

    public UserFriendlyPrettyPrinterOptions showFullAddObjectDelta(boolean showFullAddObjectDelta) {
        this.showFullAddObjectDelta = showFullAddObjectDelta;
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

    public String containerSeparatorEnd() {
        return containerSeparatorEnd;
    }

    public UserFriendlyPrettyPrinterOptions containerSeparatorEnd(String containerSeparatorEnd) {
        this.containerSeparatorEnd = containerSeparatorEnd;
        return this;
    }

    public String containerSeparatorStart() {
        return containerSeparatorStart;
    }

    public UserFriendlyPrettyPrinterOptions containerSeparatorStart(String containerSeparatorStart) {
        this.containerSeparatorStart = containerSeparatorStart;
        return this;
    }

    public String collectionSeparatorEnd() {
        return collectionSeparatorEnd;
    }

    public UserFriendlyPrettyPrinterOptions collectionSeparatorEnd(String collectionSeparatorEnd) {
        this.collectionSeparatorEnd = collectionSeparatorEnd;
        return this;
    }

    public String collectionSeparatorStart() {
        return collectionSeparatorStart;
    }

    public UserFriendlyPrettyPrinterOptions collectionSeparatorStart(String collectionSeparatorStart) {
        this.collectionSeparatorStart = collectionSeparatorStart;
        return this;
    }

    public String itemSeparator() {
        return itemSeparator;
    }

    public UserFriendlyPrettyPrinterOptions itemSeparator(String itemSeparator) {
        this.itemSeparator = itemSeparator;
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
