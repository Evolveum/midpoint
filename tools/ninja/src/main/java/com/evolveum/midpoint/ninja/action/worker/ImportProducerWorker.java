/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.ninja.action.worker;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.impl.NinjaException;
import com.evolveum.midpoint.ninja.opts.ImportOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.*;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportProducerWorker extends BaseWorker<ImportOptions, PrismObject> {

    private ObjectFilter filter;
    private boolean stopAfterFound;

    public ImportProducerWorker(NinjaContext context, ImportOptions options, BlockingQueue queue, OperationStatus operation,
                                ObjectFilter filter, boolean stopAfterFound) {
        super(context, options, queue, operation);

        this.filter = filter;
        this.stopAfterFound = stopAfterFound;
    }

    @Override
    public void run() {
        Log log = context.getLog();

        try (InputStream input = openInputStream()) {
            log.info("Starting import");
            operation.start();

            if (!options.isZip()) {
                processStream(input);
            } else {
                ZipInputStream zis = new ZipInputStream(input);
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    if (!StringUtils.endsWith(entry.getName().toLowerCase(), ".xml")) {
                        continue;
                    }

                    log.info("Processing file {}", entry.getName());
                    processStream(zis);
                }
            }
        } catch (IOException ex) {
            log.error("Unexpected error occurred, reason: {}", ex, ex.getMessage());
        } catch (NinjaException ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            markDone();
            if (isWorkersDone()) {
                operation.producerFinish();
            }
        }
    }

    private InputStream openInputStream() throws IOException {
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

        return is;
    }

    private void processStream(InputStream input) throws IOException {
        ApplicationContext appContext = context.getApplicationContext();
        PrismContext prismContext = appContext.getBean(PrismContext.class);
        MatchingRuleRegistry matchingRuleRegistry = appContext.getBean(MatchingRuleRegistry.class);

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
                            operation.incrementSkipped();

                            return EventResult.skipObject("Object doesn't match filter");
                        }
                    }

                    ObjectTypes type = options.getType();
                    if (type != null && !type.getClassDefinition().equals(object.getCompileTimeClass())) {
                        operation.incrementSkipped();

                        return EventResult.skipObject("Type doesn't match");
                    }

                    queue.put(object);
                } catch (Exception ex) {
                    throw new NinjaException("Couldn't import object, reason: " + ex.getMessage(), ex);
                }

                return stopAfterFound ? EventResult.skipObject() : EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
                operation.finish();
            }
        };

        Validator validator = new Validator(prismContext, handler);
        validator.setValidateSchema(false);

        OperationResult result = operation.getResult();

        Charset charset = context.getCharset();
        Reader reader = new InputStreamReader(input, charset);
        validator.validate(new ReaderInputStream(reader, charset), result, result.getOperation());
    }
}
