/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.poi.ss.usermodel.Sheet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.report.impl.ReportServiceImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;

/** Creates exported reports in XLSX format. */
public class XlsxReportDataWriter
        extends AbstractReportDataWriter<ExportedReportDataRow, ExportedReportHeaderRow> {

    @Nullable private final FileFormatConfigurationType configuration;

    /** Used for the sheet name; may be null when the writer is created without a view. */
    @Nullable private final CompiledObjectCollectionView compiledView;

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
        try (CommonXlsxSupport support = new CommonXlsxSupport()) {
            Sheet sheet = support.createSheet(getSheetName());

            int rowIndex = 0;
            ExportedReportHeaderRow headerRow = getHeaderRow();
            if (headerRow != null) {
                support.writeHeader(sheet, rowIndex++, headerRow.getLabels());
            }
            for (ExportedReportDataRow dataRow : getDataRows()) {
                support.writeDataRow(sheet, rowIndex++, dataRow.getValues());
            }

            String subscriptionFooter = reportService.missingSubscriptionFooter();
            if (subscriptionFooter != null) {
                support.writeTextRow(sheet, rowIndex, subscriptionFooter);
            }

            support.write(outputStream);
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
        return true;
    }

    @Override
    public String getTypeSuffix() {
        return ".xlsx";
    }

    @Override
    public String getType() {
        return "XLSX";
    }

    @Override
    public FileFormatConfigurationType getFileFormatConfiguration() {
        return configuration;
    }
}
