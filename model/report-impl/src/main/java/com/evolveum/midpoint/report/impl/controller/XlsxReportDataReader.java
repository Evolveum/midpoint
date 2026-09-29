/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.UnsupportedFileFormatException;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.ooxml.POIXMLTypeLoader;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.xmlbeans.XmlException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbookPr;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.WorkbookDocument;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FileFormatConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportDataType;

/**
 * Reads import data from the first sheet of an XLSX workbook.
 *
 * The sheet is read by POI's streaming (event) API row by row, so the workbook is not loaded into memory as a whole
 * (MID-11009). Cells are read as displayed text (numbers and dates formatted by their cell format), so a file created
 * by a spreadsheet application and a file created by {@link XlsxReportDataWriter} are handled the same way.
 * For formula cells, the value cached in the file is used.
 * Multiple values in one cell are separated by the configured multivalue delimiter (line break by default),
 * matching the XLSX export.
 * Rows without any value are skipped.
 */
public class XlsxReportDataReader implements ReportDataReader {

    private static final int LAST_COLUMN_INDEX = SpreadsheetVersion.EXCEL2007.getLastColumnIndex();

    @Nullable private final FileFormatConfigurationType configuration;

    public XlsxReportDataReader(@Nullable FileFormatConfigurationType configuration) {
        this.configuration = configuration;
    }

    @Override
    public void read(@NotNull ReportDataType reportData, @NotNull List<String> viewHeaders,
            @NotNull Predicate<VariablesMap> rowHandler) throws IOException {
        boolean fileHasHeader = CommonXlsxSupport.isHeader(configuration);
        if (viewHeaders.isEmpty() && !fileHasHeader) {
            throw new IllegalArgumentException("Couldn't find headers please "
                    + "define them via view element or write them to the first row of the sheet and set "
                    + "header element in file format configuration to true.");
        }
        readSheetRows(new File(reportData.getFilePath()), new RowConverter(viewHeaders, fileHasHeader, rowHandler));
    }

    /**
     * Converts the cell values of rows to named values: skips empty rows and the header row (taking column names
     * from it if there are no view headers), and splits multiple values in a cell.
     */
    private class RowConverter implements Predicate<List<String>> {

        @NotNull private final List<String> headers;
        private boolean headerPending;
        @NotNull private final Predicate<VariablesMap> rowHandler;

        @NotNull private final String multivalueDelimiter = CommonXlsxSupport.getMultivalueDelimiter(configuration);
        // a line break in a cell may be CRLF, depending on the application that wrote the file
        @NotNull private final String splitRegex = CommonXlsxSupport.LINE_BREAK.equals(multivalueDelimiter) ?
                "\r?\n" : Pattern.quote(multivalueDelimiter);

        RowConverter(@NotNull List<String> viewHeaders, boolean fileHasHeader, @NotNull Predicate<VariablesMap> rowHandler) {
            this.headers = new ArrayList<>(viewHeaders);
            this.headerPending = fileHasHeader;
            this.rowHandler = rowHandler;
        }

        @Override
        public boolean test(List<String> cells) {
            if (cells.stream().allMatch(StringUtils::isEmpty)) {
                return true;
            }
            if (headerPending) {
                headerPending = false;
                if (headers.isEmpty()) {
                    headers.addAll(cells);
                }
                return true;
            }
            VariablesMap variables = new VariablesMap();
            for (int i = 0; i < headers.size(); i++) {
                String name = headers.get(i);
                if (StringUtils.isEmpty(name)) {
                    continue;
                }
                String value = i < cells.size() ? StringUtils.defaultIfEmpty(cells.get(i), null) : null;
                if (value != null && value.contains(multivalueDelimiter)) {
                    variables.put(name, Arrays.asList(value.split(splitRegex)), String.class);
                } else {
                    variables.put(name, value, String.class);
                }
            }
            // passes the row to the caller (e.g. for import); false means the caller wants no more rows
            return rowHandler.test(variables);
        }
    }

    /**
     * Reads the rows of the first sheet one by one, passing the values of their cells to the handler,
     * until the handler returns false.
     *
     * The parsing is done by POI's {@link XSSFSheetXMLHandler}; we only collect the cells of each row.
     */
    static void readSheetRows(@NotNull File file, @NotNull Predicate<List<String>> rowHandler) throws IOException {
        try (OPCPackage pkg = OPCPackage.open(file, PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            Iterator<InputStream> sheets = reader.getSheetsData();
            if (!sheets.hasNext()) {
                return;
            }
            try (InputStream sheet = sheets.next()) {
                XMLReader parser = XMLHelper.newXMLReader();
                parser.setContentHandler(new XSSFSheetXMLHandler(
                        reader.getStylesTable(),
                        new ReadOnlySharedStringsTable(pkg, false),
                        new RowCollector(rowHandler),
                        createFormatter(reader),
                        false));
                parser.parse(new InputSource(sheet));
            }
        } catch (StopReadingException e) {
            // the handler does not want any more rows
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } catch (UnsupportedFileFormatException e) {
            throw new IOException("File " + file + " is not an XLSX file: " + e.getMessage(), e);
        } catch (OpenXML4JException | SAXException | ParserConfigurationException | XmlException | POIXMLException e) {
            throw new IOException("Couldn't read XLSX file " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Formats cell values like POI does for the in-memory workbook, including dates in workbooks
     * using the 1904 date system (which the event API does not take into account by itself).
     */
    private static DataFormatter createFormatter(XSSFReader reader) throws IOException, OpenXML4JException, XmlException {
        boolean date1904;
        try (InputStream workbookData = reader.getWorkbookData()) {
            CTWorkbookPr workbookProperties = WorkbookDocument.Factory
                    .parse(workbookData, POIXMLTypeLoader.DEFAULT_XML_OPTIONS)
                    .getWorkbook()
                    .getWorkbookPr();
            date1904 = workbookProperties != null && workbookProperties.getDate1904();
        }
        return new DataFormatter() {
            @Override
            public String formatRawCellContents(double value, int formatIndex, String formatString) {
                return formatRawCellContents(value, formatIndex, formatString, date1904);
            }
        };
    }

    /** Collects the cells reported by POI for each row, and passes the whole row to the handler. */
    private static class RowCollector implements XSSFSheetXMLHandler.SheetContentsHandler {

        @NotNull private final Predicate<List<String>> rowHandler;
        private List<String> cells;

        RowCollector(@NotNull Predicate<List<String>> rowHandler) {
            this.rowHandler = rowHandler;
        }

        @Override
        public void startRow(int rowNum) {
            cells = new ArrayList<>();
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int column = cellReference != null ? getColumn(cellReference) : cells.size();
            while (cells.size() <= column) {
                cells.add("");
            }
            cells.set(column, StringUtils.defaultString(formattedValue));
        }

        @Override
        public void endRow(int rowNum) {
            // passes the cells of the row on for conversion (see RowConverter); false means no more rows are wanted
            if (!rowHandler.test(cells)) {
                throw new StopReadingException();
            }
        }

        /** Columns beyond the XLSX limit are refused, as they would make us allocate a huge row. */
        private static int getColumn(String cellReference) {
            int column;
            try {
                column = new CellAddress(cellReference).getColumn();
            } catch (IllegalArgumentException e) {
                column = -1;
            }
            if (column < 0 || column > LAST_COLUMN_INDEX) {
                throw new UncheckedIOException(new IOException("Invalid cell reference in XLSX sheet: " + cellReference));
            }
            return column;
        }
    }

    /** Thrown to stop parsing when the row handler does not want more rows. */
    private static class StopReadingException extends RuntimeException {
    }

    /** Values of all cells up to the last defined one; missing cells yield empty strings. */
    static List<String> readCells(Row row, DataFormatter formatter) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.add(cell != null ? formatter.formatCellValue(cell) : "");
        }
        return values;
    }
}
