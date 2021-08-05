/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.fileformat.FileFormatController;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataWriter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.cxf.databinding.DataWriter;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.EXPORT;

import static java.util.Objects.requireNonNull;

/**
 * Contains common functionality for export activity executions.
 * This is an experiment - using object composition instead of inheritance.
 */
class ActivityExportSupport {

    private static final Trace LOGGER = TraceManager.getTrace(ActivityExportSupport.class);

    private static final String OP_CREATE_REPORT_DATA = ActivityExportSupport.class.getName() + "createReportData";

    @NotNull protected final RunningTask runningTask;
    @NotNull protected final CommonTaskBeans beans;
    @NotNull protected final ReportServiceImpl reportService;
    @NotNull protected final ObjectResolver resolver;
    @NotNull protected final AbstractReportWorkDefinition workDefinition;

    /**
     * Resolved report object.
     */
    protected ReportType report;

    /**
     * Compiled final collection from more collections and archetypes related to object type.
     */
    private CompiledObjectCollectionView compiledView;

    /**
     * Used to derive the file path and to save report data.
     *
     * TODO remove dependency on this class
     */
    private FileFormatController fileFormatController;

    /**
     * File to which the aggregated data are stored.
     */
    private String aggregatedFilePath;

    private SaveReportFileSupport saveSupport;

    ActivityExportSupport(ExecutionInstantiationContext context, ReportServiceImpl reportService,
            ObjectResolver resolver, AbstractReportWorkDefinition workDefinition) {
        runningTask = context.getTaskExecution().getRunningTask();
        beans = context.getTaskExecution().getBeans();
        this.reportService = reportService;
        this.resolver = resolver;
        this.workDefinition = workDefinition;
    }

    void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        setupReportObject(result);
        setupSaveSupport(result);
    }

    private void setupSaveSupport(OperationResult result) throws CommonException {
        saveSupport = new SaveReportFileSupport(report, runningTask, getCompiledCollectionView(result), reportService);
        saveSupport.initializeExecution(result);
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

    public void stateCheck(OperationResult result) throws CommonException {
        MiscUtil.stateCheck(getDirection(report) == EXPORT, "Only report export are supported here");
        MiscUtil.stateCheck(report.getObjectCollection() != null, "Only collection-based reports are supported here");

        if (!reportService.isAuthorizedToRunReport(report.asPrismObject(), runningTask, result)) {
            LOGGER.error("Task {} is not authorized to run report {}", runningTask, report);
            throw new SecurityViolationException("Not authorized");
        }
    }

    public void saveReportFile(String aggregatedData, ReportDataWriter dataWriter, OperationResult result) throws CommonException {
        saveSupport.saveReportFile(aggregatedData, dataWriter, result);
    }

    public void saveReportFile(ReportDataWriter dataWriter, OperationResult result) throws CommonException {
        saveSupport.saveReportFile(dataWriter.getStringData(), dataWriter, result);
    }
}
