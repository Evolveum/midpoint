/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.worker;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.LegacyValidator;
import com.evolveum.midpoint.ninja.action.BasicImportOptions;
import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportProducerWorker<T extends Containerable>
        extends BaseWorker<BasicImportOptions, T> {

    private final ObjectFilter filter;
    private final boolean stopAfterFound;
    private final boolean continueOnInputError;

    private String currentOid = null;
    private boolean convertMissingType = false;
    private boolean compatMode = false;

    public ImportProducerWorker(
            NinjaContext context, BasicImportOptions options, BlockingQueue<T> queue,
            OperationStatus operation, ObjectFilter filter, boolean stopAfterFound, boolean continueOnInputError) {
        super(context, options, queue, operation);

        this.filter = filter;
        this.stopAfterFound = stopAfterFound;
        this.continueOnInputError = continueOnInputError;
    }

    @Override
    public void run() {
        operation.start();

        File inputFile = options.getInput();
        try {
            if (!inputFile.exists()) {
                context.getLog().error("Input file '{}' doesn't exist", inputFile.getPath());
            } else if (inputFile.isDirectory()) {
                importDirectory(inputFile);
            } else {
                importFile(inputFile);
            }
        } finally {
            markDone();

            if (isWorkersDone()) {
                if (!operation.isFinished()) {
                    operation.producerFinish();
                }
            }
        }
    }

    private void importFile(File inputFile) {
        Log log = context.getLog();

        try (InputStream input = openInputStream(inputFile)) {
            if (!options.isZip()) {
                processStream(input);
            } else {
                ZipInputStream zis = new ZipInputStream(input);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    log.info("Processing file {}", entry.getName());
                    processStream(zis);
                }
            }
        } catch (Exception ex) {
            log.error("Unexpected error occurred", ex);
        }
    }

    private void importDirectory(File inputFile) {
        Log log = context.getLog();

        Collection<File> files = FileUtils.listFiles(inputFile, new String[] { "xml" }, true);
        for (File file : files) {
            try (InputStream is = new FileInputStream(file)) {
                log.info("Processing file {}", file.getName());
                processStream(is);
            } catch (Exception ex) {
                log.error("Unexpected error occurred", ex);
            }
        }
    }

    private InputStream openInputStream(File input) throws IOException {
        InputStream is;
        if (input != null) {
            if (!input.exists()) {
                throw new NinjaException("Import file '" + input.getPath() + "' doesn't exist");
            }

            is = new FileInputStream(input);
        } else {
            is = System.in;
        }

        return is;
    }

    private void processStream(InputStream input) {
        ApplicationContext appContext = context.getApplicationContext();
        PrismContext prismContext = appContext.getBean(PrismContext.class);
        MatchingRuleRegistry matchingRuleRegistry = appContext.getBean(MatchingRuleRegistry.class);

        EventHandler<T> handler = new EventHandler<>() {
            @Override
            public EventResult preMarshall(Element objectElement, Node postValidationTree,
                    OperationResult objectResult) {
                currentOid = objectElement.getAttribute("oid");
                return EventResult.cont();
            }

            @Override
            public EventResult postMarshall(
                    T object, Element objectElement, OperationResult objectResult) {
                try {
                    if (operation.isFinished()) {
                        return EventResult.stop("Operation already finished");
                    }

                    if (filter != null) {
                        boolean match = ObjectQuery.match(object, filter, matchingRuleRegistry);

                        if (!match) {
                            operation.incrementSkipped();

                            return EventResult.skipObject("Object doesn't match filter");
                        }
                    }

                    if (!matchSelectedType(object.getClass())) {
                        operation.incrementSkipped();

                        return EventResult.skipObject("Type doesn't match");
                    }

                    queue.put(object);
                } catch (Exception ex) {
                    throw new NinjaException(getErrorMessage() + ", reason: " + ex.getMessage(), ex);
                }
                currentOid = null;
                return stopAfterFound ? EventResult.skipObject() : EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult, Exception cause) {
                // This should not
                // Should we log error?
                operation.incrementError();
                String message = getErrorMessage();
                if (context.isVerbose()) {
                    context.getLog().error(message, cause);
                } else {
                    context.getLog().error(message + ", reason: {}", cause.getMessage());
                }
                if (!continueOnInputError) {
                    // We need to throw runtime exception in order to stop validator, otherwise validator will continue
                    // fill queue and this may result in deadlock
                    operation.finish();
                    throw new NinjaException(message + ", reason: " + cause.getMessage(), cause);
                }
            }
        };

        // FIXME: MID-5151: If validateSchema is false we are not validating unknown attributes on import
        LegacyValidator<?> validator = new LegacyValidator<>(prismContext, handler);
        validator.setValidateSchema(false);
        validator.setCompatMode(compatMode);
        validator.setConvertMissingType(isConvertMissingType());
        OperationResult result = operation.getResult();

        Charset charset = context.getCharset();
        Reader reader = new InputStreamReader(input, charset);
        validator.validate(new ReaderInputStream(reader, charset), result, result.getOperation());
    }

    private boolean matchSelectedType(Class<?> clazz) {
        if (options.getType().isEmpty()) {
            return true;
        }

        for (ObjectTypes type : options.getType()) {
            if (type.getClassDefinition().equals(clazz)) {
                return true;
            }
        }

        return false;
    }

    private String getErrorMessage() {
        if (currentOid != null && !currentOid.isBlank()) {
            return "Couldn't import object with oid '" + currentOid + "'";
        } else {
            return "Couldn't import object";
        }
    }

    public boolean isConvertMissingType() {
        return convertMissingType;
    }

    public void setConvertMissingType(boolean convertMissingType) {
        this.convertMissingType = convertMissingType;
    }

    public void setCompatMode(boolean mode) {
        this.compatMode = mode;
    }
}
