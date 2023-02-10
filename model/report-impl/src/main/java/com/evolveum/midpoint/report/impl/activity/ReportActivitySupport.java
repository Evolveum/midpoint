/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.model.api.ModelAuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.EXPORT;

import static java.util.Objects.requireNonNull;

/**
 * Contains common functionality for export activity executions.
 * This is an experiment - using object composition instead of inheritance.
 */
class ReportActivitySupport {

    private static final Trace LOGGER = TraceManager.getTrace(ReportActivitySupport.class);

    @NotNull protected final AbstractActivityRun<?, ?, ?> activityRun;
    @NotNull protected final RunningTask runningTask;
    @NotNull protected final CommonTaskBeans beans;
    @NotNull protected final ReportServiceImpl reportService;
    @NotNull protected final ObjectResolver resolver;
    @NotNull protected final AbstractReportWorkDefinition workDefinition;
    @NotNull protected final ModelAuditService modelAuditService;
    @NotNull protected final ModelService modelService;

    /** Resolved report object. */
    protected ReportType report;

    /** Compiled final collection from more collections and archetypes related to object type. */
    private CompiledObjectCollectionView compiledView;

    ReportActivitySupport(AbstractActivityRun<?, ?, ?> activityRun, ReportServiceImpl reportService,
            @NotNull ObjectResolver resolver, @NotNull AbstractReportWorkDefinition workDefinition) {
        this.activityRun = activityRun;
        this.runningTask = activityRun.getRunningTask();
        this.beans = activityRun.getBeans();
        this.reportService = reportService;
        this.resolver = resolver;
        this.workDefinition = workDefinition;
        modelAuditService = reportService.getModelAuditService();
        modelService = reportService.getModelService();
    }

    void beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        setupReportObject(result);
    }

    private void setupReportObject(OperationResult result) throws CommonException {
        report = resolver.resolve(workDefinition.getReportRef(), ReportType.class,
                null, "resolving report", runningTask, result);
    }

    /**
     * Should be called only after initialization.
     */
    public @NotNull ReportType getReport() {
        return requireNonNull(report);
    }

    /**
     * Should be called only after initialization.
     */
    public @NotNull ObjectReferenceType getReportRef() {
        return ObjectTypeUtil.createObjectRef(getReport());
    }

    public ReportParameterType getReportParameters() {
        return workDefinition.getReportParams();
    }

    @NotNull CompiledObjectCollectionView getCompiledCollectionView(OperationResult result) throws CommonException {
        if (compiledView == null) {
            setupCompiledView(result);
        }
        return compiledView;
    }

    private void setupCompiledView(OperationResult result) throws CommonException {
        compiledView = reportService.createCompiledView(report.getObjectCollection(), true, runningTask, result);
    }

    /**
     * Check actual report for different report activity execution.
     */
    public void stateCheck(OperationResult result) throws CommonException {
        MiscUtil.stateCheck(getDirection(report) == EXPORT, "Only report export are supported here");

        if (!reportService.isAuthorizedToRunReport(report.asPrismObject(), runningTask, result)) {
            LOGGER.error("Task {} is not authorized to run report {}", runningTask, report);
            throw new SecurityViolationException("Not authorized");
        }
    }
}
