/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.EXPORT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.task.CommonTaskBeans;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.controller.fileformat.FileFormatController;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataWriter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * Contains common functionality for executions of report-related activities.
 * This is an experiment - using object composition instead of inheritance.
 */
class ReportActivitySupport {

    private static final Trace LOGGER = TraceManager.getTrace(ReportActivitySupport.class);

    private static final String OP_CREATE_REPORT_DATA = ReportActivitySupport.class.getName() + "createReportData";

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

    ReportActivitySupport(AbstractActivityExecution<?, ?, ?> activityExecution, ReportServiceImpl reportService,
            @NotNull ObjectResolver resolver, @NotNull AbstractReportWorkDefinition workDefinition) {
        this.runningTask = activityExecution.getTaskExecution().getRunningTask();
        this.beans = activityExecution.getTaskExecution().getBeans();
        this.reportService = reportService;
        this.resolver = resolver;
        this.workDefinition = workDefinition;
        this.auditService = reportService.getAuditService();
        this.modelService = reportService.getModelService();
    }

    void beforeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
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

    /**
     * Save exported report to a file.
     */
    public void saveReportFile(String aggregatedData, ReportDataWriter dataWriter, OperationResult result) throws CommonException {
        saveSupport.saveReportFile(aggregatedData, dataWriter, result);
    }

    public void saveReportFile(ReportDataWriter dataWriter, OperationResult result) throws CommonException {
        saveSupport.saveReportFile(dataWriter.getStringData(), dataWriter, result);
    }

    /**
     * Search container objects for iterative task.
     * Temporary until will be implemented iterative search for audit records and containerable objects.
     */
    public List<? extends Containerable> searchRecords(Class<? extends Containerable> type,
            ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws CommonException {
        if (AuditEventRecordType.class.equals(type)) {
            @NotNull SearchResultList<AuditEventRecordType> auditRecords = auditService.searchObjects(query, options, result);
            return auditRecords.getList();
        } else if (ObjectType.class.isAssignableFrom(type)) {
            SearchResultList<PrismObject<ObjectType>> results = modelService.searchObjects((Class<ObjectType>) type, query, options, runningTask, result);
            List list = new ArrayList<Containerable>();
            results.forEach(object -> list.add(object.asObjectable()));
            return list;
        } else {
            SearchResultList<? extends Containerable> containers = modelService.searchContainers(type, query, options, runningTask, result);
            return containers.getList();
        }
    }
}
