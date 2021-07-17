/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller.fileformat;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Header row for report being exported.
 *
 * TODO generalize to HTML, if needed
 */
class ExportedReportHeaderRow {

    /**
     * Labels for the header row.
     */
    @NotNull private final List<String> labels;

    private ExportedReportHeaderRow(@NotNull List<String> labels) {
        this.labels = labels;
    }

    static ExportedReportHeaderRow fromLabels(List<String> labels) {
        return new ExportedReportHeaderRow(labels);
    }

    public @NotNull List<String> getLabels() {
        return labels;
    }
}
