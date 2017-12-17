package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ImportOptions;
import com.evolveum.midpoint.ninja.util.CountStatus;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.io.input.ReaderInputStream;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.*;
import java.nio.charset.Charset;
import java.util.zip.ZipInputStream;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportRepositoryAction extends RepositoryAction<ImportOptions> {

    private static final String DOT_CLASS = ImportRepositoryAction.class.getName() + ".";

    private static final String OPERATION_IMPORT = DOT_CLASS + "import";

    @Override
    public void execute() throws Exception {
        try (Reader reader = createReader()) {

            String oid = options.getOid();

            if (oid != null) {
                importByOid(reader);
            } else {
                importByFilter(reader);
            }
        }
    }

    @Override
    protected LogTarget getInfoLogTarget() {
        if (options.getInput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private Reader createReader() throws IOException {
        Charset charset = context.getCharset();

        File input = options.getInput();

        InputStream is;
        if (input != null) {
            if (!input.exists()) {
                throw new NinjaException("Import file '" + input.getPath() + "' doesn't exist");
            }

            is = new FileInputStream(input);
        } else {
            is = System.in;
        }

        if (options.isZip()) {
            is = new ZipInputStream(is);
        }

        return new InputStreamReader(is, charset);
    }

    private void importByOid(Reader reader) throws SchemaException, ObjectNotFoundException, IOException {
        InOidFilter filter = InOidFilter.createInOid(options.getOid());

        importByFilter(filter, true, reader);
    }

    private void importByFilter(Reader reader) throws SchemaException, IOException {
        ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), context);

        importByFilter(filter, false, reader);
    }

    private void importByFilter(ObjectFilter filter, boolean stopAfterFound, Reader reader) {
        ApplicationContext appContext = context.getApplicationContext();
        PrismContext prismContext = appContext.getBean(PrismContext.class);
        MatchingRuleRegistry matchingRuleRegistry = appContext.getBean(MatchingRuleRegistry.class);

        CountStatus status = new CountStatus();
        status.start();

        OperationResult result = new OperationResult(OPERATION_IMPORT);

        EventHandler handler = new EventHandler() {

            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree,
                                           OperationResult objectResult) {
                return EventResult.cont();
            }

            @Override
            public <T extends Objectable> EventResult postMarshall(PrismObject<T> object, Element objectElement,
                                                                   OperationResult objectResult) {

                try {
                    if (filter != null) {
                        boolean match = ObjectQuery.match(object, filter, matchingRuleRegistry);

                        if (!match) {
                            status.incrementSkipped();

                            return EventResult.skipObject("Object doesn't match filter");
                        }
                    }

                    ObjectTypes type = options.getType();
                    if (type != null && !type.getClassDefinition().equals(object.getCompileTimeClass())) {
                        status.incrementSkipped();

                        return EventResult.skipObject("Type doesn't match");
                    }

                    importObject(object, objectResult);

                    status.incrementCount();

                    if (status.getLastPrintout() + NinjaUtils.COUNT_STATUS_LOG_INTERVAL < System.currentTimeMillis()) {
                        logInfo("Imported: {}, skipped: {}, avg: {}ms",
                                status.getCount(), status.getSkipped(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));

                        status.lastPrintoutNow();
                    }
                } catch (Exception ex) {
                    throw new NinjaException("Couldn't import object, reason: " + ex.getMessage(), ex);
                }

                return stopAfterFound ? EventResult.stop() : EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
            }
        };

        logInfo("Starting import");

        Validator validator = new Validator(prismContext, handler);
        validator.validate(new ReaderInputStream(reader, context.getCharset()), result, OPERATION_IMPORT);

        result.recomputeStatus();

        if (result.isAcceptable()) {
            logInfo("Import finished. Processed: {} objects, skipped: {}, avg. {}ms",
                    status.getCount(), status.getSkipped(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));
        } else {
            logError("Import finished with some problems, reason: {}. Processed: {} objects, skipped: {}, avg. {}ms",
                    result.getMessage(), status.getCount(), status.getSkipped(), NinjaUtils.DECIMAL_FORMAT.format(status.getAvg()));

            if (context.isVerbose()) {
                logError("Full result\n{}", result.debugDumpLazily());
            }
        }
    }

    private String importObject(PrismObject object, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {

        RepositoryService repository = context.getRepository();
        RepoAddOptions opts = createRepoAddOptions();

        return repository.addObject(object, opts, result);
    }

    private RepoAddOptions createRepoAddOptions() {
        RepoAddOptions opts = new RepoAddOptions();
        opts.setOverwrite(options.isOverwrite());
        opts.setAllowUnencryptedValues(options.isAllowUnencryptedValues());

        return opts;
    }
}
