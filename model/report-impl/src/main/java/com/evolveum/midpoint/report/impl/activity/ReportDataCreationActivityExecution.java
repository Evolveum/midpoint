/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.util.MiscUtil.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.EXPORT;

import java.util.Collection;
import java.util.Objects;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.report.impl.controller.*;

import com.evolveum.midpoint.repo.common.task.*;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Executes parts of distributed report data creation activity:
 *
 * 1. issues repo search based on data from export controller,
 * 2. processes objects found by feeding them into the export controller,
 * 3. finally, instructs the controller to write the (potentially partial) report.
 */
public class ReportDataCreationActivityExecution
        extends SearchBasedActivityExecution
        <Containerable, DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler, ReportExportWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ReportDataCreationActivityExecution.class);

    /**
     * Execution object (~ controller) that is used to translate objects found into report data.
     * Initialized on the activity execution start.
     */
    private CollectionDistributedExportController<Containerable> controller;

    /**
     * This is "master" search specification, derived from the report.
     * It is then narrowed down using buckets by the activity framework.
     */
    private SearchSpecification<Containerable> masterSearchSpecification;

    @NotNull private final DistributedReportExportActivitySupport support;

    /** The report service Spring bean. */
    @NotNull private final ReportServiceImpl reportService;

    /**
     * Segmentation of bucketed audit report based on time from/to.
     */
    private AuditReportSegmentation auditReportSegmentation;

    ReportDataCreationActivityExecution(
            @NotNull ExecutionInstantiationContext<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> context) {
        super(context, "Report data creation");
        reportService = getActivity().getHandler().reportService;
        support = new DistributedReportExportActivitySupport(this, getActivity());
    }

    /**
     * Called at the beginning of execution of this activity (potentially in a worker task).
     * Here is the place to pre-process the report definition.
     */
    @Override
    public void beforeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.beforeExecution(result);
        initializeController(result);
    }

    private void initializeController(OperationResult result) throws CommonException {
        RunningTask task = getRunningTask();

        ReportType report = support.getReport();

        if (!getActivityHandler().reportService.isAuthorizedToRunReport(report.asPrismObject(), task, result)) {
            LOGGER.error("Task {} is not authorized to run report {}", task, report);
            throw new SecurityViolationException("Not authorized");
        }

        stateCheck(getDirection(report) == EXPORT, "Only report exports are supported here");
        stateCheck(report.getObjectCollection() != null, "Only collection-based reports are supported here");

        SearchSpecificationHolder searchSpecificationHolder = new SearchSpecificationHolder();
        ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter = ReportUtils.createDataWriter(
                report, FileFormatTypeType.CSV, getActivityHandler().reportService, support.getCompiledCollectionView(result));
        controller = new CollectionDistributedExportController<>(
                searchSpecificationHolder,
                dataWriter,
                report,
                support.getGlobalReportDataRef(),
                reportService,
                support.getCompiledCollectionView(result),
                support.getReportParameters());

        controller.initialize(task, result);

        stateCheck(searchSpecificationHolder.searchSpecification != null, "No search specification was provided");
        masterSearchSpecification = searchSpecificationHolder.searchSpecification;

        if (AuditEventRecordType.class.equals(masterSearchSpecification.getType())) {
            initializeAuditReportBucketing(result);
        }
    }

    private void initializeAuditReportBucketing(OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        @NotNull XMLGregorianCalendar reportFrom = XmlTypeConverter.createXMLGregorianCalendar("2020-01-01T00:00:00.000+02:00");
        @NotNull XMLGregorianCalendar reportTo = Objects.requireNonNull(
                activityState.getRealizationStartTimestamp(),
                "no realization start timestamp for " + this);
        auditReportSegmentation = new AuditReportSegmentation(reportFrom, reportTo);
    }

    /**
     * Report exports are very special beasts. They are not configured using traditional `ObjectSetType` beans
     * but they use collection-based configuration instead. However, even that complex configurations must boil
     * down to simple search specification - and this is done exactly in this method.
     */
    @Override
    public @NotNull SearchSpecification<Containerable> createCustomSearchSpecification(OperationResult result) {
        return masterSearchSpecification.clone();
    }

    @Override
    public boolean processItem(@NotNull Containerable item,
            @NotNull ItemProcessingRequest<Containerable> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException {
        controller.handleDataRecord(request.getSequentialNumber(), item, workerTask, result);
        return true;
    }

    @Override
    public AbstractWorkSegmentationType resolveImplicitSegmentation(@NotNull ImplicitWorkSegmentationType segmentation) {
        if (auditReportSegmentation != null) {
            return auditReportSegmentation.resolveImplicitSegmentation(segmentation);
        } else {
            return super.resolveImplicitSegmentation(segmentation);
        }
    }

    @Override
    public void beforeBucketExecution(OperationResult result) {
        controller.beforeBucketExecution(bucket.getSequentialNumber(), result);
    }

    @Override
    public void afterBucketExecution(OperationResult result) throws CommonException {
        controller.afterBucketExecution(bucket.getSequentialNumber(), result);
    }

    private static class SearchSpecificationHolder implements ReportDataSource<Containerable> {

        private SearchSpecification<Containerable> searchSpecification;

        @Override
        public void initialize(Class<Containerable> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
            searchSpecification = new SearchSpecification<>(type, query, options, false);
        }

        @Override
        public void run(Handler<Containerable> handler, OperationResult result) {
            // no-op
        }
    }
}
