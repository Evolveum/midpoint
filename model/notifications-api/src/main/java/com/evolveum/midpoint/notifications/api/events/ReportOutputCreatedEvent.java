/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.reports.ReportSupportUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.Nullable;

/**
 * Signals that a report output has been created.
 */
@Experimental
public interface ReportOutputCreatedEvent extends Event {

    /** The report definition. */
    @NotNull ReportType getReport();

    /** Name of the report. */
    default @NotNull String getReportName() {
        ReportType report = getReport();
        PolyStringType namePoly = MiscUtil.requireNonNull(
                report.getName(), () -> new IllegalStateException("Unnamed report: " + report));
        return MiscUtil.requireNonNull(
                namePoly.getOrig(), () -> new IllegalStateException("Report name without 'orig' component: " + report));
    }

    /** The report data (output) object. */
    @NotNull ReportDataType getReportData();

    /** The content type of the report output. */
    default @NotNull String getContentType() {
        return ReportSupportUtil.getContentType(
                getReportData());
    }

    /** Path to the file containing the report output. Normally it should be non-null. */
    default String getFilePath() {
        return getReportData().getFilePath();
    }

    /**
     * Activity run in which the report was created. Normally it should be non-null, as currently all the reports
     * are created within activities.
     */
    @Nullable AbstractActivityRun<?, ?, ?> getActivityRun();
}
