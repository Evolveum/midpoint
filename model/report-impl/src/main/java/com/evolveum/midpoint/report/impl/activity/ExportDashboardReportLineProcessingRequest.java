/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.task.CorrelatableProcessingRequest;
import com.evolveum.midpoint.repo.common.task.GenericProcessingRequest;
import com.evolveum.midpoint.repo.common.task.IterativeActivityExecution;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;

import org.jetbrains.annotations.NotNull;

/**
 * Wrapper for export dashboard report line object.
 *
 * (This is needed for the activity framework to process {@link ExportDashboardReportLine} objects.)
 */
public class ExportDashboardReportLineProcessingRequest
        extends GenericProcessingRequest<ExportDashboardReportLine<Containerable>>
        implements CorrelatableProcessingRequest {

    ExportDashboardReportLineProcessingRequest(@NotNull ExportDashboardReportLine<Containerable> item,
            @NotNull IterativeActivityExecution<ExportDashboardReportLine<Containerable>, ?, ?, ?, ?, ?> activityExecution) {
        super(item.getLineNumber(), item, activityExecution);
    }

    @Override
    public Object getCorrelationValue() {
        return getName();
    }

    @Override
    public @NotNull IterationItemInformation getIterationItemInformation() {
        return new IterationItemInformation(
                getName(),
                null,
                null,
                null);
    }

    private String getName() {
        return "dashboard " + item.getWidgetIdentifier() + "line #" + item.getLineNumber();
    }
}
