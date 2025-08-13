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

import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;

import com.evolveum.midpoint.schema.util.task.ActivityPath;
import com.evolveum.midpoint.task.api.RunningTask;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

/**
 * Support for "distributed report export" activity.
 */
class DistributedReportExportActivitySupport extends ExportActivitySupport {

    @NotNull private final Activity<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> activity;

    /**
     * Global report data - point of aggregation. It is created on the very start of the {@link ReportDataCreationActivityRun},
     * see {@link DistributedReportExportActivityHandler#createEmptyAggregatedDataObject(EmbeddedActivity, RunningTask,
     * OperationResult)}.
     */
    private ObjectReferenceType globalReportDataRef;

    DistributedReportExportActivitySupport(AbstractActivityRun<?, ?, ?> activityRun,
            Activity<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> activity) {
        super(activityRun,
                activity.getHandler().reportService,
                activity.getHandler().objectResolver,
                activity.getWorkDefinition());
        this.activity = activity;
    }

    void beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        super.beforeRun(result);
        globalReportDataRef = fetchGlobalReportDataRef(result);
    }

    private @NotNull ObjectReferenceType fetchGlobalReportDataRef(OperationResult result)
            throws SchemaException, ObjectNotFoundException, ActivityRunException {
        ActivityState activityState = getWholeActivityState(activity.getPath().allExceptLast(), runningTask, result);
        ObjectReferenceType globalReportDataRef = activityState.getWorkStateReferenceRealValue(F_REPORT_DATA_REF);
        if (globalReportDataRef == null) {
            throw new ActivityRunException("No global report data reference in " + activityState, FATAL_ERROR, PERMANENT_ERROR);
        }
        return globalReportDataRef;
    }

    /**
     * Should be called only after initialization.
     */
    @NotNull ObjectReferenceType getGlobalReportDataRef() {
        return requireNonNull(globalReportDataRef);
    }

    @Override
    public void stateCheck(OperationResult result) throws CommonException {
        MiscUtil.stateCheck(report.getObjectCollection() != null, "Only collection-based reports are supported here");
        super.stateCheck(result);
    }

    /**
     * Returns the activity state of the whole "distributed export" activity, i.e. parent of both data creation
     * and aggregation sub-activities.
     *
     * @param wholeActivityPath Path to the whole (parent) "distributed export" activity
     */
    static @NotNull ActivityState getWholeActivityState(
            ActivityPath wholeActivityPath, RunningTask runningTask, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return ActivityState.getActivityStateUpwards(
                wholeActivityPath,
                runningTask,
                ReportExportWorkStateType.COMPLEX_TYPE,
                result);
    }
}
