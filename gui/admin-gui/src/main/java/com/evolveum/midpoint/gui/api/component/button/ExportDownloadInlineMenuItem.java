/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractDataExporter;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AsyncWebProcess;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.ExportingPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.security.MidPointApplication;

public abstract class ExportDownloadInlineMenuItem extends InlineMenuItem {

    private static final Trace LOGGER = TraceManager.getTrace(ExportDownloadInlineMenuItem.class);

    protected final ContainerableListPanel component;

    private final String fileNamePrefix;

    private IModel<String> name;

    protected List<Integer> exportableColumnsIndex = new ArrayList<>();

    @Serial
    private static final long serialVersionUID = 1L;

    public ExportDownloadInlineMenuItem(IModel<String> label, ContainerableListPanel component, String fileNamePrefix) {
        super(label);
        this.component = component;
        this.fileNamePrefix = fileNamePrefix;
        initLayout();
    }

    private void initLayout() {
        name = Model.of("");
    }

    private String getVerifiedFileNameWithExtension() {
        String fileName = getFilename();
        return fileName.toLowerCase().endsWith(getFileExtension()) ? fileName : fileName + getFileExtension();
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
                    LOGGER.error("Unable to get export size limit,", ex);
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
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void exportPerformed(AjaxRequestTarget target) {
                        startExportProcess(target);
                        component.getPageBase().startDownloadTimerBehavior(target);
                    }

                    @Override
                    protected IModel<String> getConfirmationMessage(final Long exportSizeLimit) {
                        return ExportDownloadInlineMenuItem.this.getConfirmationMessage(exportSizeLimit);
                    }
                };
                WebComponentUtil.getPageBase(component).showMainPopup(exportingPanel, target);
            }
        };
    }

    protected <T> List<IExportableColumn<T, ?>> getExportableColumns() {
        List<IExportableColumn<T, ?>> exportableColumns = new ArrayList<>();
        List<? extends IColumn<?, ?>> allColumns = getDataTable().getColumns();
        for (Integer index : exportableColumnsIndex) {
            exportableColumns.add((IExportableColumn) allColumns.get(index));
        }
        return exportableColumns;
    }

    protected DataTable<?, ?> getDataTable() {
        return component.getTable().getDataTable();
    }

    private String getFilename() {
        if (StringUtils.isNotEmpty(name.getObject())) {
            return name.getObject();
        }

        return getDefaultFilename();
    }

    private String getDefaultFilename() {
        return fileNamePrefix + "_" +
                ColumnUtils.createStringResource("MainObjectListPanel.exportFileName").getString();
    }

    protected abstract String getFileExtension();

    protected abstract AbstractDataExporter getDataExporter();

    protected abstract IModel<String> getConfirmationMessage(final Long exportSizeLimit);

    protected <T> IModel<T> getModel(IModel<T> model) {
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
        return model;
    }

    private void startExportProcess(AjaxRequestTarget target) {
        var pageBase = component.getPageBase();
        AsyncWebProcess<String> exportProcess = pageBase.getAsyncWebProcessManager()
                .createProcess(getVerifiedFileNameWithExtension());
        var processId = exportProcess.getId();
        pageBase.getSessionStorage().addExportProcessId(processId);
        pageBase.getAsyncWebProcessManager().submit(processId, createFileLoader());
        new Toast()
                .info()
                .title(pageBase.createStringResource("ExportDownloadInlineMenuItem.export.title").getString())
                .body(pageBase.createStringResource("ExportDownloadInlineMenuItem.export.message").getString())
                .show(target);
    }

    private Callable<File> createFileLoader() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();
        MidPointApplication application = MidPointApplication.get();
        final SecurityContextManager secManager = application.getSecurityContextManager();

        String fileName = getDefaultFilename();
        String fileExtension = getFileExtension();

        IDataProvider<?> provider = getDataTable().getDataProvider();
        Session session = Session.get();

        return new SecurityContextAwareCallable<>(secManager, auth, connInfo) {

            @Override
            public File callWithContextPrepared() {
                try {
                    ThreadContext.setApplication(application);
                    ThreadContext.setSession(session);

                    String midpointHome = System.getProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY);
                    String tmpFilePath = StringUtils.isNotEmpty(midpointHome) ? midpointHome + "/tmp" : ".";
                    File file = File.createTempFile(fileName, fileExtension, new File(tmpFilePath));

                    try (OutputStream os = new FileOutputStream(file)) {
                        getDataExporter().exportData(provider, getExportableColumns(), os);
                    }
                    return file;
                } catch (IOException e) {
                    LOGGER.error("Failed to generate the file for export.");
                }
                return null;
            }
        };
    }
}
