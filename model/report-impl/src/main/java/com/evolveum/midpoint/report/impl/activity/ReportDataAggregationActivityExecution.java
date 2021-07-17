/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.activity;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.evolveum.midpoint.report.impl.ReportUtils;
import com.evolveum.midpoint.report.impl.controller.engine.CollectionEngineController;
import com.evolveum.midpoint.report.impl.controller.fileformat.FileFormatController;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.io.FileUtils;
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
    @NotNull private final ActivityExecutionSupport support;

    /**
     * Data from all the partial reports.
     *
     * TODO eliminate gathering in memory: write to a file immediately after getting the data.
     */
    private final StringBuilder aggregatedData = new StringBuilder();

    /**
     * File to which the aggregated data are stored.
     */
    private String aggregatedFilePath;

    /**
     * Used to derive the file path and to save report data.
     *
     * TODO remove dependency on this class
     */
    private FileFormatController fileFormatController;

    ReportDataAggregationActivityExecution(
            @NotNull ExecutionInstantiationContext<DistributedReportExportWorkDefinition, DistributedReportExportActivityHandler> context) {
        super(context, "Report data aggregation");
        support = new ActivityExecutionSupport(context);
    }

    @Override
    protected void initializeExecution(OperationResult result) throws CommonException, ActivityExecutionException {
        support.initializeExecution(result);

        CollectionEngineController engineController = new CollectionEngineController(getActivityHandler().reportService);

        fileFormatController = ReportUtils.createExportController(
                support.getReport(),
                engineController.getDefaultFileFormat(),
                getActivityHandler().reportService);

        aggregatedFilePath =
                replaceColons(
                        engineController.getDestinationFileName(support.getReport(), fileFormatController));
    }

    /**
     * Very strange: colons are no problem for Windows, but Apache file utils complain for them (when running on Windows).
     * So they will be replaced, at least temporarily.
     *
     * TODO research this
     */
    private String replaceColons(String path) {
        if (onWindows()) {
            return path.replaceAll(":", "_");
        } else {
            return path;
        }
    }

    private boolean onWindows() {
        return File.separatorChar == '\\';
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
        writeToReportFile();
        saveReportDataObject(result);
    }

    /**
     * TODO use correct charset
     */
    private void writeToReportFile() {
        try {
            FileUtils.writeByteArrayToFile(
                    new File(aggregatedFilePath),
                    aggregatedData.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SystemException("Couldn't write aggregated report to " + aggregatedFilePath, e);
        }
    }

    private void saveReportDataObject(OperationResult result) throws CommonException {
        getActivityHandler().reportTaskHandler.saveReportDataType(
                aggregatedFilePath,
                support.getReport(),
                fileFormatController,
                getRunningTask(),
                result);

        LOGGER.info("Aggregated report was saved - the file is {}", aggregatedFilePath);
    }

    @Override
    public boolean supportsStatistics() {
        return true;
    }
}
