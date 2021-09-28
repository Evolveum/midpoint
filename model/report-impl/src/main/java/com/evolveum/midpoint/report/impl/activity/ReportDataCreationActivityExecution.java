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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.report.impl.controller.*;

import com.evolveum.midpoint.repo.common.task.*;

import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

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

    /**
     * This method converts implicit segmentation (containing typically the number of buckets, maybe with the discriminator)
     * into full segmentation specification (discriminator, segmentation type, start/end value, and so on).
     *
     * Currently, only the number of buckets is supported in the implicit segmentation. Discriminator and matching rule
     * are not allowed here.
     *
     * We return ExplicitWorkSegmentationType, because dateTime-based buckets are not available now.
     * The disadvantage is that the list of buckets is re-created at each bucket acquisition, so if it's
     * large, the performance will suffer.
     */
    @Override
    public AbstractWorkSegmentationType resolveImplicitSegmentation(@NotNull ImplicitWorkSegmentationType segmentation) {
        stateCheck(getItemType().equals(AuditEventRecordType.class),
                "Implicit segmentation is supported only for audit reports, not for %s", getItemType());
        argCheck(segmentation.getDiscriminator() == null, "Discriminator specification is not supported");
        argCheck(segmentation.getMatchingRule() == null, "Matching rule specification is not supported");
        argCheck(segmentation.getNumberOfBuckets() != null, "Number of buckets must be specified");

        // TODO these should be determined from the report parameters or collection specification or from other place
        //  They MUST be the same on each invocation!
        XMLGregorianCalendar reportFrom = XmlTypeConverter.createXMLGregorianCalendar("2020-01-01T00:00:00.000+02:00");
        XMLGregorianCalendar reportTo = XmlTypeConverter.createXMLGregorianCalendar("2022-01-01T00:00:00.000+02:00");

        long reportFromMillis = XmlTypeConverter.toMillis(reportFrom);
        long reportToMillis = XmlTypeConverter.toMillis(reportTo);
        long step = (reportToMillis - reportFromMillis) / segmentation.getNumberOfBuckets();

        LOGGER.trace("Creating segmentation: from = {}, to = {}, step = {}",
                reportFrom, reportTo, step);

        ExplicitWorkSegmentationType explicitSegmentation = new ExplicitWorkSegmentationType(PrismContext.get());
        for (long bucketFromMillis = reportFromMillis; bucketFromMillis < reportToMillis; bucketFromMillis += step) {
            explicitSegmentation.getContent().add(
                    createBucketContent(bucketFromMillis, step, reportToMillis));
        }
        return explicitSegmentation;
    }

    /**
     * Creates a single bucket, given the start of the bucket (`bucketFromMillis`), step (`step`), and
     * the global end (`reportToMillis`).
     *
     * Note that start of the interval is inclusive, whereas the end is exclusive.
     */
    private AbstractWorkBucketContentType createBucketContent(long bucketFromMillis, long step, long reportToMillis) {
        XMLGregorianCalendar bucketFrom = XmlTypeConverter.createXMLGregorianCalendar(bucketFromMillis);
        XMLGregorianCalendar bucketTo;
        if (bucketFromMillis + step < reportToMillis) {
            bucketTo = XmlTypeConverter.createXMLGregorianCalendar(bucketFromMillis + step);
        } else {
            bucketTo = null;
        }

        ObjectFilter filter;
        if (bucketTo != null) {
            filter = PrismContext.get().queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_TIMESTAMP).ge(bucketFrom) // inclusive
                    .and().item(AuditEventRecordType.F_TIMESTAMP).lt(bucketTo) // exclusive
                    .buildFilter();
        } else {
            filter = PrismContext.get().queryFor(AuditEventRecordType.class)
                    .item(AuditEventRecordType.F_TIMESTAMP).ge(bucketFrom)
                    .buildFilter();
        }
        SearchFilterType filterBean;
        try {
            filterBean = PrismContext.get().getQueryConverter().createSearchFilterType(filter);
        } catch (SchemaException e) {
            throw new SystemException("Unexpected schema exception while converting bucket filter: " + e.getMessage(), e);
        }

        return new FilterWorkBucketContentType()
                .filter(filterBean);
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
