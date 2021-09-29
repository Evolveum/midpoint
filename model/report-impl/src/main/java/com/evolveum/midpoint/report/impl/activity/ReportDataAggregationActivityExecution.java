/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.List;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.*;
import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.ExportedReportDataRow;
import com.evolveum.midpoint.report.impl.controller.ExportedReportHeaderRow;
import com.evolveum.midpoint.report.impl.controller.ReportDataWriter;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

class ReportDataAggregationActivityExecution
        extends SearchBasedActivityExecution
        <ReportDataType,
                DistributedReportExportWorkDefinition,
                DistributedReportExportActivityHandler,
                ReportExportWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ReportDataAggregationActivityExecution.class);

    /** Helper functionality. */
    @NotNull private final DistributedReportExportActivitySupport support;

    /**
     * Data from all the partial reports.
     *
     * TODO eliminate gathering in memory: write to a file immediately after getting the data.
     */
    private final StringBuilder aggregatedData = new StringBuilder();

    /**
     * Data writer which completes the content of the report.
     */
    private ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter;

    ReportDataAggregationActivityExecution(
            @NotNull ExecutionInstantiationContext<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> context) {
        super(context, "Report data aggregation");
        support = new DistributedReportExportActivitySupport(this, getActivity());
    }

    @Override
    public void beforeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.beforeExecution(result);

        dataWriter = ReportUtils.createDataWriter(
                support.getReport(), FileFormatTypeType.CSV, getActivityHandler().reportService, support.getCompiledCollectionView(result));
    }

    @Override
    @NotNull
    public SearchSpecification<ReportDataType> createCustomSearchSpecification(OperationResult result) {
        // FIXME When parent OID is indexed, the query can be improved
        // FIXME Also when sequenceNumber is indexed, we'll sort on it
        String prefix = String.format("Partial report data for [%s]", support.getGlobalReportDataRef().getOid());
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
            throws CommonException, ActivityExecutionException {
        LOGGER.info("Appending data from {} (and deleting the object)", reportData);
        aggregatedData.append(reportData.getData());
        getActivityHandler().commonTaskBeans.repositoryService.deleteObject(ReportDataType.class, reportData.getOid(), result);
        return true;
    }

    @Override
    public void afterExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.saveReportFile(aggregatedData.toString(), dataWriter, result);
    }
}
