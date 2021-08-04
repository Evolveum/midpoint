/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType.F_REPORT_DATA_REF;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.task.SearchBasedActivityExecution;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * Contains common functionality for both activity executions (data creation + data aggregation).
 * This is an experiment - using object composition instead of inheritance.
 *
 * TODO better name
 */
class ActivityExecutionSupport {

    @NotNull private final RunningTask runningTask;
    @NotNull private final Activity<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> activity;
    @NotNull private final CommonTaskBeans beans;

    /**
     * Resolved report object.
     */
    private ReportType report;

    /**
     * Global report data - point of aggregation.
     */
    private ObjectReferenceType globalReportDataRef;

    ActivityExecutionSupport(
            SearchBasedActivityExecution<?, DistributedReportExportWorkDefinition,
                    DistributedReportExportActivityHandler, ?> activityExecution) {
        runningTask = activityExecution.getRunningTask();
        activity = activityExecution.getActivity();
        beans = activityExecution.getTaskExecution().getBeans();
    }

    void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        setupReportObject(result);
        globalReportDataRef = fetchGlobalReportDataRef(result);
    }

    private @NotNull ObjectReferenceType fetchGlobalReportDataRef(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ActivityExecutionException {
        ActivityState activityState =
                ActivityState.getActivityStateUpwards(
                        activity.getPath().allExceptLast(),
                        runningTask,
                        ReportExportWorkStateType.COMPLEX_TYPE,
                        beans,
                        result);
        ObjectReferenceType globalReportDataRef = activityState.getWorkStateReferenceRealValue(F_REPORT_DATA_REF);
        if (globalReportDataRef == null) {
            throw new ActivityExecutionException("No global report data reference in " + activityState,
                    FATAL_ERROR, PERMANENT_ERROR);
        }
        return globalReportDataRef;
    }

    private void setupReportObject(OperationResult result) throws CommonException {
        DistributedReportExportWorkDefinition workDefinition = activity.getWorkDefinition();
        report = activity.getHandler().objectResolver.resolve(workDefinition.getReportRef(), ReportType.class,
                null, "resolving report", runningTask, result);
    }

    /**
     * Should be called only after initialization.
     */
    @NotNull ObjectReferenceType getGlobalReportDataRef() {
        return requireNonNull(globalReportDataRef);
    }

    /**
     * Should be called only after initialization.
     */
    public @NotNull ReportType getReport() {
        return requireNonNull(report);
    }
}
