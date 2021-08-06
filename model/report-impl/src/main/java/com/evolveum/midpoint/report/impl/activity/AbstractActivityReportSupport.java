/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.EXPORT;

import static java.util.Objects.requireNonNull;

/**
 * Contains common functionality for export activity executions.
 * This is an experiment - using object composition instead of inheritance.
 */
class ActivityReportSupport {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityReportSupport.class);

    @NotNull protected final RunningTask runningTask;
    @NotNull protected final CommonTaskBeans beans;
    @NotNull protected final ReportServiceImpl reportService;
    @NotNull protected final ObjectResolver resolver;
    @NotNull protected final AbstractReportWorkDefinition workDefinition;
    @NotNull protected final AuditService auditService;
    @NotNull protected final ModelService modelService;

    /**
     * Resolved report object.
     */
    protected ReportType report;

    /**
     * Compiled final collection from more collections and archetypes related to object type.
     */
    private CompiledObjectCollectionView compiledView;

    ActivityReportSupport(ExecutionInstantiationContext context, ReportServiceImpl reportService,
                                  ObjectResolver resolver, AbstractReportWorkDefinition workDefinition) {
        runningTask = context.getTaskExecution().getRunningTask();
        beans = context.getTaskExecution().getBeans();
        this.reportService = reportService;
        this.resolver = resolver;
        this.workDefinition = workDefinition;
        auditService = reportService.getAuditService();
        modelService = reportService.getModelService();
    }

    void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
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
     * Check actual report for different report handlers.
     */
    public void stateCheck(OperationResult result) throws CommonException {
        MiscUtil.stateCheck(getDirection(report) == EXPORT, "Only report export are supported here");
        MiscUtil.stateCheck(report.getObjectCollection() != null, "Only collection-based reports are supported here");

        if (!reportService.isAuthorizedToRunReport(report.asPrismObject(), runningTask, result)) {
            LOGGER.error("Task {} is not authorized to run report {}", runningTask, report);
            throw new SecurityViolationException("Not authorized");
        }
    }
}
