/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.activity;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.DistributableReportDataWriter;
import com.evolveum.midpoint.report.impl.controller.ExportedReportDataRow;
import com.evolveum.midpoint.report.impl.controller.ExportedReportHeaderRow;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

final class ReportDataAggregationActivityRun
        extends SearchBasedActivityRun
        <ReportDataType,
                DistributedReportExportWorkDefinition,
                DistributedReportExportActivityHandler,
                ReportExportWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ReportDataAggregationActivityRun.class);

    /** Helper functionality for the "distributed report exports" activity. */
    @NotNull private final DistributedReportExportActivitySupport support;

    /** Data writer which merges the partial data and completes the content of the report (e.g. by providing HTML code at the end) */
    private DistributableReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> completingDataWriter;

    /** The number of bucket we expect (during collection of partial results). */
    private int expectedSequentialNumber = 1;

    ReportDataAggregationActivityRun(
            @NotNull ActivityRunInstantiationContext<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> context) {
        super(context, "Report data aggregation");
        support = new DistributedReportExportActivitySupport(this, getActivity());
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .skipWritingOperationExecutionRecords(true); // partial data objects are deleted anyway
    }

    @Override
    public boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        if (!super.beforeRun(result)) {
            return false;
        }
        ensureNoParallelism();

        support.beforeRun(result);

        completingDataWriter = ReportUtils.createDistributableDataWriter(
                support.getReport(),
                FileFormatTypeType.CSV, // default type
                getActivityHandler().reportService,
                support.getCompiledCollectionView(result));

        return true;
    }

    @Override
    @NotNull
    public SearchSpecification<ReportDataType> createCustomSearchSpecification(OperationResult result) {
        // FIXME When parent OID is indexed, the query can be improved
        // FIXME Also when sequenceNumber is indexed, we'll sort on it
        String prefix = String.format("Partial report data for %s", support.getGlobalReportDataRef().getOid());
        return new SearchSpecification<>(
                ReportDataType.class,
                PrismContext.get().queryFor(ReportDataType.class)
                        .item(ReportDataType.F_NAME).startsWithPoly(prefix)
                        .asc(ReportDataType.F_NAME)
                        .build(),
                List.of(),
                true);
    }

    @Override
    public boolean processItem(@NotNull ReportDataType reportData,
            @NotNull ItemProcessingRequest<ReportDataType> request, RunningTask workerTask, OperationResult result)
            throws CommonException {
        LOGGER.info("Appending data from {} (and deleting the object)", reportData);
        checkSequentialNumber(reportData); // TODO check also the total # of buckets (after we know it at the start!)
        try {
            completingDataWriter.appendPartialData(reportData);
        } catch (IOException e) {
            throw new SystemException("Couldn't process partial report data " + reportData, e);
        }
        getActivityHandler().commonTaskBeans.repositoryService.deleteObject(ReportDataType.class, reportData.getOid(), result);
        return true;
    }

    private void checkSequentialNumber(@NotNull ReportDataType reportData) {
        int sequentialNumber = Objects.requireNonNull(
                reportData.getSequentialNumber(),
                () -> "No sequential number in " + reportData);
        stateCheck(sequentialNumber == expectedSequentialNumber,
                "Expected sequential number %d but got %d", expectedSequentialNumber, sequentialNumber);
        expectedSequentialNumber++;
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException {
        support.saveAggregatedReportData(completingDataWriter, support.getGlobalReportDataRef(), result);
    }
}
