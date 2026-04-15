/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.io.*;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.button.XlsxDataExporter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * XLSX data exporter that uses streaming (iterative) export to avoid loading all data into memory.
 */
public class StreamingXlsxDataExporter extends XlsxDataExporter {

    private static final Trace LOGGER = TraceManager.getTrace(StreamingXlsxDataExporter.class);
    private static final String DOT_CLASS = StreamingXlsxDataExporter.class.getName() + ".";
    private static final String OPERATION_EXPORT_DATA = DOT_CLASS + "exportXlsxData";

    private final PageBase pageBase;

    public StreamingXlsxDataExporter(PageBase pageBase) {
        super();
        this.pageBase = pageBase;
    }

    @Override
    public <T> void exportData(
            IDataProvider<T> dataProvider,
            List<IExportableColumn<T, ?>> columns,
            OutputStream outputStream) throws IOException {

        if (!(dataProvider instanceof IterativeExportSupport)
                || !((IterativeExportSupport<?>) dataProvider).supportsIterativeExport()) {

            LOGGER.info("DataProvider {} does not support iterative export, falling back to standard export",
                    dataProvider.getClass().getName());

            super.exportData(dataProvider, columns, outputStream);
            return;
        }

        @SuppressWarnings("unchecked")
        IterativeExportSupport<T> iterativeProvider = (IterativeExportSupport<T>) dataProvider;

        Task task = pageBase.createSimpleTask(OPERATION_EXPORT_DATA);
        OperationResult result = task.getResult();

        SXSSFWorkbook workbook = new SXSSFWorkbook();
        Sheet sheet = workbook.createSheet();

        try {
            writeHeaders(columns, sheet);
            writeDataIterative(dataProvider, iterativeProvider, columns, sheet, task, result);

            workbook.write(outputStream);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error during iterative XLSX export", e);
            throw new IOException("Error during iterative XLSX export: " + e.getMessage(), e);
        } finally {
            workbook.dispose(); // VERY IMPORTANT (cleans temp files)
            workbook.close();
            result.computeStatusIfUnknown();
        }
    }

    private <T> void writeDataIterative(
            IDataProvider<T> dataProvider,
            IterativeExportSupport<T> iterativeProvider,
            List<IExportableColumn<T, ?>> columns,
            Sheet sheet,
            Task task,
            OperationResult result) throws CommonException {

        final int[] rowIndex = {1}; // mutable counter

        iterativeProvider.exportIterative(
                (item, opResult) -> {
                    try {
                        writeRow(dataProvider, columns, item, sheet, rowIndex[0]++);
                        return true;
                    } catch (Exception e) {
                        LOGGER.error("Error writing XLSX row", e);
                        return false;
                    }
                },
                task,
                result
        );
    }

    private <T> void writeRow(
            IDataProvider<T> dataProvider,
            List<IExportableColumn<T, ?>> columns,
            T row,
            Sheet sheet,
            int rowIndex) {

        Row sheetRow = sheet.createRow(rowIndex);

        int indexColumn = 0;
        for (IExportableColumn<T, ?> col : columns) {
            IModel<?> dataModel = col.getDataModel(dataProvider.model(row));
            Object value = wrapModel(dataModel).getObject();

            if (value != null) {
                Class<?> c = value.getClass();
                IConverter converter = Application.get().getConverterLocator().getConverter(c);

                String cellValue = (converter == null)
                        ? value.toString()
                        : converter.convertToString(value, Session.get().getLocale());

                sheetRow.createCell(indexColumn++).setCellValue(cellValue);
            } else {
                sheetRow.createCell(indexColumn++);
            }
        }
    }
}
