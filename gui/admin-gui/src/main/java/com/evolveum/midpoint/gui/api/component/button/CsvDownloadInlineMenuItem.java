/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import java.io.OutputStream;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.CSVDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;

public class CsvDownloadInlineMenuItem extends ExportDownloadInlineMenuItem {

    private static final Trace LOGGER = TraceManager.getTrace(CsvDownloadInlineMenuItem.class);
    private static final long serialVersionUID = 1L;

    public CsvDownloadInlineMenuItem(ContainerableListPanel component, String fileNamePrefix) {
        super(ColumnUtils.createStringResource("CsvDownloadButtonPanel.export"), component, fileNamePrefix);
    }

    @Override
    protected String getFileExtension() {
        return ".csv";
    }

    protected IModel<String> getConfirmationMessage(final Long exportSizeLimit) {
        return WebComponentUtil.getPageBase(component).createStringResource(
                "CsvDownloadButtonPanel.confirmationMessage",
                exportSizeLimit
        );
    }

    @Override
    protected AbstractDataExporter getDataExporter() {
        return new CSVDataExporter() {
            private static final long serialVersionUID = 1L;

            @Override
            public <T> void exportData(IDataProvider<T> dataProvider,
                    List<IExportableColumn<T, ?>> columns, OutputStream outputStream) {
                if (dataProvider instanceof SelectableBeanContainerDataProvider) {
                    ((SelectableBeanContainerDataProvider) dataProvider).setExport(true);
                }
                try {
                    ((BaseSortableDataProvider) dataProvider).setExportSize(true);
                    super.exportData(dataProvider, getExportableColumns(), outputStream);
                    ((BaseSortableDataProvider) dataProvider).setExportSize(false);
                } catch (Exception ex) {
                    LOGGER.error("Unable to export data,", ex);
                } finally {
                    if (dataProvider instanceof SelectableBeanContainerDataProvider) {
                        ((SelectableBeanContainerDataProvider) dataProvider).setExport(false);
                    }
                }
            }

            @Override
            protected String quoteValue(String value) {
                value = value.replaceAll("^\\[|\\]$", "");
                return super.quoteValue(value);
            }

            @Override
            protected <T> IModel<T> wrapModel(IModel<T> model) {
                return super.wrapModel(getModel(model));
            }
        };
    }
}
