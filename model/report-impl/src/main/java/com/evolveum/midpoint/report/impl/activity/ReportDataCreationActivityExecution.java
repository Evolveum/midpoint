/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.EXPORT;

import java.util.Collection;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeActivityExecution;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.repo.common.task.SearchSpecification;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.fileformat.CollectionBasedExportController;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataSource;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataWriter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

/**
 * Executes the (potentially distributed) report data creation activity:
 *
 * 1. issues repo search based on data from export controller,
 * 2. processes objects found by feeding them into the export controller,
 * 3. finally, instructs the controller to write the (potentially partial) report.
 */
public class ReportDataCreationActivityExecution
        extends AbstractSearchIterativeActivityExecution
        <ObjectType,
                DistributedReportExportWorkDefinition,
                DistributedReportExportActivityHandler,
                ReportDataCreationActivityExecution,
                ReportExportWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ReportDataCreationActivityExecution.class);

    /**
     * Execution object (~ controller) that is used to transfer objects found into report data.
     * Initialized on the activity execution start.
     *
     * TODO decide on the correct name for this class and its instances
     */
    private CollectionBasedExportController<ObjectType> controller;

    /**
     * This is "master" search specification, derived from the report.
     * It is then narrowed down using buckets by the activity framework.
     */
    private SearchSpecification<ObjectType> masterSearchSpecification;

    @NotNull private final ActivityExecutionSupport support;

    /** The report service Spring bean. */
    @NotNull private final ReportServiceImpl reportService;

    ReportDataCreationActivityExecution(
            @NotNull ExecutionInstantiationContext<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> context) {
        super(context, "Data creation");
        reportService = context.getActivity().getHandler().reportService;
        support = new ActivityExecutionSupport(context);
    }

    /**
     * Called at the beginning of execution of this activity (potentially in a worker task).
     * Here is the place to pre-process the report definition.
     */
    @Override
    protected void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.initializeExecution(result);
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
        ReportDataWriter dataWriter = ReportUtils.createDataWriter(
                report, FileFormatTypeType.CSV, getActivityHandler().reportService, support.getCompiledCollectionView(result));
        controller = new CollectionBasedExportController<>(
                searchSpecificationHolder,
                dataWriter,
                report,
                support.getGlobalReportDataRef(),
                reportService,
                support.getCompiledCollectionView(result));

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
    protected @NotNull SearchSpecification<ObjectType> createSearchSpecification(OperationResult opResult) {
        return masterSearchSpecification.clone();
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ObjectType>> createItemProcessor(OperationResult opResult)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        return createDefaultItemProcessor(
                (object, request, workerTask, result) -> {
                    controller.handleDataRecord(request.getSequentialNumber(), object.asObjectable(), workerTask, result);
                    return true;
                }
        );
    }

    @Override
    protected void beforeBucketExecution(OperationResult opResult) {
        controller.beforeBucketExecution(bucket.getSequentialNumber(), opResult);
    }

    @Override
    protected void afterBucketExecution(OperationResult opResult) throws CommonException {
        controller.afterBucketExecution(bucket.getSequentialNumber(), opResult);
    }

    private static class SearchSpecificationHolder implements ReportDataSource<ObjectType> {

        private SearchSpecification<ObjectType> searchSpecification;

        @Override
        public void initialize(Class<ObjectType> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
            searchSpecification = new SearchSpecification<>(type, query, options, false);
        }

        @Override
        public void run(Handler<ObjectType> handler, OperationResult result) {
            // no-op
        }
    }
}
