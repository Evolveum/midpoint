/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

abstract class ConcurrencyRaceHelpers extends ConcurrencyRaceValidation {

    protected void runStress(StressConfig config) throws Exception {
        Queue<Failure> failures = new ConcurrentLinkedQueue<>();
        AtomicBoolean stop = new AtomicBoolean(false);
        long deadline = config.durationSeconds() > 0
                ? System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(config.durationSeconds())
                : Long.MAX_VALUE;

        int round = 0;
        while (!stop.get()
                && (config.rounds() > 0 ? round < config.rounds() : System.currentTimeMillis() < deadline)) {
            List<WorkItem> roundItems = collectRoundWorkItems(config, round);
            runRound(config, round, roundItems, failures, stop);
            round++;
        }

        if (!failures.isEmpty()) {
            fail(renderFailures(config, failures));
        }
    }

    protected void runRound(
            StressConfig config,
            int round,
            List<WorkItem> roundItems,
            Queue<Failure> failures,
            AtomicBoolean stop) throws Exception {

        if (config.concurrencyMode() == ConcurrencyMode.DISTINCT_USERS) {
            assertNoDuplicateUserOids(roundItems, config, round);
        }

        List<WorkItem> shuffledItems = new ArrayList<>(roundItems);
        java.util.Collections.shuffle(shuffledItems, new java.util.Random(17_000L + round));
        AtomicInteger cursor = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(config.threads());
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < config.threads(); i++) {
            int threadIndex = i;
            futures.add(executor.submit(
                    () -> runWorker(config, round, threadIndex, shuffledItems, cursor, failures, stop)));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        if (config.concurrencyMode() == ConcurrencyMode.DISTINCT_USERS && !stop.get()) {
            validateAfterRound(config, round, roundItems, failures, stop);
        }
    }

    protected void runWorker(
            StressConfig config,
            int round,
            int threadIndex,
            List<WorkItem> workItems,
            AtomicInteger cursor,
            Queue<Failure> failures,
            AtomicBoolean stop) {

        try {
            Task localTask = createTask(config.name() + "-r" + round + "-t" + threadIndex);
            login(userAdministrator.copy());

            while (!stop.get()) {
                int index = cursor.getAndIncrement();
                if (index >= workItems.size()) {
                    return;
                }
                WorkItem workItem = workItems.get(index);
                try {
                    executeForUser(workItem.userOid(), config.serializePerUser(), () -> {
                        recomputeUser(workItem.userOid(), executeOptions().reconcile(), localTask, new OperationResult("recompute"));
                        if (config.concurrencyMode() == ConcurrencyMode.SAME_USER_CONCURRENT) {
                            validateUser(config, workItem, round, threadIndex, ValidationPhase.IMMEDIATE);
                        }
                    });
                } catch (Throwable t) {
                    failures.add(Failure.of(config, workItem, round, threadIndex, ValidationPhase.IMMEDIATE, t));
                    stop.set(true);
                    return;
                }
            }
        } catch (Throwable t) {
            failures.add(Failure.of(config, null, round, threadIndex, ValidationPhase.IMMEDIATE, t));
            stop.set(true);
        }
    }

    protected void validateAfterRound(
            StressConfig config,
            int round,
            List<WorkItem> roundItems,
            Queue<Failure> failures,
            AtomicBoolean stop) {
        for (WorkItem workItem : roundItems) {
            if (stop.get()) {
                return;
            }
            try {
                validateUser(config, workItem, round, -1, ValidationPhase.POST_ROUND);
            } catch (Throwable t) {
                failures.add(Failure.of(config, workItem, round, -1, ValidationPhase.POST_ROUND, t));
                stop.set(true);
                return;
            }
        }
    }

    protected void executeForUser(String userOid, boolean serializePerUser, ThrowingRunnable runnable) throws Exception {
        if (!serializePerUser) {
            runnable.run();
            return;
        }
        Object lock = userLocks.computeIfAbsent(userOid, ignored -> new Object());
        synchronized (lock) {
            runnable.run();
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
