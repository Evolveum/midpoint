/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.ninja.impl.Log;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.ninja.util.NinjaUtils;

public class ActionExecutor<PR, CR, P extends Worker<PR>, C extends Worker<CR>, O> {

    private static final int QUEUE_CAPACITY_PER_THREAD = 100;

    private final NinjaContext context;

    private final int producerCount;

    private final int consumerCount;

    private final WorkerFactory<O, P> producerFactory;

    private final WorkerFactory<O, C> consumerFactory;

    private final Log log;

    public ActionExecutor(
            @NotNull NinjaContext context,
            int producerCount,
            int consumerCount,
            @NotNull WorkerFactory<O, P> producerFactory,
            @NotNull WorkerFactory<O, C> consumerFactory) {

        this.context = context;
        this.producerCount = producerCount;
        this.consumerCount = consumerCount;
        this.producerFactory = producerFactory;
        this.consumerFactory = consumerFactory;

        this.log = context.getLog();
    }

    public List<CR> execute() {
        int capacity = QUEUE_CAPACITY_PER_THREAD * Math.max(producerCount, consumerCount);
        BlockingQueue<O> queue = new LinkedBlockingQueue<>(capacity);

        Operation operation = new Operation();

        // +1 is for progress monitor
        int threads = producerCount + consumerCount + 1;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<P> producers = createWorkers(producerCount, producerFactory, operation, queue);
            List<C> consumers = createWorkers(consumerCount, consumerFactory, operation, queue);

            // todo add progress monitor

            List<Future<PR>> producerFutures = new ArrayList<>();
            producers.forEach(p -> producerFutures.add(executor.submit(p)));

            List<Future<CR>> consumerFutures = new ArrayList<>();
            consumers.forEach(c -> consumerFutures.add(executor.submit(c)));

            // todo handle states of producers & consumers, e.g. when one group finishes before the other, failures, etc.

            executor.shutdown();
            boolean awaitResult = executor.awaitTermination(NinjaUtils.WAIT_FOR_EXECUTOR_FINISH, TimeUnit.DAYS);
            if (!awaitResult) {
                log.error("Executor did not finish before timeout");
            }

            // todo handle finish & handle results
        } catch (Exception ex) {
            ex.printStackTrace();
            // todo handle exception
        } finally {
            executor.shutdownNow();
        }

        // todo handle result
        return null;
    }

    private <W> List<W> createWorkers(int count, WorkerFactory<O, W> factory, Operation operation, BlockingQueue<O> queue)
            throws Exception {

        List<W> workers = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            W worker = factory.create(context, operation, queue);
            workers.add(worker);
        }

        return workers;
    }
}
