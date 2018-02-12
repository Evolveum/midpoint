package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.CountStatus;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
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
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ExportRepositoryAction extends RepositoryAction<ExportOptions> {

    private static final String DOT_CLASS = ExportRepositoryAction.class.getName() + ".";

    private static final String OPERATION_EXPORT = DOT_CLASS + "export";

    @Override
    public void execute() throws Exception {
        try (Writer writer = NinjaUtils.createWriter(options.getOutput(), context.getCharset(), options.isZip())) {
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
        if (type == null) {
            throw new NinjaException("Type must be defined");
        }

        Collection<SelectorOptions<GetOperationOptions>> opts = Collections.emptyList();
        if (options.isRaw()) {
            opts = GetOperationOptions.createRawCollection();
        }
        addIncludeOptionsForExport(opts, type.getClassDefinition());

        OperationResult result = new OperationResult(OPERATION_EXPORT);

        log.info("Starting export");

        PrismObject object = repository.getObject(type.getClassDefinition(), options.getOid(), opts, result);

        PrismSerializer<String> serializer = prismContext.xmlSerializer();
        String xml = serializer.serialize(object);
        writer.write(xml);

        handleResultOnFinish(result, null, "Export finished");
    }

    private void exportByFilter(final Writer writer) throws SchemaException, IOException {
        OperationResult result = new OperationResult(OPERATION_EXPORT);

        CountStatus status = new CountStatus();
        status.start();

        log.info("Starting export");

        ObjectTypes type = options.getType();
        if (type != null) {
            exportByType(type, writer, status, result);
        } else {
            for (ObjectTypes t : ObjectTypes.values()) {
                if (Modifier.isAbstract(t.getClassDefinition().getModifiers())) {
                    continue;
                }

                exportByType(t, writer, status, result);
            }
        }

        handleResultOnFinish(result, status, "Export finished");
    }

    private void exportByType(ObjectTypes type, Writer writer, CountStatus status, OperationResult result)
            throws SchemaException, IOException {

        RepositoryService repository = context.getRepository();

        PrismContext prismContext = context.getPrismContext();
        PrismSerializer<String> serializer = prismContext.xmlSerializer();

        Collection<SelectorOptions<GetOperationOptions>> opts;
        if (options.isRaw()) {
            opts = GetOperationOptions.createRawCollection();
        } else {
            opts = new ArrayList<>();
        }
        addIncludeOptionsForExport(opts, type.getClassDefinition());

        ObjectQuery query = NinjaUtils.createObjectQuery(options.getFilter(), context);

        ResultHandler handler = (object, parentResult) -> {
            try {
                String xml = serializer.serialize(object);
                writer.write(xml);

                status.incrementCount();

                logCountProgress(status);
            } catch (Exception ex) {
                return false;
            }

            return true;
        };

        repository.searchObjectsIterative(type.getClassDefinition(), query, handler, opts, false, result);
    }
}
