/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.slow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Checks the {@link RepositoryService#allocateContainerIdentifiers(Class, String, int, OperationResult)} for correctness
 * under concurrent access:
 *
 * . competing multiple threads when each of them does the pre-allocation;
 * . competing multiple threads when some do pre-allocation and some simply add values without providing any IDs.
 */
public class IdAllocationConcurrencyTest extends SqaleRepoBaseTest {

    private static final int SANITY_CHECK_DURATION = 500;
    private static final int DURATION = 10_000;
    private static final int WAIT_FOR_STOP = 10_000;
    private static final int VALUES_PER_STEP = 10;

    private final AtomicInteger uniqueIdCounter = new AtomicInteger(0);

    @Test
    public void test100OneThreadAllocating() throws Exception {
        doTest(SANITY_CHECK_DURATION,
                new WorkerThread[] {
                        new WorkerThread(1, true)
                });
    }

    @Test
    public void test110OneThreadNotAllocating() throws Exception {
        doTest(SANITY_CHECK_DURATION,
                new WorkerThread[] {
                        new WorkerThread(1, false)
                });
    }

    @Test
    public void test120FourThreadsAllocating() throws Exception {
        doComplex(4, 0);
    }

    @Test
    public void test120FourThreadsNotAllocating() throws Exception {
        doComplex(0, 4);
    }

    @Test
    public void test130FourThreadsBoth() throws Exception {
        doComplex(4, 4);
    }

    private void doComplex(int allocating, int notAllocating) throws Exception {
        WorkerThread[] mts = new WorkerThread[allocating + notAllocating];
        for (int i = 0; i < allocating; i++) {
            mts[i] = new WorkerThread(i + 1, true);
        }
        for (int i = 0; i < notAllocating; i++) {
            mts[allocating + i] = new WorkerThread(allocating + i + 1, false);
        }
        doTest(DURATION, mts);
    }

    private void doTest(long duration, WorkerThread[] workerThreads) throws Exception {

        OperationResult result = createOperationResult();
        String oid = repositoryService.addObject(
                new UserType()
                        .name(getTestNameShort())
                        .asPrismObject(),
                null, result);
        display("Object added: " + oid);

        display("Starting modifier threads");
        for (WorkerThread t : workerThreads) {
            t.setOid(oid);
            t.start();
        }

        display("Waiting " + duration + " ms");
        Thread.sleep(duration);

        for (WorkerThread t : workerThreads) {
            t.stop = true;
        }

        long endTime = System.currentTimeMillis() + WAIT_FOR_STOP;
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
            if (Arrays.stream(workerThreads).noneMatch(Thread::isAlive)) {
                break;
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

        int iterations = Arrays.stream(workerThreads)
                .mapToInt(t -> t.counter)
                .sum();
        int expectedValues = iterations * VALUES_PER_STEP;
        displayValue("iterations", iterations);
        displayValue("expectedValues", expectedValues);

        var userAfter = repositoryService.getObject(UserType.class, oid, null, result).asObjectable();
        var assignments = userAfter.getAssignment().size();
        assertThat(assignments).as("assignments created").isEqualTo(expectedValues);
    }

    private class WorkerThread extends Thread {
        private final int id;
        private final boolean allocating;

        private String oid;

        private volatile Throwable threadResult;
        private volatile int counter = 0;
        private volatile boolean stop = false;

        WorkerThread(int id, boolean allocating) {
            this.id = id;
            this.allocating = allocating;
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

        void runOnce() throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
            OperationResult result = new OperationResult("run");
            List<AssignmentType> assignments = new ArrayList<>();
            if (allocating) {
                Collection<Long> identifiers = repositoryService.allocateContainerIdentifiers(
                        UserType.class, oid, VALUES_PER_STEP, result);
                MiscUtil.stateCheck(
                        identifiers.size() == VALUES_PER_STEP,
                        "Wrong number of identifiers: %s", identifiers);
                for (Long identifier : identifiers) {
                    assignments.add(new AssignmentType()
                            .id(identifier)
                            .description("business id " + uniqueId()));
                }
            } else {
                for (int i = 0; i < VALUES_PER_STEP; i++) {
                    assignments.add(new AssignmentType()
                            .description("business id " + uniqueId()));
                }
            }
            repositoryService.modifyObject(
                    UserType.class,
                    oid,
                    prismContext.deltaFor(UserType.class)
                            .item(UserType.F_ASSIGNMENT)
                            .addRealValues(assignments)
                            .asItemDeltas(),
                    result);
        }

        public void setOid(String oid) {
            this.oid = oid;
        }
    }

    private int uniqueId() {
        return uniqueIdCounter.getAndIncrement();
    }
}
