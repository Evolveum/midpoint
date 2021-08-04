/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.state.ActivityState;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType.F_REPORT_DATA_REF;

import static java.util.Objects.requireNonNull;

/**
 * Contains common functionality for both activity executions (data creation + data aggregation).
 * This is an experiment - using object composition instead of inheritance.
 *
 * TODO better name
 */
class ActivityImportSupport extends AbstractReportActivitySupport {

    @NotNull private final Activity<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> activity;

    /**
     * Resolved report data object.
     */
    private ReportDataType reportData;

    ActivityImportSupport(
            ExecutionInstantiationContext<ClassicReportImportWorkDefinition, ClassicReportImportActivityHandler> context) {
        super(context);
        activity = context.getActivity();
    }

    @Override
    void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        super.initializeExecution(result);
        setupReportDataObject(result);
    }

    private void setupReportDataObject(OperationResult result) throws CommonException {
        @NotNull ClassicReportImportWorkDefinition workDefinition = activity.getWorkDefinition();
        reportData = getObjectResolver().resolve(workDefinition.getReportDataRef(), ReportDataType.class,
                null, "resolving report data", runningTask, result);
    }

    @Override
    protected ObjectResolver getObjectResolver() {
        return activity.getHandler().objectResolver;
    }

    @Override
    protected AbstractReportWorkDefinition getWorkDefinition() {
        return activity.getWorkDefinition();
    }

    @Override
    protected ReportServiceImpl getReportService() {
        return activity.getHandler().reportService;
    }

    public ReportDataType getReportData() {
        return reportData;
    }

    boolean existImportScript() {
        return getReport().getBehavior() != null && getReport().getBehavior().getImportScript() != null;
    }

    boolean existCollectionConfiguration() {
        return getReport().getObjectCollection() != null;
    }
}
