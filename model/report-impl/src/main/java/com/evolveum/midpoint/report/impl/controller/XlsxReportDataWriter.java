/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Creates exported reports in XLSX format.
 *
 * For distributed export, a partial is a small workbook with the rows of one bucket, stored as bytes in
 * {@link ReportDataType#getData()}. The first bucket's workbook starts with the header row.
 * The aggregation copies the rows of all partials into one streaming workbook.
 */
public class XlsxReportDataWriter
        extends AbstractReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow>
        implements DistributableReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> {

    @Nullable private final FileFormatConfigurationType configuration;

    /** Used for the sheet name; may be null when the writer is created without a view. */
    @Nullable private final CompiledObjectCollectionView compiledView;

    /** The workbook being assembled from partial data; created on the first partial. */
    private CommonXlsxSupport aggregation;
    private Sheet aggregationSheet;
    private int aggregationNextRow;

    public XlsxReportDataWriter(
            @NotNull ReportServiceImpl reportService,
            @Nullable CompiledObjectCollectionView compiledView,
            @Nullable FileFormatConfigurationType configuration) {
        super(reportService);
        this.compiledView = compiledView;
        this.configuration = configuration;
    }

    @Override
    public void writeCompletedReport(@NotNull OutputStream outputStream) throws IOException {
        try (CommonXlsxSupport support = new CommonXlsxSupport(configuration)) {
            Sheet sheet = support.createSheet(getSheetName());
            int nextRow = writeBufferedRows(support, sheet);
            writeFooter(support, sheet, nextRow);
            support.write(outputStream);
        }
    }

    @Override
    public void storePartialData(@NotNull ReportDataType partialReportData) throws IOException {
        if (getHeaderRow() == null && getDataRows().isEmpty()) {
            return; // nothing to store; the aggregation skips partials without data
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (CommonXlsxSupport support = new CommonXlsxSupport(configuration)) {
            Sheet sheet = support.createSheet(null);
            writeBufferedRows(support, sheet);
            support.write(bytes);
        }
        partialReportData.setData(bytes.toByteArray());
    }

    @Override
    public void appendPartialData(@NotNull ReportDataType partialReportData) throws IOException {
        byte[] bytes = DistributableReportDataWriter.getBinaryData(partialReportData);
        if (bytes == null || bytes.length == 0) {
            return;
        }
        if (aggregation == null) {
            aggregation = new CommonXlsxSupport(configuration);
            aggregationSheet = aggregation.createSheet(getSheetName());
            aggregationNextRow = 0;
        }
        // The header (if any) is the first row of the first bucket; see CollectionExportController.beforeBucketExecution.
        boolean headerPending = shouldWriteHeader()
                && partialReportData.getSequentialNumber() != null && partialReportData.getSequentialNumber() == 1;
        DataFormatter formatter = new DataFormatter();
        try (Workbook partial = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (partial.getNumberOfSheets() == 0) {
                return;
            }
            for (Row row : partial.getSheetAt(0)) {
                List<String> cells = XlsxReportDataReader.readCells(row, formatter);
                if (headerPending) {
                    headerPending = false;
                    aggregation.writeHeader(aggregationSheet, aggregationNextRow++, cells);
                } else {
                    // cell texts are already joined by the multivalue delimiter, so each is a single value here
                    aggregation.writeDataRow(aggregationSheet, aggregationNextRow++, cells.stream().map(List::of).toList());
                }
            }
        }
    }

    @Override
    public void writeAggregatedReport(@NotNull OutputStream outputStream) throws IOException {
        if (aggregation == null) {
            aggregation = new CommonXlsxSupport(configuration);
            aggregationSheet = aggregation.createSheet(getSheetName());
            aggregationNextRow = 0;
        }
        try (CommonXlsxSupport support = aggregation) {
            writeFooter(support, aggregationSheet, aggregationNextRow);
            support.write(outputStream);
        } finally {
            aggregation = null;
            aggregationSheet = null;
        }
    }

    /** Writes header (if set) and buffered rows; returns the index of the next free row. */
    private int writeBufferedRows(CommonXlsxSupport support, Sheet sheet) {
        int rowIndex = 0;
        ExportedReportHeaderRow headerRow = getHeaderRow();
        if (headerRow != null) {
            support.writeHeader(sheet, rowIndex++, headerRow.getLabels());
        }
        for (ExportedReportDataRow dataRow : getDataRows()) {
            support.writeDataRow(sheet, rowIndex++, dataRow.getValues());
        }
        return rowIndex;
    }

    private void writeFooter(CommonXlsxSupport support, Sheet sheet, int rowIndex) {
        String subscriptionFooter = reportService.missingSubscriptionFooter();
        if (subscriptionFooter != null) {
            support.writeTextRow(sheet, rowIndex, subscriptionFooter);
        }
    }

    private @Nullable String getSheetName() {
        if (compiledView == null) {
            return null;
        }
        return new CommonHtmlSupport(reportService.getClock(), compiledView)
                .getTableName(reportService.getLocalizationService());
    }

    @Override
    public boolean shouldWriteHeader() {
        return CommonXlsxSupport.isHeader(configuration);
    }

    @Override
    public @NotNull FileFormatTypeType getFileFormatType() {
        return FileFormatTypeType.XLSX;
    }

    @Override
    public FileFormatConfigurationType getFileFormatConfiguration() {
        return configuration;
    }
}
