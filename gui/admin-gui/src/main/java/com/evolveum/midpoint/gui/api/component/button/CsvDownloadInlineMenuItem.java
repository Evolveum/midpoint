/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSortableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AbstractAjaxDownloadBehavior;
import com.evolveum.midpoint.web.component.dialog.ExportingPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;

public abstract class CsvDownloadInlineMenuItem extends InlineMenuItem {

    private static final Trace LOGGER = TraceManager.getTrace(CsvDownloadInlineMenuItem.class);
    private final Component component;
    private AbstractAjaxDownloadBehavior ajaxDownloadBehavior;
    private IModel<String> name;
    List<Integer> exportableColumnsIndex = new ArrayList<>();

    public CsvDownloadInlineMenuItem(IModel<String> label, Component component) {
        super(label);
        this.component = component;
        initLayout();
    }

    private static final long serialVersionUID = 1L;

    private void initLayout() {
        CSVDataExporter csvDataExporter = new CSVDataExporter() {
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
                if (model == null || model.getObject() == null) {
                    return () -> (T) "";
                }
                if (model.getObject() instanceof Referencable) {
                    return () -> {
                        String value = WebModelServiceUtils.resolveReferenceName(
                                (Referencable) model.getObject(),
                                WebComponentUtil.getPageBase(component)
                        );
                        return (T) (value == null ? "" : value);
                    };
                }
                return super.wrapModel(model);
            }
        };
        name = Model.of("");
        ajaxDownloadBehavior = new AbstractAjaxDownloadBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            public IResourceStream getResourceStream() {
                return new ExportToolbar.DataExportResourceStreamWriter(csvDataExporter, getDataTable());
            }

            public String getFileName() {
                if (StringUtils.isEmpty(name.getObject())) {
                    return CsvDownloadInlineMenuItem.this.getFilename();
                }
                return name.getObject();
            }
        };
        component.add(ajaxDownloadBehavior);
    }

    @Override
    public InlineMenuItemAction initAction() {
        return new InlineMenuItemAction() {
            @Override
            public void onClick(AjaxRequestTarget target) {
                long exportSizeLimit = -1;
                try {
                    CompiledGuiProfile adminGuiConfig = WebComponentUtil.getPageBase(component).getCompiledGuiProfile();
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
                    IDataProvider<?> dataProvider = getDataTable().getDataProvider();
                    long size = dataProvider.size();
                    askForSizeLimitConfirmation = size > exportSizeLimit;
                }
                Long useExportSizeLimit = null;
                if (askForSizeLimitConfirmation) {
                    useExportSizeLimit = exportSizeLimit;
                }
                exportableColumnsIndex.clear();
                ExportingPanel exportingPanel = new ExportingPanel(WebComponentUtil.getPageBase(component).getMainPopupBodyId(),
                        getDataTable(), exportableColumnsIndex, useExportSizeLimit, name) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void exportPerformed(AjaxRequestTarget target) {
                        ajaxDownloadBehavior.initiate(target);
                    }
                };
                WebComponentUtil.getPageBase(component).showMainPopup(exportingPanel, target);
            }
        };
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
