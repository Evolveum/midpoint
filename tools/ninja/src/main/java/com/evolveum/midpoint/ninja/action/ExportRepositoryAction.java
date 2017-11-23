package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismSerializer;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import org.springframework.context.ApplicationContext;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.ZipOutputStream;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportRepositoryAction extends RepositoryAction<ExportOptions> {

    private static final String DOT_CLASS = ExportRepositoryAction.class.getName() + ".";

    private static final String OPERATION_EXPORT = DOT_CLASS + "export";

    @Override
    public void execute() throws Exception {
        try (Writer writer = createWriter()) {
            writer.write(NinjaUtils.XML_OBJECTS_PREFIX);

            String oid = options.getOid();

            if (oid != null) {
                exportByOid(writer);
            } else {
                exportByFilter(writer);
            }

            writer.write(NinjaUtils.XML_OBJECTS_SUFFIX);
        }
    }

    @Override
    protected LogTarget getInfoLogTarget() {
        if (options.getOutput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private void exportByOid(Writer writer) throws SchemaException, ObjectNotFoundException, IOException {
        RepositoryService repository = context.getRepository();
        ApplicationContext appContext = context.getApplicationContext();
        PrismContext prismContext = appContext.getBean(PrismContext.class);

        ObjectTypes type = options.getType();

        Collection<SelectorOptions<GetOperationOptions>> opts = Collections.emptyList();
        if (options.isRaw()) {
            opts = GetOperationOptions.createRawCollection();
        }

        OperationResult result = new OperationResult(OPERATION_EXPORT);

        PrismObject object = repository.getObject(type.getClassDefinition(), options.getOid(), opts, result);

        result.recomputeStatus();

        if (!result.isAcceptable()) {
            //todo show some warning
        }

        PrismSerializer<String> serializer = prismContext.xmlSerializer();
        String xml = serializer.serialize(object);
        writer.write(xml);
    }

    private void exportByFilter(final Writer writer) throws SchemaException, IOException {
        RepositoryService repository = context.getRepository();

        ApplicationContext appContext = context.getApplicationContext();

        PrismContext prismContext = appContext.getBean(PrismContext.class);
        PrismSerializer<String> serializer = prismContext.xmlSerializer();

        ObjectTypes type = options.getType();
        if (type == null) {
            //todo throw error
        }

        Collection<SelectorOptions<GetOperationOptions>> opts = Collections.emptyList();
        if (options.isRaw()) {
            opts = GetOperationOptions.createRawCollection();
        }

        ObjectQuery query = NinjaUtils.createObjectQuery(options.getFilter(), context);

        ResultHandler handler = (object, parentResult) -> {
            try {
                String xml = serializer.serialize(object);
                writer.write(xml);
            } catch (Exception ex) {
                return false;
            }

            return true;
        };

        OperationResult result = new OperationResult(OPERATION_EXPORT);

        repository.searchObjectsIterative(type.getClassDefinition(), query, handler, opts, false, result);

        result.recomputeStatus();

        if (!result.isAcceptable()) {
            //todo show some warning
        }
    }

    private Writer createWriter() throws IOException {
        Charset charset = context.getCharset();

        File output = options.getOutput();

        OutputStream os;
        if (output != null) {
            if (output.exists()) {
                throw new NinjaException("Export file '" + output.getPath() + "' already exists");
            }
            output.createNewFile();

            os = new FileOutputStream(output);
        } else {
            os = System.out;
        }

        if (options.isZip()) {
            os = new ZipOutputStream(os);
        }

        return new OutputStreamWriter(os, charset);
    }
}
