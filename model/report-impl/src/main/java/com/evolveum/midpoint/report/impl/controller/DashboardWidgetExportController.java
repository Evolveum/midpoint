/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl.controller;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.report.impl.activity.ClassicDashboardReportExportActivityExecutionSpecifics;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.report.impl.controller.CommonHtmlSupport.*;

/**
 * Controls the process of exporting collection-based reports.
 *
 * Currently the only use of this class is to be a "bridge" between the world of the activity framework
 * (represented mainly by {@link ClassicDashboardReportExportActivityExecutionSpecifics} class) and a set of cooperating
 * classes that implement the report export itself. However, in the future it may be used in other ways,
 * independently of the activity framework.
 *
 * The process is driven by the activity execution that calls the following methods of this class:
 *
 * 1. {@link #initialize()} that sets up the processes (in a particular worker task),
 * 2. {@link #beforeBucketExecution(int, OperationResult)} that starts processing of a given work bucket,
 * 3. {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} that processes given prism object,
 * to be aggregated.
 *
 * @param <C> Type of records to be processed.
 */
@Experimental
public class DashboardWidgetExportController<C extends Containerable> implements ExportController<C> {

    /**
     * Data writer for the report. Produces e.g. CSV or HTML data.
     */
    @NotNull protected final ReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> dataWriter;

    /** The report of which an export is being done. */
    @NotNull protected final ReportType report;

    /**
     * ExportedReportHeaderRow for the widget table.
     */
    protected ExportedDashboardReportHeaderRow headerRow;

    // Useful Spring beans
    protected final ReportServiceImpl reportService;
    protected final SchemaService schemaService;
    protected final RepositoryService repositoryService;

    public DashboardWidgetExportController(
            @NotNull ReportDataWriter<? extends ExportedReportDataRow, ? extends ExportedReportHeaderRow> dataWriter,
            @NotNull ReportType report,
            @NotNull ReportServiceImpl reportService) {

        this.dataWriter = (ReportDataWriter) dataWriter;
        this.report = report;
        this.reportService = reportService;
        this.schemaService = reportService.getSchemaService();
        this.repositoryService = reportService.getRepositoryService();
    }

    /**
     * Prepares the controller for accepting the source data:
     * initializes the data source, determines columns, etc.
     */
    public void initialize()
            throws CommonException {

        List<ExportedReportHeaderColumn> headerColumns = getHeadsOfWidget().stream()
                .map(label -> ExportedReportHeaderColumn.fromLabel(
                        GenericSupport.getMessage(reportService.getLocalizationService(), "Widget." + label)))
                .collect(Collectors.toList());

        headerRow = ExportedDashboardReportHeaderRow.fromColumns(headerColumns, null);
    }

    /**
     * Called before bucket of data is executed, i.e. before data start flowing to
     * {@link #handleDataRecord(int, Containerable, RunningTask, OperationResult)} method.
     *
     * We have to prepare for collecting the data.
     */
    public void beforeBucketExecution(int sequentialNumber, @SuppressWarnings("unused") OperationResult result) {
        if (sequentialNumber == 1 && dataWriter.shouldWriteHeader()) {
            setHeaderRow();
        }
    }

    private void setHeaderRow() {
        dataWriter.setHeaderRow(headerRow);
    }

    protected static Set<String> getHeadsOfWidget() {
        return HEADS_OF_WIDGET;
    }

    /**
     * BEWARE: Can be called from multiple threads at once.
     * The resulting rows should be sorted on sequentialNumber.
     */
    public void handleDataRecord(int sequentialNumber, C record, RunningTask workerTask, OperationResult result) throws CommonException {

        ColumnDataConverter<C> columnDataConverter =
                new ColumnDataConverter<>(record, report, reportService, workerTask, result);

        ExportedDashboardReportDataRow dataRow = new ExportedDashboardReportDataRow(sequentialNumber);

        for (String label : getHeadsOfWidget()) {
            dataRow.addColumn(
                    columnDataConverter.convertWidgetColumn(label, dataWriter.getFunctionForWidgetStatus()));
        }

        dataWriter.appendDataRow(dataRow);
    }
}
