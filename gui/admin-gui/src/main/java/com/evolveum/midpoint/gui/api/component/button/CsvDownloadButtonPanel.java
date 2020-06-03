/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.dialog.ExportingPanel;

import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.CSVDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.ExportToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.IResourceStream;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.web.component.AbstractAjaxDownloadBehavior;
import com.evolveum.midpoint.web.component.AjaxIconButton;

public abstract class CsvDownloadButtonPanel extends BasePanel {

    private static final Trace LOGGER = TraceManager.getTrace(CsvDownloadButtonPanel.class);
    private static final String DOT_CLASS = CsvDownloadButtonPanel.class.getName() + ".";
    private static final String OPERATION_GET_EXPORT_SIZE_LIMIT = DOT_CLASS + "getDefaultExportSizeLimit";

    private static final String ID_EXPORT_DATA = "exportData";

    private final boolean canCountBeforeExporting;
    List<Integer> exportableColumnsIndex = new ArrayList<>();

    public CsvDownloadButtonPanel(String id, boolean canCountBeforeExporting, LoadableModel<Search> search) {
        super(id);
        this.canCountBeforeExporting = canCountBeforeExporting;
        initLayout(search);
    }

    public CsvDownloadButtonPanel(String id) {
        this(id, true, null);
    }

    private static final long serialVersionUID = 1L;

    private void initLayout(LoadableModel<Search> search) {
        CSVDataExporter csvDataExporter = new CSVDataExporter() {
            private static final long serialVersionUID = 1L;

            @Override
            public <T> void exportData(IDataProvider<T> dataProvider, List<IExportableColumn<T, ?>> columns,
                    OutputStream outputStream) throws IOException {
                if (dataProvider instanceof SelectableBeanObjectDataProvider) {
                    ((SelectableBeanObjectDataProvider) dataProvider).setExport(true);        // TODO implement more nicely
                }
                try {
                    ((BaseSortableDataProvider) dataProvider).setExportSize(true);
                    super.exportData(dataProvider, getExportableColumns(), outputStream);
                    ((BaseSortableDataProvider) dataProvider).setExportSize(false);
                } catch (Exception ex){
                    LOGGER.error("Unable to export data,", ex);
                } finally {
                    if (dataProvider instanceof SelectableBeanObjectDataProvider) {
                        ((SelectableBeanObjectDataProvider) dataProvider).setExport(false);
                    }
                }
            }

            @Override
            protected <T> IModel<T> wrapModel(IModel<T> model) {
                if(model.getObject() == null) {
                    return new IModel<T>() {

                        @Override
                        public T getObject() {
                            return (T)"";
                        }
                    };
                }
                return super.wrapModel(model);
            }
        };
        IModel<String> name = Model.of("");
        final AbstractAjaxDownloadBehavior ajaxDownloadBehavior = new AbstractAjaxDownloadBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            public IResourceStream getResourceStream() {
                return new ExportToolbar.DataExportResourceStreamWriter(csvDataExporter, getDataTable());
            }

            public String getFileName() {
                if (StringUtils.isEmpty(name.getObject())) {
                    return CsvDownloadButtonPanel.this.getFilename();
                }
                return name.getObject();
            }
        };

        add(ajaxDownloadBehavior);

        AjaxIconButton exportDataLink = new AjaxIconButton(ID_EXPORT_DATA, new Model<>("fa fa-download"),
                createStringResource("CsvDownloadButtonPanel.export")) {

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
                    LOGGER.error("Unable to get csv export size limit,", ex);
                }
                boolean askForSizeLimitConfirmation;
                if (exportSizeLimit < 0) {
                    askForSizeLimitConfirmation = false;
                } else {
                    if (canCountBeforeExporting) {
                        IDataProvider<?> dataProvider = getDataTable().getDataProvider();
                        long size = dataProvider.size();
                        askForSizeLimitConfirmation = size > exportSizeLimit;
                    } else {
                        askForSizeLimitConfirmation = true;     // size is unknown
                    }
                }
                Long useExportSizeLimit = null;
                if (askForSizeLimitConfirmation) {
                    useExportSizeLimit = exportSizeLimit;
                }
                exportableColumnsIndex.clear();
                ExportingPanel exportingPanel = new ExportingPanel(getPageBase().getMainPopupBodyId(),
                        getDataTable(), exportableColumnsIndex, useExportSizeLimit, search, name) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void exportPerformed(AjaxRequestTarget target) {
                        ajaxDownloadBehavior.initiate(target);
                    }

                    @Override
                    protected void createReportPerformed(String name, SearchFilterType filter, AjaxRequestTarget target) {
                        CsvDownloadButtonPanel.this.createReportPerformed(name, filter, exportableColumnsIndex, target);
                    }

                    @Override
                    public boolean isVisibleCreateReportOption() {
                        return CsvDownloadButtonPanel.this.isVisibleCreateReportOption();
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

    protected abstract DataTable<?,?> getDataTable();

    protected abstract String getFilename();

    protected abstract void createReportPerformed(String name, SearchFilterType filter, List<Integer> indexOfColumns, AjaxRequestTarget target);

    public boolean isVisibleCreateReportOption() {
        return true;
    }

}
