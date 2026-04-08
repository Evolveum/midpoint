/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

public class XlsxDataExporter extends AbstractDataExporter {

    private static final int SHEET_DEFAULT_COLUMN_WIDTH = 5000;

    public XlsxDataExporter() {
        super(Model.of("XLSX"), "text/xlsx", "xlsx");
    }

    @Override
    public <T> void exportData(
            IDataProvider<T> dataProvider,
            List<IExportableColumn<T, ?>> columns,
            OutputStream outputStream
    ) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        writeHeaders(columns, sheet);
        writeData(dataProvider, columns, sheet);
        workbook.write(outputStream);
        workbook.close();
    }

    private <T> void writeHeaders(List<IExportableColumn<T, ?>> columns, Sheet sheet) {
        Row header = sheet.createRow(0);
        int index = 0;
        for (IExportableColumn<T, ?> col : columns) {
            IModel<String> displayModel = col.getDisplayModel();
            String display = this.wrapModel(displayModel).getObject();
            sheet.setColumnWidth(index, SHEET_DEFAULT_COLUMN_WIDTH);
            header.createCell(index++).setCellValue(display);
        }
    }

    private <T> void writeData(IDataProvider<T> dataProvider, List<IExportableColumn<T, ?>> columns, Sheet sheet) {
        long numberOfRows = dataProvider.size();
        Iterator<? extends T> rowIterator = dataProvider.iterator(0L, numberOfRows);

        int indexRow = 1;
        while (rowIterator.hasNext()) {
            Row sheetRow = sheet.createRow(indexRow++);
            T row = (T) rowIterator.next();

            int indexColumn = 0;
            for (IExportableColumn<T, ?> col : columns) {
                IModel<?> dataModel = col.getDataModel(dataProvider.model(row));
                Object value = this.wrapModel(dataModel).getObject();
                if (value != null) {
                    Class<?> c = value.getClass();
                    IConverter converter = Application.get().getConverterLocator().getConverter(c);
                    String cellValue;
                    if (converter == null) {
                        cellValue = value.toString();
                    } else {
                        cellValue = converter.convertToString(value, Session.get().getLocale());
                    }

                    sheetRow.createCell(indexColumn++).setCellValue(cellValue);
                }
            }
        }
    }

    protected <T> IModel<T> wrapModel(IModel<T> model) {
        return model;
    }
}
