/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;

import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.fileformat.CollectionBasedExportController;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataSource;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataWriter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.task.api.RunningTask;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Activity execution specifics for classical report export.
 *
 * TODO finish the implementation
 */
public class ClassicReportExportActivityExecutionSpecifics
        extends BasePlainIterativeExecutionSpecificsImpl
        <Containerable,
                ClassicReportExportWorkDefinition,
                ClassicReportExportActivityHandler> {

    @NotNull private final ExportActivitySupport support;

    /** The report service Spring bean. */
    @NotNull private final ReportServiceImpl reportService;

    private ReportType report;

    /**
     * Data writer which completize context of report.
     */
    private ReportDataWriter dataWriter;

    /**
     * Execution object (~ controller) that is used to transfer objects found into report data.
     * Initialized on the activity execution start.
     *
     * TODO decide on the correct name for this class and its instances
     */
    private CollectionBasedExportController<Containerable> controller;

    /**
     * This is "master" search specification, derived from the report.
     */
    private SearchSpecificationHolder searchSpecificationHolder;

    ClassicReportExportActivityExecutionSpecifics(
            @NotNull PlainIterativeActivityExecution<Containerable, ClassicReportExportWorkDefinition,
                    ClassicReportExportActivityHandler, ?> activityExecution) {
        super(activityExecution);
        reportService = activityExecution.getActivity().getHandler().reportService;
        support = new ExportActivitySupport(activityExecution, activityExecution.getActivityHandler().reportService,
                activityExecution.getActivityHandler().objectResolver, activityExecution.getActivity().getWorkDefinition());
    }

    @Override
    public void beforeExecution(OperationResult result) throws ActivityExecutionException, CommonException {
        RunningTask task = getRunningTask();
        support.beforeExecution(result);
        report = support.getReport();

        support.stateCheck(result);

        searchSpecificationHolder = new SearchSpecificationHolder();
        dataWriter = ReportUtils.createDataWriter(
                report, FileFormatTypeType.CSV, getActivityHandler().reportService, support.getCompiledCollectionView(result));
        controller = new CollectionBasedExportController<>(
                searchSpecificationHolder,
                dataWriter,
                report,
                reportService,
                support.getCompiledCollectionView(result));

        controller.initialize(task, result);
        controller.beforeBucketExecution(1, result);
    }

    @Override
    public void iterateOverItems(OperationResult result) throws CommonException {
        // Issue the search to audit or model/repository
        // And use the following handler to handle the results

        AtomicInteger sequence = new AtomicInteger(0);

        Handler<Containerable> handler = record -> {
            ItemProcessingRequest<Containerable> request = new ContainerableProcessingRequest<>(
                    sequence.getAndIncrement(), record, activityExecution);
            getProcessingCoordinator().submit(request, result);
            return true;
        };

        List<? extends Containerable> objects = support.searchRecords(
                searchSpecificationHolder.type,
                searchSpecificationHolder.query,
                searchSpecificationHolder.options,
                result);
        objects.forEach(object -> handler.handle(object));
    }

    @Override
    public boolean processItem(ItemProcessingRequest<Containerable> request, RunningTask workerTask, OperationResult result)
            throws CommonException, ActivityExecutionException {
        Containerable record = request.getItem();
        controller.handleDataRecord(request.getSequentialNumber(), record, workerTask, result);
        return true;
    }

    @Override
    public void afterExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.saveReportFile(dataWriter, result);
    }

    @Override
    public @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }

    private static class SearchSpecificationHolder implements ReportDataSource<Containerable> {

        private Class<Containerable> type;
        private ObjectQuery query;
        Collection<SelectorOptions<GetOperationOptions>> options;

        @Override
        public void initialize(Class<Containerable> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options) {
            this.type = type;
            this.query = query;
            this.options = options;
        }

        @Override
        public void run(Handler<Containerable> handler, OperationResult result) {
            // no-op
        }
    }
}
