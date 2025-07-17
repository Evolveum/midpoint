/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.GenericProcessingRequest;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.*;
import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Activity execution specifics for classical (i.e. not distributed) collection report export.
 */
public final class ClassicCollectionReportExportActivityRun<T>
        extends PlainIterativeActivityRun
        <T,
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
     * Translates objects found into report data. Initialized on the activity run start.
     */
    private CollectionExportController<T> controller;

    /**
     * This is "master" search specification, derived from the report.
     */
    private PrismableReportDataSource<T> searchSpecificationHolder;

    ClassicCollectionReportExportActivityRun(
            ActivityRunInstantiationContext<ClassicReportExportWorkDefinition, ClassicReportExportActivityHandler> context) {
        super(context, "Collection report export");
        reportService = getActivityHandler().reportService;
        support = new ExportCollectionActivitySupport(this, reportService,
                getActivityHandler().objectResolver, getWorkDefinition());
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .skipWritingOperationExecutionRecords(true) // because of performance
                .determineOverallSizeDefault(ActivityOverallItemCountingOptionType.ALWAYS);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws ActivityRunException, CommonException {
        if (!super.beforeRun(result)) {
            return false;
        }

        RunningTask task = getRunningTask();
        support.beforeRun(result);
        @NotNull ReportType report = support.getReport();

        support.stateCheck(result);

        searchSpecificationHolder = new PrismableReportDataSource<>(support);
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
        return true;
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return support.getReportRef();
    }

    @Override
    public Integer determineOverallSize(OperationResult result) throws CommonException {
        return support.countRecords(
                searchSpecificationHolder.getType(),
                searchSpecificationHolder.getQuery(),
                searchSpecificationHolder.getOptions(),
                result);
    }

    @Override
    public void iterateOverItemsInBucket(OperationResult gResult) throws CommonException {
        // Issue the search to audit or model/repository
        // And use the following handler to handle the results

        AtomicInteger sequence = new AtomicInteger(0);

        ObjectHandler<T> handler = (record, lResult) -> {
            ItemProcessingRequest<T> request =
                    new GenericProcessingRequest<>(sequence.getAndIncrement(), record, this);
            return coordinator.submit(request, lResult);
        };
        searchSpecificationHolder.run(handler, gResult);
    }

    @Override
    public boolean processItem(
            @NotNull ItemProcessingRequest<T> request, @NotNull RunningTask workerTask, OperationResult result)
            throws ConfigurationException {
        T record = request.getItem();
        controller.handleDataRecord(request.getSequentialNumber(), record, workerTask, result);
        return true;
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException {
        support.saveSimpleReportData(dataWriter, result);
    }

    @Override
    public @NotNull ErrorHandlingStrategyExecutor.FollowUpAction getDefaultErrorAction() {
        return ErrorHandlingStrategyExecutor.FollowUpAction.CONTINUE;
    }
}
