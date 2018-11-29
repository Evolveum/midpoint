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
package com.evolveum.midpoint.ninja.action;

import com.evolveum.midpoint.ninja.action.worker.SearchProducerWorker;
import com.evolveum.midpoint.ninja.action.worker.ProgressReporterWorker;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.opts.ExportOptions;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Abstract action for all search-based operations, such as export and verify.
 * 
 * @author Viliam Repan (lazyman)
 */
public abstract class AbstractRepositorySearchAction<OP extends ExportOptions> extends RepositoryAction<OP> {
	
	private static final String DOT_CLASS = AbstractRepositorySearchAction.class.getName() + ".";

    private static final String OPERATION_LIST_RESOURCES = DOT_CLASS + "listResources";

    private static final int QUEUE_CAPACITY_PER_THREAD = 100;
    private static final long CONSUMERS_WAIT_FOR_START = 2000L;
    
    protected abstract String getOperationShortName();
    
    protected abstract Runnable createConsumer(BlockingQueue<PrismObject> queue, OperationStatus operation);

    protected String getOperationName() {
    	return this.getClass().getName() + "." + getOperationShortName();
    }
    
    @Override
    public void execute() throws Exception {
        OperationResult result = new OperationResult(getOperationName());
        OperationStatus operation = new OperationStatus(context, result);

        // "+ 2" will be used for consumer and progress reporter
        ExecutorService executor = Executors.newFixedThreadPool(options.getMultiThread() + 2);

        BlockingQueue<PrismObject> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD * options.getMultiThread());

        List<SearchProducerWorker> producers = createProducers(queue, operation);

        log.info("Starting " + getOperationShortName());
        operation.start();

        // execute as many producers as there are threads for them
        for (int i = 0; i < producers.size() && i < options.getMultiThread(); i++) {
            executor.execute(producers.get(i));
        }

        Thread.sleep(CONSUMERS_WAIT_FOR_START);

        executor.execute(new ProgressReporterWorker(context, options, queue, operation));

        Runnable consumer = createConsumer(queue, operation);
        executor.execute(consumer);

        // execute rest of the producers
        for (int i = options.getMultiThread(); i < producers.size(); i++) {
            executor.execute(producers.get(i));
        }

        executor.shutdown();
        executor.awaitTermination(NinjaUtils.WAIT_FOR_EXECUTOR_FINISH, TimeUnit.DAYS);

        handleResultOnFinish(operation, "Finished " + getOperationShortName());
    }

    @Override
    public LogTarget getInfoLogTarget() {
        if (options.getOutput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private List<SearchProducerWorker> createProducers(BlockingQueue<PrismObject> queue, OperationStatus operation)
            throws SchemaException, IOException {

        List<SearchProducerWorker> producers = new ArrayList<>();

        if (options.getOid() != null) {
            Set<ObjectTypes> types = options.getType();

            ObjectTypes type = types.isEmpty() ? ObjectTypes.OBJECT : types.iterator().next();

            InOidFilter filter = InOidFilter.createInOid(options.getOid());
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);

            producers.add(new SearchProducerWorker(context, options, queue, operation, producers, type, query));
            return producers;
        }

        List<ObjectTypes> types = NinjaUtils.getTypes(options.getType());
        for (ObjectTypes type : types) {
            ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), context, type.getClassDefinition());
            ObjectQuery query = ObjectQuery.createObjectQuery(filter);
            if (ObjectTypes.SHADOW.equals(type)) {
                List<SearchProducerWorker> shadowProducers = createProducersForShadows(queue, operation, producers, filter);
                producers.addAll(shadowProducers);
                continue;
            }

            producers.add(new SearchProducerWorker(context, options, queue, operation, producers, type, query));
        }

        return producers;
    }

    /**
     * The idea is to split shadow per resource. We will get more producer workers in this way, therefore we can
     * run in more threads. No extra special processing is done for shadows. Just to split them to workers for
     * performance reasons. 
     */
    private List<SearchProducerWorker> createProducersForShadows(BlockingQueue<PrismObject> queue,
                                                                 OperationStatus operation,
                                                                 List<SearchProducerWorker> producers,
                                                                 ObjectFilter filter) {

        List<SearchProducerWorker> shadowProducers = new ArrayList<>();

        try {
            RepositoryService repository = context.getRepository();

            Collection<SelectorOptions<GetOperationOptions>> opts =
                    SelectorOptions.createCollection(GetOperationOptions.createRaw());

            OperationResult result = new OperationResult(OPERATION_LIST_RESOURCES);

            SearchResultList<PrismObject<ResourceType>> resultList = repository.searchObjects(ResourceType.class,
                    ObjectQuery.createObjectQuery((ObjectFilter) null), opts, result);

            List<PrismObject<ResourceType>> list = resultList.getList();
            if (list == null || list.isEmpty()) {
                shadowProducers.add(createProducer(queue, operation, producers, ObjectTypes.SHADOW, filter));
                return shadowProducers;
            }

            List<RefFilter> existingResourceRefs = new ArrayList<>();
            for (PrismObject<ResourceType> obj : list) {
                RefFilter resourceRefFilter = createResourceRefFilter(obj.getOid());
                existingResourceRefs.add(resourceRefFilter);

                ObjectFilter fullFilter = resourceRefFilter;
                if (filter != null) {
                    fullFilter = AndFilter.createAnd(fullFilter, filter);
                }

                shadowProducers.add(createProducer(queue, operation, producers, ObjectTypes.SHADOW, fullFilter));
            }

            // all other shadows (no resourceRef or non existing resourceRef)
            List<ObjectFilter> notFilters = new ArrayList<>();
            existingResourceRefs.forEach(f -> notFilters.add(NotFilter.createNot(f)));

            ObjectFilter fullFilter = OrFilter.createOr(
                    AndFilter.createAnd(notFilters),
                    createResourceRefFilter(null)
            );
            if (filter != null) {
                fullFilter = AndFilter.createAnd(fullFilter, filter);
            }

            shadowProducers.add(createProducer(queue, operation, producers, ObjectTypes.SHADOW, fullFilter));
        } catch (Exception ex) {
            shadowProducers.clear();

            shadowProducers.add(createProducer(queue, operation, producers, ObjectTypes.SHADOW, filter));
        }

        return shadowProducers;
    }

    private RefFilter createResourceRefFilter(String oid) throws SchemaException {
        List<PrismReferenceValue> values = new ArrayList<>();
        if (oid != null) {
            values.add(new PrismReferenceValueImpl(oid, ResourceType.COMPLEX_TYPE));
        }

        PrismContext prismContext = context.getPrismContext();
        SchemaRegistry registry = prismContext.getSchemaRegistry();
        PrismReferenceDefinition def = registry.findItemDefinitionByFullPath(ShadowType.class,
                PrismReferenceDefinition.class, ShadowType.F_RESOURCE_REF);

        return RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, def, values);
    }

    private SearchProducerWorker createProducer(BlockingQueue<PrismObject> queue, OperationStatus operation,
                                                List<SearchProducerWorker> producers, ObjectTypes type, ObjectFilter filter) {
        ObjectQuery query = ObjectQuery.createObjectQuery(filter);
        return new SearchProducerWorker(context, options, queue, operation, producers, type, query);
    }
}
