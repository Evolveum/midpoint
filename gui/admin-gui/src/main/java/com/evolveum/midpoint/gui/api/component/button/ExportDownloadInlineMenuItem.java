/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.button;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.provider.BaseSearchDataProvider;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AbstractAjaxDownloadBehavior;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.dialog.ExportingPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;

import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Application;
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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.wicket.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class ExportDownloadInlineMenuItem extends InlineMenuItem {

    private static final Trace LOGGER = TraceManager.getTrace(ExportDownloadInlineMenuItem.class);
    protected final ContainerableListPanel component;
    private final String fileNamePrefix;
    private AbstractAjaxDownloadBehavior ajaxDownloadBehavior;
    private IModel<String> name;
    protected List<Integer> exportableColumnsIndex = new ArrayList<>();

    private static final ExecutorService EXPORT_EXECUTOR = Executors.newSingleThreadExecutor();

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
//        ajaxDownloadBehavior = new AbstractAjaxDownloadBehavior() {
//            @Serial private static final long serialVersionUID = 1L;
//
//            @Override
//            public IResourceStream getResourceStream() {
//                if (future == null || !future.isDone()) {
//                    LOGGER.error("Failed to load the file.");
//                }
//                try {
//                    timer.stop(null);
//                    return new FileResourceStream(future.get());
//                } catch (Exception e) {
//                    LOGGER.error("Failed to load the file.");
//                }
//                return null;
//            }
//
//            public String getFileName() {
//                if (StringUtils.isEmpty(name.getObject())) {
//                    return ExportDownloadInlineMenuItem.this.getFilename();
//                }
//                return name.getObject();
//            }
//        };
//        component.add(ajaxDownloadBehavior);

//        timer = new AbstractAjaxTimerBehavior(Duration.ofMillis(1000)) {
//            @Serial private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void onTimer(AjaxRequestTarget target) {
//                if (future != null && future.isDone()) {
//                    stop(target); // stop polling
//
//                    // trigger download
//                    ajaxDownloadBehavior.initiate(target);
//                }
//            }
//        };
//        component.add(timer);
//        timer.stop(null);
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
//                        String processId = PROCESS_ID_PREFIX + System.currentTimeMillis();
//                        AsyncWebProcess<?> webProcess = initAsyncWebProcess(processId);
                        startExportProcess();
                        component.getPageBase().startDownloadTimerBehavior(target);
//                        Session session = Session.get();
//                        SecurityContext context = SecurityContextHolder.getContext();
//                        Application application = Application.get();

//                        component.getPageBase().runPrivileged(() -> {
//                                    future = EXPORT_EXECUTOR.submit(() -> {
//                                        ThreadContext.setApplication(application);
//                                        ThreadContext.setSession(session);
//                                        SecurityContextHolder.setContext(context);
//
//                                        File file = File.createTempFile("export-", ".xlsx");
//
//                                        IDataProvider<?> provider = getDataTable().getDataProvider();
//
//                                        try (OutputStream os = new FileOutputStream(file)) {
//                                            getDataExporter().exportData(provider, getExportableColumns(), os);
//                                        }
//
//                                        return file;
//                                    });
//                                    return null;
//                                });
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

    protected String getFilename() {
        return fileNamePrefix +
                "_" +
                ColumnUtils
                        .createStringResource("MainObjectListPanel.exportFileName")
                        .getString() +
                getFileExtension();
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

    private void startExportProcess() {
        var pageBase = component.getPageBase();
        pageBase.getAsyncWebProcessManager().createProcess(PageBase.EXPORT_PROCESS_ID, null);
        pageBase.getAsyncWebProcessManager().submit(PageBase.EXPORT_PROCESS_ID, createFileLoader());
    }

    private Callable<File> createFileLoader() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        final HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();
        MidPointApplication application = MidPointApplication.get();
        final SecurityContextManager secManager = application.getSecurityContextManager();

        String fileName = getFilename();
        String fileExtension = getFileExtension();

        BaseSearchDataProvider provider = (BaseSearchDataProvider) getDataTable().getDataProvider();
        AbstractDataExporter dataExporter = getDataExporter();
        return new SecurityContextAwareCallable<>(secManager, auth, connInfo) {

            @Override
            public File callWithContextPrepared() {
                try {
                     ThreadContext.setApplication(application);

                    File file = File.createTempFile(fileName, fileExtension);

                    try (OutputStream os = new FileOutputStream(file)) {
                        dataExporter.exportData(provider, getExportableColumns(), os);
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
