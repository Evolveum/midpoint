/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.action.RepositoryAction;
import com.evolveum.midpoint.ninja.action.worker.ProgressReporterWorker;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.ninja.util.OperationStatus;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ExportMiningRepositoryAction extends RepositoryAction<ExportMiningOptions, Void> {

    private static final int QUEUE_CAPACITY_PER_THREAD = 100;
    private static final long CONSUMERS_WAIT_FOR_START = 2000L;

    public static final String OPERATION_SHORT_NAME = "exportMining";
    public static final String OPERATION_NAME = ExportMiningRepositoryAction.class.getName() + "." + OPERATION_SHORT_NAME;

    @Override
    public String getOperationName() {
        return "export mining data";
    }

    protected Runnable createConsumer(
            BlockingQueue<FocusType> queue, OperationStatus operation) {
        return new ExportMiningConsumerWorker(context, options, queue, operation);
    }

    @Override
    public Void execute() throws Exception {
        OperationResult result = new OperationResult(OPERATION_NAME);
        OperationStatus operation = new OperationStatus(context, result);

        ExecutorService executor = Executors.newFixedThreadPool(options.getMultiThread() + 2);

        BlockingQueue<FocusType> queue =
                new LinkedBlockingQueue<>(QUEUE_CAPACITY_PER_THREAD * options.getMultiThread());

        List<ExportMiningProducerWorker> producers = createProducers(queue, operation);

        log.info("Starting " + OPERATION_SHORT_NAME);
        operation.start();

        for (int i = 0; i < producers.size() && i < options.getMultiThread(); i++) {
            executor.execute(producers.get(i));
        }

        Thread.sleep(CONSUMERS_WAIT_FOR_START);

        executor.execute(new ProgressReporterWorker<>(context, options, queue, operation));

        // NOTE: the consumer is designed to be executed in a single thread
        Runnable consumer = createConsumer(queue, operation);
        executor.execute(consumer);

        for (int i = options.getMultiThread(); i < producers.size(); i++) {
            executor.execute(producers.get(i));
        }

        executor.shutdown();
        boolean awaitResult = executor.awaitTermination(NinjaUtils.WAIT_FOR_EXECUTOR_FINISH, TimeUnit.DAYS);
        if (!awaitResult) {
            log.error("Executor did not finish before timeout");
        }

        handleResultOnFinish(null, operation, "Finished " + OPERATION_SHORT_NAME);

        return null;
    }

    @Override
    public LogTarget getLogTarget() {
        if (options.getOutput() != null) {
            return LogTarget.SYSTEM_OUT;
        }

        return LogTarget.SYSTEM_ERR;
    }

    private @NotNull List<ExportMiningProducerWorker> createProducers(
            BlockingQueue<FocusType> queue, OperationStatus operation)
            throws SchemaException, IOException {

        QueryFactory queryFactory = context.getPrismContext().queryFactory();
        List<ExportMiningProducerWorker> producers = new ArrayList<>();

        ObjectFilter filter = NinjaUtils.createObjectFilter(options.getRoleFilter(), context, RoleType.class);
        ObjectQuery query = queryFactory.createQuery(filter);
        producers.add(new ExportMiningProducerWorker(context, options, queue, operation, producers, query, RoleType.class));

        filter = NinjaUtils.createObjectFilter(options.getUserFilter(), context, UserType.class);
        query = queryFactory.createQuery(filter);
        producers.add(new ExportMiningProducerWorker(context, options, queue, operation, producers, query, UserType.class));

        if (options.isIncludeOrg()) {
            filter = NinjaUtils.createObjectFilter(options.getOrgFilter(), context, OrgType.class);
            query = queryFactory.createQuery(filter);
            producers.add(new ExportMiningProducerWorker(context, options, queue, operation, producers, query, OrgType.class));
        }
        return producers;
    }
}
