/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.util.List;

import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.fileformat.ReportDataWriter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeActivityExecution;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.repo.common.task.SearchSpecification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportExportWorkStateType;

class ReportDataAggregationActivityExecution
        extends AbstractSearchIterativeActivityExecution
        <ReportDataType,
                DistributedReportExportWorkDefinition,
                DistributedReportExportActivityHandler,
                ReportDataAggregationActivityExecution,
                ReportExportWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(ReportDataAggregationActivityExecution.class);

    /** Helper functionality. */
    @NotNull private final ActivityDistributedExportSupport support;

    /**
     * Data from all the partial reports.
     *
     * TODO eliminate gathering in memory: write to a file immediately after getting the data.
     */
    private final StringBuilder aggregatedData = new StringBuilder();

    /**
     * Data writer which completize context of report.
     */
    private ReportDataWriter dataWriter;

    ReportDataAggregationActivityExecution(
            @NotNull ExecutionInstantiationContext<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> context) {
        super(context, "Report data aggregation");
        support = new ActivityDistributedExportSupport(context);
    }

    @Override
    protected void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.initializeExecution(result);

        dataWriter = ReportUtils.createDataWriter(
                support.getReport(), FileFormatTypeType.CSV, getActivityHandler().reportService, support.getCompiledCollectionView(result));
    }

    @Override
    protected @NotNull SearchSpecification<ReportDataType> createSearchSpecification(OperationResult opResult) {
        // FIXME When parent OID is indexed, the query can be improved
        // FIXME Also when sequenceNumber is indexed, we'll sort on it
        String prefix = String.format("Partial report data for [%s]", support.getGlobalReportDataRef().getOid());
        return new SearchSpecification<>(
                ReportDataType.class,
                getPrismContext().queryFor(ReportDataType.class)
                        .item(ReportDataType.F_NAME).startsWithPoly(prefix)
                        .asc(ReportDataType.F_NAME)
                        .build(),
                List.of(),
                true);
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<ReportDataType>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(
                (reportData, request, workerTask, result) -> {
                    LOGGER.info("Appending data from {} (and deleting the object)", reportData);
                    aggregatedData.append(reportData.asObjectable().getData());
                    beans.repositoryService.deleteObject(ReportDataType.class, reportData.getOid(), result);
                    return true;
                }
        );
    }

    @Override
    protected void finishExecution(OperationResult result) throws CommonException, ActivityExecutionException {
       support.saveReportFile(aggregatedData.toString(), dataWriter, result);
    }

    @Override
    public boolean supportsStatistics() {
        return true;
    }
}
