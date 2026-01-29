/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.CSVDataExporter;
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
 * CSV data exporter that uses streaming (iterative) export to avoid loading all data into memory.
 * This extends Wicket's CSVDataExporter and overrides exportData to use IterativeExportSupport
 * when available.
 */
public class StreamingCsvDataExporter extends CSVDataExporter {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(StreamingCsvDataExporter.class);
    private static final String DOT_CLASS = StreamingCsvDataExporter.class.getName() + ".";
    private static final String OPERATION_EXPORT_DATA = DOT_CLASS + "exportData";

    private final PageBase pageBase;

    public StreamingCsvDataExporter(PageBase pageBase) {
        this.pageBase = pageBase;
    }

    @Override
    public <T> void exportData(IDataProvider<T> dataProvider,
            List<IExportableColumn<T, ?>> columns, OutputStream outputStream)
            throws IOException {

        if (!(dataProvider instanceof IterativeExportSupport)
                || !((IterativeExportSupport<?>) dataProvider).supportsIterativeExport()) {
            // Fall back to standard export if provider doesn't support iterative export
            LOGGER.info("DataProvider {} does not support iterative export, falling back to standard export",
                    dataProvider.getClass().getName());
            super.exportData(dataProvider, columns, outputStream);
            return;
        }

        @SuppressWarnings("unchecked")
        IterativeExportSupport<T> iterativeProvider = (IterativeExportSupport<T>) dataProvider;

        Task task = pageBase.createSimpleTask(OPERATION_EXPORT_DATA);
        OperationResult result = task.getResult();

        try (CsvGrid grid = new CsvGrid(new OutputStreamWriter(outputStream, Charset.forName(getCharacterSet())))) {
            writeHeaders(columns, grid);
            writeDataIterative(dataProvider, iterativeProvider, columns, grid, task, result);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Error during iterative CSV export", e);
            throw new IOException("Error during iterative CSV export: " + e.getMessage(), e);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private <T> void writeHeaders(List<IExportableColumn<T, ?>> columns, CsvGrid grid)
            throws IOException {
        if (isExportHeadersEnabled()) {
            for (IExportableColumn<T, ?> col : columns) {
                IModel<String> displayModel = col.getDisplayModel();
                String display = wrapModel(displayModel).getObject();
                grid.cell(quoteValue(display));
            }
            grid.row();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> void writeDataIterative(
            IDataProvider<T> dataProvider,
            IterativeExportSupport<T> iterativeProvider,
            List<IExportableColumn<T, ?>> columns,
            CsvGrid grid,
            Task task,
            OperationResult result) throws CommonException {

        iterativeProvider.exportIterative(
                (item, opResult) -> {
                    try {
                        writeRow(dataProvider, columns, item, grid);
                        return true;
                    } catch (IOException e) {
                        LOGGER.error("Error writing CSV row", e);
                        return false;
                    }
                },
                task,
                result
        );
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private <T> void writeRow(IDataProvider<T> dataProvider,
            List<IExportableColumn<T, ?>> columns, T row, CsvGrid grid) throws IOException {
        for (IExportableColumn<T, ?> col : columns) {
            IModel<?> dataModel = col.getDataModel(dataProvider.model(row));
            Object value = wrapModel(dataModel).getObject();
            if (value != null) {
                Class<?> c = value.getClass();
                String s;
                IConverter converter = getConverterLocator().getConverter(c);
                if (converter == null) {
                    s = value.toString();
                } else {
                    s = converter.convertToString(value, Session.get().getLocale());
                }
                grid.cell(quoteValue(s));
            } else {
                grid.cell("");
            }
        }
        grid.row();
    }

    /**
     * Simple CSV grid writer that handles cells and rows.
     */
    private class CsvGrid implements AutoCloseable {
        private final Writer writer;
        private boolean first = true;

        public CsvGrid(Writer writer) {
            this.writer = writer;
        }

        public void cell(String value) throws IOException {
            if (first) {
                first = false;
            } else {
                writer.write(getDelimiter());
            }
            writer.write(value);
        }

        public void row() throws IOException {
            writer.write("\r\n");
            writer.flush();  // Flush after each row for streaming
            first = true;
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }
}
