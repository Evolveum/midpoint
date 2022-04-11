/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.slow;

import static org.testng.AssertJUnit.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequenceType;

public class SequenceConcurrencyTest extends SqaleRepoBaseTest {

    private static final int STOP_TIMEOUT = 10_000;

    @Test
    public void test101OneThread() throws Exception {
        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1)
        };

        concurrencyUniversal(10_000L, mts, false);
    }

    @Test
    public void test102TwoThreads() throws Exception {
        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1),
                new WorkerThread(2)
        };

        concurrencyUniversal(10_000L, mts, false);
    }

    @Test
    public void test110TenThreads() throws Exception {
        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1),
                new WorkerThread(2),
                new WorkerThread(3),
                new WorkerThread(4),
                new WorkerThread(5),
                new WorkerThread(6),
                new WorkerThread(7),
                new WorkerThread(8),
                new WorkerThread(9),
                new WorkerThread(10)
        };

        concurrencyUniversal(10_000L, mts, false);
    }

    @Test
    public void test201OneThreadReturning() throws Exception {
        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1, 5)
        };

        concurrencyUniversal(10_000L, mts, true);
    }

    @Test
    public void test202TwoThreadsReturning() throws Exception {
        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1, 5),
                new WorkerThread(2, 5)
        };

        concurrencyUniversal(10_000L, mts, true);
    }

    @Test
    public void test210TenThreadsReturning() throws Exception {
        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1, 5),
                new WorkerThread(2, 5),
                new WorkerThread(3, 2),
                new WorkerThread(4, 2),
                new WorkerThread(5, 2),
                new WorkerThread(6, 2),
                new WorkerThread(7, 2),
                new WorkerThread(8, 2),
                new WorkerThread(9, 0),
                new WorkerThread(10, 0)
        };

        concurrencyUniversal(10_000L, mts, true);
    }

    private void concurrencyUniversal(
            long duration, WorkerThread[] workerThreads, boolean alwaysOrder) throws Exception {

        try (JdbcSession jdbcSession = startTransaction()) {
            System.out.println(">>>>" + jdbcSession.connection().getTransactionIsolation());
        }

        OperationResult result = createOperationResult();
        String oid = repositoryService.addObject(
                new SequenceType()
                        .name(getTestNameShort())
                        .counter(0L)
                        .maxUnusedValues(10)
                        .asPrismObject(),
                null, result);
        display("*** Object added: " + oid + " ***");

        display("*** Starting modifier threads ***");
        for (WorkerThread t : workerThreads) {
            t.setOid(oid);
            t.start();
        }

        display("*** Waiting " + duration + " ms ***");
        Thread.sleep(duration);

        for (WorkerThread t : workerThreads) {
            t.stop = true;
        }

        long endTime = System.currentTimeMillis() + STOP_TIMEOUT;
        for (; ; ) {
            long remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }
            for (WorkerThread t : workerThreads) {
                t.join(remaining);
                remaining = endTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
            }
        }

        for (WorkerThread t : workerThreads) {
            display("Worker thread " + t.id + " finished after " + t.counter
                    + " iterations with result: " + (t.threadResult != null ? t.threadResult : "OK"));
        }

        for (WorkerThread t : workerThreads) {
            if (t.threadResult != null) {
                throw new AssertionError(
                        "Worker thread " + t.id + " finished with an exception: " + t.threadResult,
                        t.threadResult);
            }
        }

        List<Long> allValues = new ArrayList<>();
        for (WorkerThread t : workerThreads) {
            allValues.addAll(t.values);
        }
        if (alwaysOrder || workerThreads.length > 1) {
            Collections.sort(allValues);
        }
        logger.trace("Checking a list of {} values", allValues.size());
        for (int i = 0; i < allValues.size(); i++) {
            if (allValues.get(i) != i) {
                logger.error("Incorrect value at position {}: {}", i, allValues.get(i));
                for (WorkerThread t : workerThreads) {
                    display("Thread " + t.id + ": " + t.values);
                }
                fail("Incorrect value at position " + i + ": " + allValues.get(i));
            }
        }
    }

    private class WorkerThread extends Thread {
        private final int id;
        private final int returnEach;
        private final List<Long> values = new ArrayList<>();

        private String oid; // sequence to use
        private int countToReturn;

        private volatile Throwable threadResult;
        private volatile int counter = 0;
        private volatile boolean stop = false;

        WorkerThread(int id, int returnEach) {
            this.id = id;
            this.returnEach = returnEach;
            this.countToReturn = returnEach;
        }

        WorkerThread(int id) {
            this(id, 0);
        }

        @Override
        public void run() {
            try {
                while (!stop) {
                    runOnce();
                    //noinspection NonAtomicOperationOnVolatileField
                    counter++;
                }
            } catch (Throwable t) {
                LoggingUtils.logException(logger, "Unexpected exception: " + t, t);
                threadResult = t;
            }
        }

        public void runOnce() throws SchemaException, ObjectNotFoundException {
            OperationResult result = new OperationResult("run");
            long value = repositoryService.advanceSequence(oid, result);
            logger.debug("Advance sequence returned {}", value);
            values.add(value);
            if (returnEach > 0) {
                if (countToReturn > 0) {
                    countToReturn--;
                } else {
                    countToReturn = returnEach;
                    int i = (int) (Math.random() * values.size());
                    long v = values.remove(i);
                    repositoryService.returnUnusedValuesToSequence(
                            oid, Collections.singletonList(v), result);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // ignored
                    }
                    value = repositoryService.advanceSequence(oid, result);
                    logger.debug("Advance sequence returned {} (after return)", value);
                    values.add(value);
                }
            }
        }

        public void setOid(String oid) {
            this.oid = oid;
        }
    }
}
