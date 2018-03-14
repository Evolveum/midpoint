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
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.Reader;
import java.util.concurrent.BlockingQueue;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportProducerWorker implements Runnable {

    private static final String DOT_CLASS = ImportProducerWorker.class.getName() + ".";

    private static final String OPERATION_IMPORT = DOT_CLASS + "import";

    private NinjaContext context;
    private ImportOptions options;

    private BlockingQueue<PrismObject> queue;

    private ObjectFilter filter;
    private boolean stopAfterFound;
    private Reader reader;

    private OperationStatus operation;

    private OperationResult result;

    public ImportProducerWorker(NinjaContext context, ImportOptions options, BlockingQueue queue,
                                ObjectFilter filter, boolean stopAfterFound, Reader reader) {
        this.context = context;
        this.options = options;
        this.queue = queue;

        this.filter = filter;
        this.stopAfterFound = stopAfterFound;
        this.reader = reader;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setOperation(OperationStatus operation) {
        this.operation = operation;
    }

    @Override
    public void run() {
        ApplicationContext appContext = context.getApplicationContext();
        PrismContext prismContext = appContext.getBean(PrismContext.class);
        MatchingRuleRegistry matchingRuleRegistry = appContext.getBean(MatchingRuleRegistry.class);

        result = new OperationResult(OPERATION_IMPORT);

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

                return stopAfterFound ? EventResult.stop() : EventResult.cont();
            }

            @Override
            public void handleGlobalError(OperationResult currentResult) {
                operation.finish();
            }
        };

        context.getLog().info("Starting import");
        operation.start();

        Validator validator = new Validator(prismContext, handler);
        validator.validate(new ReaderInputStream(reader, context.getCharset()), result, OPERATION_IMPORT);

        operation.producerFinish();
    }
}
