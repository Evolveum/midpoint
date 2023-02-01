/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration.component;

import static com.evolveum.midpoint.prism.SerializationOptions.createSerializeForExport;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.file.Files;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromFile;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author lazyman
 */
public class PageDebugDownloadBehaviour<T extends ObjectType> extends AjaxDownloadBehaviorFromFile {

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugDownloadBehaviour.class);

    private static final String DOT_CLASS = PageDebugDownloadBehaviour.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "loadObjects";
    private static final String OPERATION_CREATE_DOWNLOAD_FILE = DOT_CLASS + "createDownloadFile";

    private boolean exportAll;
    private Class<T> type;
    private boolean useZip;
    private boolean showAllItems;
    private ObjectQuery query;

    public boolean isExportAll() {
        return exportAll;
    }

    public void setExportAll(boolean exportAll) {
        this.exportAll = exportAll;
    }

    public Class<? extends ObjectType> getType() {
        if (type == null) {
            return ObjectType.class;
        }
        return type;
    }

    public void setType(Class<T> type) {
        this.type = type;
    }

    public ObjectQuery getQuery() {
        return query;
    }

    public void setQuery(ObjectQuery query) {
        this.query = query;
    }

    public boolean isUseZip() {
        return useZip;
    }

    public void setUseZip(boolean useZip) {
        this.useZip = useZip;
    }

    public boolean isShowAllItems() {
        return showAllItems;
    }

    public void setShowAllItems(boolean showAllItems) {
        this.showAllItems = showAllItems;
    }

    @Override
    protected File initFile() {
        PageBase page = getPage();

        OperationResult result = new OperationResult(OPERATION_CREATE_DOWNLOAD_FILE);
        MidPointApplication application = page.getMidpointApplication();
        WebApplicationConfiguration config = application.getWebApplicationConfiguration();
        File folder = new File(config.getExportFolder());
        if (!folder.exists() || !folder.isDirectory()) {
            folder.mkdir();
        }

        String suffix = isUseZip() ? "zip" : "xml";
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_m_s"));
        String fileName = "ExportedData_" + getType().getSimpleName() + "_" + currentTime + "." + suffix;
        File file = new File(folder, fileName);

        LOGGER.debug("Creating file '{}'.", file.getAbsolutePath());
        try (Writer writer = createWriter(file)) {
            LOGGER.debug("Exporting objects.");
            dumpHeader(writer);
            dumpObjectsToStream(writer, result);
            dumpFooter(writer);
            LOGGER.debug("Export finished.");

            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't init download link", ex);
            result.recordFatalError(getPage().createStringResource("PageDebugDownloadBehaviour.message.initFile.fatalError").getString(), ex);
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            page.showResult(result);
            page.getSession().error(page.getString("pageDebugList.message.createFileException"));
            LOGGER.debug("Removing file '{}'.", file.getAbsolutePath());
            Files.remove(file);

            throw new RestartResponseException(PageDebugList.class);
        }

        return file;
    }

    private Writer createWriter(File file) throws IOException {
        OutputStream stream;
        if (isUseZip()) {
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file));
            String fileName = file.getName();
            if (StringUtils.isNotEmpty(file.getExtension())) {
                fileName = fileName.replaceAll(file.getExtension() + "$", "xml");
            }
            ZipEntry entry = new ZipEntry(fileName);
            out.putNextEntry(entry);
            stream = out;
        } else {
            stream = new FileOutputStream(file);
        }

        return new OutputStreamWriter(stream);
    }

    private void dumpObjectsToStream(final Writer writer, OperationResult result) throws Exception {
        final PageBase page = getPage();

        ResultHandler<T> handler = (object, parentResult) -> {
            try {
                String xml = page.getPrismContext().xmlSerializer().options(createSerializeForExport()).serialize(object);
                writer.write('\t');
                writer.write(xml);
                writer.write('\n');
            } catch (IOException | SchemaException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }
            return true;
        };

        ModelService service = page.getModelService();
        GetOperationOptionsBuilder optionsBuilder = page.getSchemaService().getOperationOptionsBuilder()
                .raw()
                .resolveNames();
        if (showAllItems) {
            optionsBuilder = optionsBuilder.retrieve();
        }
        service.searchObjectsIterative(type, query, handler, optionsBuilder.build(),
                page.createSimpleTask(OPERATION_SEARCH_OBJECT), result);
    }

    private PageBase getPage() {
        return (PageBase) getComponent().getPage();
    }

    private void dumpFooter(Writer writer) throws IOException {
        writer.write("</objects>");
    }

    private void dumpHeader(Writer writer) throws IOException {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.write("<objects xmlns=\"");
        writer.write(SchemaConstantsGenerated.NS_COMMON);
        writer.write("\"\n");
        writer.write("\txmlns:c=\"");
        writer.write(SchemaConstantsGenerated.NS_COMMON);
        writer.write("\"\n");
        writer.write("\txmlns:org=\"");
        writer.write(SchemaConstants.NS_ORG);
        writer.write("\">\n");
    }
}
