/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import org.jetbrains.annotations.NotNull;

/**
 * Header row for report being exported.
 */
class ExportedReportHeaderColumn {

    /**
     * Label for the header column.
     */
    @NotNull private final String label;

    /**
     * Css class for the header column.
     */
    private final String cssClass;

    /**
     * Css style for the header column.
     */
    private final String cssStyle;

    private ExportedReportHeaderColumn(@NotNull String label, String cssClass, String cssStyle) {
        this.label = label;
        this.cssClass = cssClass;
        this.cssStyle = cssStyle;
    }

    static ExportedReportHeaderColumn fromLabel(String label) {
        return fromLabel(label, null, null);
    }

    static ExportedReportHeaderColumn fromLabel(String label, String cssClass, String cssStyle) {
        return new ExportedReportHeaderColumn(label, cssClass, cssStyle);
    }

    public @NotNull String getLabel() {
        return label;
    }

    public String getCssClass() {
        return cssClass;
    }

    public String getCssStyle() {
        return cssStyle;
    }
}
