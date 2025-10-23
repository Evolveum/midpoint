/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import com.evolveum.midpoint.ninja.action.worker.ProgressReporterWorker;
import com.evolveum.midpoint.ninja.action.worker.SearchProducerWorker;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Abstract action for all search-based operations, such as export and verify.
 *
 * @param <O> options class
 */
public abstract class AbstractRepositorySearchAction<O extends ExportOptions, R> extends RepositoryAction<O, R> {

    private static final String DOT_CLASS = AbstractRepositorySearchAction.class.getName() + ".";

    private static final String OPERATION_LIST_RESOURCES = DOT_CLASS + "listResources";

    private static final int QUEUE_CAPACITY_PER_THREAD = 100;
    private static final long CONSUMERS_WAIT_FOR_START = 2000L;

    public AbstractRepositorySearchAction() {
    }

    public AbstractRepositorySearchAction(boolean partial) {
        super(partial);
    }

    protected abstract Callable<R> createConsumer(BlockingQueue<ObjectType> queue, OperationStatus operation);

    @Override
    public R execute() throws Exception {
        OperationResult result = new OperationResult(getClass().getName());
        OperationStatus operation = new OperationStatus(context, result);

        // "+ 2" will be used for consumer and progress reporter
        ExecutorService executor = Executors.newFixedThreadPool(options.getMultiThread() + 2);

        BlockingQueue<ObjectType> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD * options.getMultiThread());

        context.getResourceSchemaRegistry(); // Make sure we have resource schema registry initialiezd

        List<SearchProducerWorker> producers = createProducers(queue, operation);

        operation.start();


        // execute as many producers as there are threads for them
        for (int i = 0; i < producers.size() && i < options.getMultiThread(); i++) {
            executor.execute(producers.get(i));
        }

        Thread.sleep(CONSUMERS_WAIT_FOR_START);

        executor.execute(new ProgressReporterWorker<>(context, options, queue, operation));

        Callable<R> consumer = createConsumer(queue, operation);
        Future<R> consumerFuture = executor.submit(consumer);

        // execute rest of the producers
        for (int i = options.getMultiThread(); i < producers.size(); i++) {
            executor.execute(producers.get(i));
        }

        executor.shutdown();
        boolean awaitResult = executor.awaitTermination(NinjaUtils.WAIT_FOR_EXECUTOR_FINISH, TimeUnit.DAYS);
        if (!awaitResult) {
            log.error("Executor did not finish before timeout");
        }

        R consumerResult = consumerFuture.get();

        handleResultOnFinish(consumerResult, operation, "Finished " + getOperationName());

        return consumerResult;
    }

    @Override
    public LogTarget getLogTarget() {
        if (options.getOutput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private List<SearchProducerWorker> createProducers(BlockingQueue<ObjectType> queue, OperationStatus operation)
            throws SchemaException, IOException {

        QueryFactory queryFactory = context.getPrismContext().queryFactory();
        List<SearchProducerWorker> producers = new ArrayList<>();

        if (options.getOid() != null) {
            Set<ObjectTypes> types = options.getType();

            ObjectTypes type = types.isEmpty() ? ObjectTypes.OBJECT : types.iterator().next();

            InOidFilter filter = queryFactory.createInOid(options.getOid());
            ObjectQuery query = queryFactory.createQuery(filter);

            producers.add(new SearchProducerWorker(context, options, queue, operation, producers, type, query));
            return producers;
        }

        List<ObjectTypes> types = NinjaUtils.getTypes(options.getType(), supportedObjectTypes());
        for (ObjectTypes type : types) {
            if (!context.getRepository().supports(type.getClassDefinition())) {
                log.warn("Repository doesn't support operation '{}' for objects of type '{}'",
                        getOperationName(),  type.getClassDefinition().getSimpleName());
                continue;
            }

            ObjectFilter filter = NinjaUtils.createObjectFilter(options.getFilter(), context, type.getClassDefinition());
            ObjectQuery query = queryFactory.createQuery(filter);
            if (ObjectTypes.SHADOW.equals(type)) {
                List<SearchProducerWorker> shadowProducers =
                        createProducersForShadows(context, queue, operation, producers, filter);
                producers.addAll(shadowProducers);
                continue;
            }

            producers.add(new SearchProducerWorker(context, options, queue, operation, producers, type, query));
        }

        return producers;
    }

    /**
     * Returns list of object types supported by this repository action
     *
     * @return
     */
    protected Iterable<ObjectTypes> supportedObjectTypes() {
        return List.of(ObjectTypes.values());
    }

    /**
     * The idea is to split shadow per resource. We will get more producer workers in this way, therefore we can
     * run in more threads. No extra special processing is done for shadows. Just to split them to workers for
     * performance reasons.
     */
    private List<SearchProducerWorker> createProducersForShadows(
            NinjaContext context, BlockingQueue<ObjectType> queue,
            OperationStatus operation, List<SearchProducerWorker> producers, ObjectFilter filter) {

        QueryFactory queryFactory = context.getPrismContext().queryFactory();
        List<SearchProducerWorker> shadowProducers = new ArrayList<>();

        try {
            RepositoryService repository = context.getRepository();

            Collection<SelectorOptions<GetOperationOptions>> opts =
                    SelectorOptions.createCollection(GetOperationOptions.createRaw());

            OperationResult result = new OperationResult(OPERATION_LIST_RESOURCES);

            SearchResultList<PrismObject<ResourceType>> resultList = repository.searchObjects(ResourceType.class,
                    queryFactory.createQuery((ObjectFilter) null), opts, result);

            List<PrismObject<ResourceType>> list = resultList.getList();
            if (list == null || list.isEmpty()) {
                shadowProducers.add(createShadowProducer(queue, operation, producers, filter));
                return shadowProducers;
            }

            List<RefFilter> existingResourceRefs = new ArrayList<>();
            for (PrismObject<ResourceType> obj : list) {
                RefFilter resourceRefFilter = createResourceRefFilter(obj.getOid());
                existingResourceRefs.add(resourceRefFilter);

                ObjectFilter fullFilter = resourceRefFilter;
                if (filter != null) {
                    fullFilter = queryFactory.createAnd(fullFilter, filter);
                }

                shadowProducers.add(createShadowProducer(queue, operation, producers, fullFilter));
            }

            // all other shadows (no resourceRef or non-existing resourceRef)
            List<ObjectFilter> notFilters = new ArrayList<>();
            existingResourceRefs.forEach(f -> notFilters.add(queryFactory.createNot(f)));

            ObjectFilter fullFilter = queryFactory.createOr(
                    queryFactory.createAnd(notFilters),
                    createResourceRefFilter(null)
            );
            if (filter != null) {
                fullFilter = queryFactory.createAnd(fullFilter, filter);
            }

            shadowProducers.add(createShadowProducer(queue, operation, producers, fullFilter));
        } catch (Exception ex) {
            shadowProducers.clear();

            shadowProducers.add(createShadowProducer(queue, operation, producers, filter));
        }

        return shadowProducers;
    }

    private RefFilter createResourceRefFilter(String oid) throws SchemaException {
        PrismContext prismContext = context.getPrismContext();
        List<PrismReferenceValue> values = new ArrayList<>();
        if (oid != null) {
            values.add(prismContext.itemFactory().createReferenceValue(oid, ResourceType.COMPLEX_TYPE));
        }

        SchemaRegistry registry = prismContext.getSchemaRegistry();
        PrismReferenceDefinition def = registry.findItemDefinitionByFullPath(ShadowType.class,
                PrismReferenceDefinition.class, ShadowType.F_RESOURCE_REF);

        return prismContext.queryFactory().createReferenceEqual(ShadowType.F_RESOURCE_REF, def, values);
    }

    private SearchProducerWorker createShadowProducer(BlockingQueue<ObjectType> queue,
            OperationStatus operation, List<SearchProducerWorker> producers, ObjectFilter filter) {
        ObjectQuery query = context.getPrismContext().queryFactory().createQuery(filter);
        query = context.getResourceSchemaRegistry().tryToNormalizeQuery(query);
        return new SearchProducerWorker(context, options, queue, operation, producers, ObjectTypes.SHADOW, query);
    }
}
