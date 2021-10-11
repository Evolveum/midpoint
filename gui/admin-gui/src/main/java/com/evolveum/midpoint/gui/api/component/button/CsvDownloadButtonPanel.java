/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.button;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
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

    public CsvDownloadButtonPanel(String id, boolean canCountBeforeExporting) {
        super(id);
        this.canCountBeforeExporting = canCountBeforeExporting;
        initLayout();
    }

    public CsvDownloadButtonPanel(String id) {
        this(id, true);
    }

    private static final long serialVersionUID = 1L;

    private void initLayout() {
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
                    super.exportData(dataProvider, columns, outputStream);
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
        final AbstractAjaxDownloadBehavior ajaxDownloadBehavior = new AbstractAjaxDownloadBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            public IResourceStream getResourceStream() {
                return new ExportToolbar.DataExportResourceStreamWriter(csvDataExporter, getDataTable());
            }

            public String getFileName() {
                return CsvDownloadButtonPanel.this.getFilename();
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
                if (askForSizeLimitConfirmation) {
                    ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
                            createStringResource("CsvDownloadButtonPanel.confirmationMessage", exportSizeLimit)) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void yesPerformed(AjaxRequestTarget target) {
                            ajaxDownloadBehavior.initiate(target);
                        }
                    };
                    getPageBase().showMainPopup(confirmationPanel, target);
                } else {
                    ajaxDownloadBehavior.initiate(target);
                }
            }
        };
        add(exportDataLink);
    }

    protected abstract DataTable<?,?> getDataTable();

    protected abstract String getFilename();

}
