/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.common.activity;

import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.jetbrains.annotations.NotNull;

/**
 * Notifies external observers about "report created" events.
 *
 * Preliminary implementation.
 */
@Experimental
public interface ReportOutputCreatedListener {

    /**
     * Called when a report output is created.
     */
    void onReportOutputCreated(
            @NotNull AbstractActivityRun<?, ?, ?> activityRun,
            @NotNull ReportType report,
            @NotNull ReportDataType reportOutput,
            @NotNull Task task,
            @NotNull OperationResult result);
}
