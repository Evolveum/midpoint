package com.evolveum.midpoint.web.page.admin.configuration.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromFile;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.util.file.File;
import org.apache.wicket.util.file.Files;

import java.io.*;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.evolveum.midpoint.prism.SerializationOptions.createSerializeForExport;

/**
 * @author lazyman
 */
public class PageDebugDownloadBehaviour extends AjaxDownloadBehaviorFromFile {

    private static final Trace LOGGER = TraceManager.getTrace(PageDebugDownloadBehaviour.class);

    private static final String DOT_CLASS = PageDebugDownloadBehaviour.class.getName() + ".";
    private static final String OPERATION_SEARCH_OBJECT = DOT_CLASS + "loadObjects";
    private static final String OPERATION_CREATE_DOWNLOAD_FILE = DOT_CLASS + "createDownloadFile";

    private boolean exportAll;
    private Class<? extends ObjectType> type;
    private boolean useZip;
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

    public void setType(Class<? extends ObjectType> type) {
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
        String fileName = "ExportedData_" + getType().getSimpleName() + "_" + System.currentTimeMillis() + "." + suffix;
        File file = new File(folder, fileName);

        Writer writer = null;
        try {
            LOGGER.debug("Creating file '{}'.", file.getAbsolutePath());
            writer = createWriter(file);
            LOGGER.debug("Exporting objects.");
            dumpHeader(writer);
            dumpObjectsToStream(writer, result);
            dumpFooter(writer);
            LOGGER.debug("Export finished.");

            result.recomputeStatus();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't init download link", ex);
            result.recordFatalError("Couldn't init download link", ex);
        } finally {
            if (writer != null) {
                IOUtils.closeQuietly(writer);
            }
        }

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            page.showResult(result);
            page.getSession().error(page.getString("pageDebugList.message.createFileException"));
            LOGGER.debug("Removing file '{}'.", new Object[]{file.getAbsolutePath()});
            Files.remove(file);

            throw new RestartResponseException(PageError.class);
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

    private <T extends ObjectType> void dumpObjectsToStream(final Writer writer, OperationResult result) throws Exception {
        final PageBase page = getPage();

        ResultHandler handler = (object, parentResult) -> {
            try {
                String xml = page.getPrismContext().xmlSerializer().options(createSerializeForExport()).serialize(object);
                writer.write('\t');
                writer.write(xml);
                writer.write('\n');
            } catch (IOException|SchemaException ex) {
                throw new SystemException(ex.getMessage(), ex);
            }
            return true;
        };

        ModelService service = page.getModelService();
        GetOperationOptions options = GetOperationOptions.createRaw();
        options.setResolveNames(true);
        Collection<SelectorOptions<GetOperationOptions>> optionsCollection = SelectorOptions.createCollection(options);
        WebModelServiceUtils.addIncludeOptionsForExportOrView(optionsCollection, type);
        service.searchObjectsIterative(type, query, handler, optionsCollection,
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
