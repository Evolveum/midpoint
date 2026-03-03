/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.wicket.Application;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.ExportToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.resource.IResourceStream;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AbstractAjaxDownloadBehavior;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.ExportingPanel;

public abstract class XlsxDownloadButtonPanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(XlsxDownloadButtonPanel.class);
    private static final String ID_EXPORT_DATA = "exportXlsxData";
    List<Integer> exportableColumnsIndex = new ArrayList<>();

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public XlsxDownloadButtonPanel(String id) {
        super(id);
    }

    private static final long serialVersionUID = 1L;

    private void initLayout() {
        AbstractDataExporter xlsxDataExporter = new AbstractDataExporter(Model.of("XLSX"), "text/xlsx", "xlsx") {

            @Override
            public <T> void exportData(IDataProvider<T> dataProvider,
                    List<IExportableColumn<T, ?>> columns, OutputStream outputStream) {
                if (dataProvider instanceof SelectableBeanContainerDataProvider) {
                    ((SelectableBeanContainerDataProvider) dataProvider).setExport(true);
                }
                try {
                    ((BaseSortableDataProvider) dataProvider).setExportSize(true);
                    exportDataToXLSX(dataProvider, getExportableColumns(), outputStream);
                    ((BaseSortableDataProvider) dataProvider).setExportSize(false);
                } catch (Exception ex) {
                    LOGGER.error("Unable to export data,", ex);
                } finally {
                    if (dataProvider instanceof SelectableBeanContainerDataProvider) {
                        ((SelectableBeanContainerDataProvider) dataProvider).setExport(false);
                    }
                }
            }

            private <T> void exportDataToXLSX(
                    IDataProvider<T> dataProvider,
                    List<IExportableColumn<Object, ?>> columns,
                    OutputStream outputStream
            ) {
                Workbook workbook = new XSSFWorkbook();
                Sheet sheet = workbook.createSheet("Exported data");
                try {
                    writeHeaders(columns, sheet);
                    writeData(dataProvider, columns, sheet);
                    workbook.write(outputStream);
                    workbook.close();
                } catch (IOException ex) {
                    throw new IllegalStateException("Error during export to XLSX " + ex.getMessage(), ex);
                }
            }

            private <T> void writeHeaders(List<IExportableColumn<T, ?>> columns, Sheet sheet) throws IOException {
                Row header = sheet.createRow(0);
                int index = 0;
                for (IExportableColumn<T, ?> col : columns) {
                    IModel<String> displayModel = col.getDisplayModel();
                    String display = this.wrapModel(displayModel).getObject();
                    sheet.setColumnWidth(index, 5000);
                    header.createCell(index++).setCellValue(display);
                }
            }

            private <T> void writeData(IDataProvider<T> dataProvider, List<IExportableColumn<Object,?>> columns, Sheet sheet) throws IOException {
                long numberOfRows = dataProvider.size();
                Iterator<? extends T> rowIterator = dataProvider.iterator(0L, numberOfRows);

                int indexRow = 1;
                while (rowIterator.hasNext()) {
                    Row sheetRow = sheet.createRow(indexRow++);
                    T row = (T) rowIterator.next();

                    int indexColumn = 0;
                    for (IExportableColumn<Object, ?> col : columns) {
                        IModel<?> dataModel = col.getDataModel((IModel<Object>) dataProvider.model(row));
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

            private <T> IModel<T> wrapModel(IModel<T> model) {
                if (model == null || model.getObject() == null) {
                    return () -> (T) "";
                }
                if (model.getObject() instanceof Referencable) {
                    return () -> {
                        String value = WebModelServiceUtils.resolveReferenceName((Referencable) model.getObject(), getPageBase());
                        return (T) (value == null ? "" : value);
                    };
                }
                return model;
            }
        };

        IModel<String> name = Model.of("");
        final AbstractAjaxDownloadBehavior ajaxDownloadBehavior = new AbstractAjaxDownloadBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            public IResourceStream getResourceStream() {
                return new ExportToolbar.DataExportResourceStreamWriter(xlsxDataExporter, getDataTable());
            }

            public String getFileName() {
                if (StringUtils.isEmpty(name.getObject())) {
                    return XlsxDownloadButtonPanel.this.getFilename();
                }
                return name.getObject();
            }
        };

        add(ajaxDownloadBehavior);

        AjaxIconButton exportDataLink = new AjaxIconButton(ID_EXPORT_DATA, new Model<>("fa fa-download"),
                createStringResource("XlsxDownloadButtonPanel.export")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                long exportSizeLimit = -1;
                try {
                    CompiledGuiProfile adminGuiConfig = getPageBase().getCompiledGuiProfile();
                    if (adminGuiConfig.getDefaultExportSettings() != null && adminGuiConfig.getDefaultExportSettings().getSizeLimit() != null) {
                        exportSizeLimit = adminGuiConfig.getDefaultExportSettings().getSizeLimit();
                    }
                } catch (Exception ex) {
                    LOGGER.error("Unable to get XLSX export size limit,", ex);
                }
                boolean askForSizeLimitConfirmation;
                if (exportSizeLimit < 0) {
                    askForSizeLimitConfirmation = false;
                } else {
                    IDataProvider<?> dataProvider = getDataTable().getDataProvider();
                    long size = dataProvider.size();
                    askForSizeLimitConfirmation = size > exportSizeLimit;
                }
                Long useExportSizeLimit = null;
                if (askForSizeLimitConfirmation) {
                    useExportSizeLimit = exportSizeLimit;
                }
                exportableColumnsIndex.clear();
                ExportingPanel exportingPanel = new ExportingPanel(getPageBase().getMainPopupBodyId(),
                        getDataTable(), exportableColumnsIndex, useExportSizeLimit, name) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void exportPerformed(AjaxRequestTarget target) {
                        ajaxDownloadBehavior.initiate(target);
                    }

                    @Override
                    protected IModel<String> getConfirmationMessage(final Long exportSizeLimit) {
                        return getPageBase().createStringResource("XlsxDownloadButtonPanel.confirmationMessage", exportSizeLimit);
                    }
                };
                getPageBase().showMainPopup(exportingPanel, target);
            }
        };
        add(exportDataLink);
    }

    private <T> List<IExportableColumn<T, ?>> getExportableColumns() {
        List<IExportableColumn<T, ?>> exportableColumns = new ArrayList<>();
        List<? extends IColumn<?, ?>> allColumns = getDataTable().getColumns();
        for (Integer index : exportableColumnsIndex) {
            exportableColumns.add((IExportableColumn) allColumns.get(index));
        }
        return exportableColumns;
    }

    protected abstract DataTable<?, ?> getDataTable();

    protected abstract String getFilename();
}
