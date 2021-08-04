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
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.FATAL_ERROR;
import static com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus.PERMANENT_ERROR;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType.F_REPORT_DATA_REF;

import static java.util.Objects.requireNonNull;

/**
 * Contains common functionality for all activity executions (export and import).
 * This is an experiment - using object composition instead of inheritance.
 *
 * TODO better name
 */
abstract class AbstractReportActivitySupport {

    @NotNull protected final RunningTask runningTask;
    @NotNull protected final CommonTaskBeans beans;

    /**
     * Resolved report object.
     */
    private ReportType report;

    /**
     * Compiled final collection from more collections and archetypes related to object type.
     */
    private CompiledObjectCollectionView compiledView;

    AbstractReportActivitySupport(
            ExecutionInstantiationContext context) {
        runningTask = context.getTaskExecution().getRunningTask();
        beans = context.getTaskExecution().getBeans();
    }

    void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        setupReportObject(result);
    }

    private void setupReportObject(OperationResult result) throws CommonException {
        AbstractReportWorkDefinition workDefinition = getWorkDefinition();
        report = getObjectResolver().resolve(workDefinition.getReportRef(), ReportType.class,
                null, "resolving report", runningTask, result);
    }

    protected abstract ObjectResolver getObjectResolver();

    protected abstract AbstractReportWorkDefinition getWorkDefinition();

    private void setupCompiledView(OperationResult result) throws CommonException {
        compiledView = getReportService().createCompiledView(report.getObjectCollection(), true, runningTask, result);
    }

    protected abstract ReportServiceImpl getReportService();

    /**
     * Should be called only after initialization.
     */
    public @NotNull ReportType getReport() {
        return requireNonNull(report);
    }

    /**
     * Should be called only after initialization.
     */
    @NotNull CompiledObjectCollectionView getCompiledCollectionView(OperationResult result) throws CommonException {
        if (compiledView == null) {
            setupCompiledView(result);
        }
        return compiledView;
    }
}
