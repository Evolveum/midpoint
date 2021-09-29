/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;

import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.*;
import com.evolveum.midpoint.task.api.RunningTask;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.Handler;
import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.Nullable;

/**
 * Activity execution specifics for classical (i.e. not distributed) collection report export.
 */
public class ClassicCollectionReportExportActivityExecution
        extends PlainIterativeActivityExecution
        <Containerable,
                ClassicReportExportWorkDefinition,
                ClassicReportExportActivityHandler,
                ReportExportWorkStateType> {

    @NotNull private final ExportCollectionActivitySupport support;

    /** The report service Spring bean. */
    @NotNull private final ReportServiceImpl reportService;

    /**
     * Data writer which completes the content of the report.
     */
    private ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter;

    /**
     * Execution object (~ controller) that is used to transfer objects found into report data.
     * Initialized on the activity execution start.
     */
    private CollectionExportController<Containerable> controller;

    /**
     * This is "master" search specification, derived from the report.
     */
    private ContainerableReportDataSource searchSpecificationHolder;

    ClassicCollectionReportExportActivityExecution(
            ExecutionInstantiationContext<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> context) {
        super(context, "Collection report export");
        reportService = getActivityHandler().reportService;
        support = new ExportCollectionActivitySupport(this, reportService,
                getActivityHandler().objectResolver, getWorkDefinition());
    }

    @Override
    public ActivityReportingOptions getDefaultReportingOptions() {
        return super.getDefaultReportingOptions()
                .defaultDetermineOverallSize(ActivityOverallItemCountingOptionType.ALWAYS)
                .defaultDetermineBucketSize(ActivityItemCountingOptionType.NEVER);
    }

    @Override
    public void beforeExecution(OperationResult result) throws ActivityExecutionException, CommonException {
        RunningTask task = getRunningTask();
        support.beforeExecution(result);
        @NotNull ReportType report = support.getReport();

        support.stateCheck(result);

        searchSpecificationHolder = new ContainerableReportDataSource(support);
        dataWriter = ReportUtils.createDataWriter(
                report, FileFormatTypeType.CSV, getActivityHandler().reportService, support.getCompiledCollectionView(result));
        controller = new CollectionExportController<>(
                searchSpecificationHolder,
                dataWriter,
                report,
                reportService,
                support.getCompiledCollectionView(result),
                support.getReportParameters());

        controller.initialize(task, result);
        controller.beforeBucketExecution(1, result);
    }

    @Override
    public @Nullable Integer determineOverallSize(OperationResult result) throws CommonException {
        return support.countRecords(
                searchSpecificationHolder.getType(),
                searchSpecificationHolder.getQuery(),
                searchSpecificationHolder.getOptions(),
                result);
    }

    @Override
    public void iterateOverItemsInBucket(OperationResult result) throws CommonException {
        // Issue the search to audit or model/repository
        // And use the following handler to handle the results

        AtomicInteger sequence = new AtomicInteger(0);

        Handler<Containerable> handler = record -> {
            ItemProcessingRequest<Containerable> request =
                    ContainerableProcessingRequest.create(sequence.getAndIncrement(), record, this);
            coordinator.submit(request, result);
            return true;
        };
        searchSpecificationHolder.run(handler, result);
    }

    @Override
    public boolean processItem(@NotNull ItemProcessingRequest<Containerable> request, @NotNull RunningTask workerTask,
            OperationResult result)
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
}
