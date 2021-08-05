/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;

import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.fileformat.CollectionBasedExportController;
import com.evolveum.midpoint.report.impl.controller.fileformat.ImportController;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataSource;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataWriter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

import static com.evolveum.midpoint.report.impl.ReportUtils.getDirection;
import static com.evolveum.midpoint.util.MiscUtil.stateCheck;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.EXPORT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.DirectionTypeType.IMPORT;

/**
 * Activity execution for classical report export.
 *
 * TODO finish the implementation
 */
public class ClassicReportExportActivityExecution
        extends AbstractIterativeActivityExecution
        <Containerable,
                ClassicReportExportWorkDefinition,
                ClassicReportExportActivityHandler,
                ReportExportWorkStateType> {

    @NotNull private final ActivityExportSupport support;

    /** The report service Spring bean. */
    @NotNull private final ReportServiceImpl reportService;

    @NotNull private ReportType report;

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

    ClassicReportExportActivityExecution(
            @NotNull ExecutionInstantiationContext<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> context) {
        super(context, "Report export");
        reportService = context.getActivity().getHandler().reportService;
        support = new ActivityExportSupport(context, context.getActivity().getHandler().reportService,
                context.getActivity().getHandler().objectResolver, context.getActivity().getWorkDefinition());
    }

    @Override
    protected void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        RunningTask task = getRunningTask();
        support.initializeExecution(result);
        report = support.getReport();

        support.stateCheck(result);

        ClassicReportExportActivityExecution.SearchSpecificationHolder searchSpecificationHolder = new ClassicReportExportActivityExecution.SearchSpecificationHolder();
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
    protected void processItems(OperationResult result) throws CommonException {
        // Issue the search to audit or model/repository
        // And use the following handler to handle the results

        AtomicInteger sequence = new AtomicInteger(0);

        Handler<Containerable> handler = record -> {
            ItemProcessingRequest<Containerable> request = new ContainerableProcessingRequest<>(
                    sequence.getAndIncrement(), record, ClassicReportExportActivityExecution.this);
            coordinator.submit(request, result);
            return true;
        };
    }

    @Override
    protected @NotNull ItemProcessor<Containerable> createItemProcessor(OperationResult opResult) {
        return (request, workerTask, parentResult) -> {
            Containerable record = request.getItem();

            // TODO process the record

            return true;
        };
    }

    @Override
    protected void finishExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        support.saveReportFile(dataWriter, opResult);
    }

    @Override
    public boolean providesTracingAndDynamicProfiling() {
        return false;
    }

    @Override
    protected @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
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
