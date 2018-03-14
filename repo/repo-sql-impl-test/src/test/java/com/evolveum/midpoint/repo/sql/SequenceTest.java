/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequenceType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

/**
 * @author Pavol Mederly
 */

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SequenceTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(SequenceTest.class);

    private static final String TEST_DIR = "src/test/resources/sequence/";

    private static final int STOP_TIMEOUT = 10000;

    @Test
    public void test001_OneThread() throws Exception {

        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1)
        };

        concurrencyUniversal("Test1", "sequence-unbound.xml", 10000L, mts, false);
    }

    @Test
    public void test002_TwoThreads() throws Exception {

        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1),
                new WorkerThread(2)
        };

        concurrencyUniversal("Test2", "sequence-unbound.xml", 10000L, mts, false);
    }

    @Test
    public void test003_TenThreads() throws Exception {

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

        concurrencyUniversal("Test3", "sequence-unbound.xml", 10000L, mts, false);
    }

    @Test
    public void test010_ReturningValues() throws Exception {

        OperationResult result = new OperationResult("test010_ReturningValues");
        final File file = new File(TEST_DIR + "sequence-bound-returned-wrapped.xml");
        PrismObject<SequenceType> sequence = prismContext.parseObject(file);
        String oid = repositoryService.addObject(sequence, null, result);

        assertEquals(0L, repositoryService.advanceSequence(oid, result));
        assertEquals(1L, repositoryService.advanceSequence(oid, result));
        assertEquals(2L, repositoryService.advanceSequence(oid, result));
        assertEquals(3L, repositoryService.advanceSequence(oid, result));
        assertEquals(4L, repositoryService.advanceSequence(oid, result));
        repositoryService.returnUnusedValuesToSequence(oid, Arrays.asList(2L, 4L), result);
        assertEquals(2L, repositoryService.advanceSequence(oid, result));
        assertEquals(4L, repositoryService.advanceSequence(oid, result));
        assertEquals(5L, repositoryService.advanceSequence(oid, result));
        assertEquals(6L, repositoryService.advanceSequence(oid, result));
        repositoryService.returnUnusedValuesToSequence(oid, null, result);
        repositoryService.returnUnusedValuesToSequence(oid, new ArrayList<>(), result);
        repositoryService.returnUnusedValuesToSequence(oid, Arrays.asList(6L), result);
        assertEquals(6L, repositoryService.advanceSequence(oid, result));
        repositoryService.returnUnusedValuesToSequence(oid, Arrays.asList(0L, 1L, 2L, 3L, 4L, 5L, 6L), result);     // only 0-4 will be returned
        assertEquals(0L, repositoryService.advanceSequence(oid, result));
        assertEquals(1L, repositoryService.advanceSequence(oid, result));
        assertEquals(2L, repositoryService.advanceSequence(oid, result));
        assertEquals(3L, repositoryService.advanceSequence(oid, result));
        assertEquals(4L, repositoryService.advanceSequence(oid, result));
        assertEquals(7L, repositoryService.advanceSequence(oid, result));
        assertEquals(8L, repositoryService.advanceSequence(oid, result));
        assertEquals(9L, repositoryService.advanceSequence(oid, result));
        assertEquals(0L, repositoryService.advanceSequence(oid, result));
        assertEquals(1L, repositoryService.advanceSequence(oid, result));
        assertEquals(2L, repositoryService.advanceSequence(oid, result));
    }

    @Test
    public void test020_ReachingLimit() throws Exception {
        OperationResult result = new OperationResult("test020_ReachingLimit");
        final File file = new File(TEST_DIR + "sequence-bound.xml");
        PrismObject<SequenceType> sequence = prismContext.parseObject(file);
        String oid = repositoryService.addObject(sequence, null, result);

        assertEquals(0L, repositoryService.advanceSequence(oid, result));
        assertEquals(1L, repositoryService.advanceSequence(oid, result));
        assertEquals(2L, repositoryService.advanceSequence(oid, result));
        assertEquals(3L, repositoryService.advanceSequence(oid, result));
        assertEquals(4L, repositoryService.advanceSequence(oid, result));
        assertEquals(5L, repositoryService.advanceSequence(oid, result));
        assertEquals(6L, repositoryService.advanceSequence(oid, result));
        assertEquals(7L, repositoryService.advanceSequence(oid, result));
        assertEquals(8L, repositoryService.advanceSequence(oid, result));
        assertEquals(9L, repositoryService.advanceSequence(oid, result));
        try {
            long value = repositoryService.advanceSequence(oid, result);
            fail("Expected an exception, got value of " + value);
        } catch (SystemException e) {
            // ok
        }
    }

    @Test
    public void test031_OneThreadReturning() throws Exception {

        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1, 5)
        };

        concurrencyUniversal("Test031", "sequence-unbound.xml", 10000L, mts, true);
    }

    @Test
    public void test032_TwoThreadsReturning() throws Exception {

        WorkerThread[] mts = new WorkerThread[] {
                new WorkerThread(1, 5),
                new WorkerThread(2, 5)
        };

        concurrencyUniversal("Test032", "sequence-unbound.xml", 10000L, mts, true);
    }

    @Test
    public void test033_TenThreadsReturning() throws Exception {

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

        concurrencyUniversal("Test033", "sequence-unbound.xml", 10000L, mts, true);
    }


    private void concurrencyUniversal(String name, String sequenceFileName, long duration, WorkerThread[] workerThreads, boolean alwaysOrder) throws Exception {

        Session session = getFactory().openSession();
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                System.out.println(">>>>" + connection.getTransactionIsolation());
            }
        });
        session.close();

        final File file = new File(TEST_DIR + sequenceFileName);
        PrismObject<SequenceType> sequence = prismContext.parseObject(file);
        sequence.asObjectable().setName(new PolyStringType(name));

        OperationResult result = new OperationResult("Concurrency Test");
        String oid = repositoryService.addObject(sequence, null, result);

        LOGGER.info("*** Object added: " + oid + " ***");

        LOGGER.info("*** Starting modifier threads ***");

        for (WorkerThread t : workerThreads) {
            t.setOid(oid);
            t.start();
        }

        LOGGER.info("*** Waiting " + duration + " ms ***");
        Thread.sleep(duration);

        for (WorkerThread t : workerThreads) {
            t.stop = true;
        }

        long endTime = System.currentTimeMillis() + STOP_TIMEOUT;
        for (;;) {
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
            LOGGER.info("Worker thread {} finished after {} iterations with result: {}", t.id, t.counter, t.threadResult != null ? t.threadResult : "OK");
        }

        for (WorkerThread t : workerThreads) {
            if (t.threadResult != null) {
                throw new AssertionError("Worker thread " + t.id + " finished with an exception: " + t.threadResult, t.threadResult);
            }
        }

        List<Long> allValues = new ArrayList<>();
        for (WorkerThread t : workerThreads) {
            allValues.addAll(t.values);
        }
        if (alwaysOrder || workerThreads.length > 1) {
            Collections.sort(allValues);
        }
        LOGGER.trace("Checking a list of {} values", allValues.size());
        for (int i = 0; i < allValues.size(); i++) {
            if (allValues.get(i) != i) {
                LOGGER.error("Incorrect value at position {}: {}", i, allValues.get(i));
                for (WorkerThread t : workerThreads) {
                    LOGGER.info("Thread {}: {}", t.id, t.values);
                }
                fail("Incorrect value at position " + i + ": " + allValues.get(i));
            }
        }
    }

    class WorkerThread extends Thread {
        int id;
        String oid;                                 // sequence to use
        List<Long> values = new ArrayList<>();
        volatile Throwable threadResult;
        volatile int counter = 0;
        int returnEach;
        int countToReturn;

        WorkerThread(int id, int returnEach) {
            this.id = id;
            this.returnEach = returnEach;
            this.countToReturn = returnEach;
        }

        WorkerThread(int id) {
            this(id, 0);
        }


        public volatile boolean stop = false;

        @Override
        public void run() {
            try {
                while (!stop) {
                    runOnce();
                    counter++;
                }
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Unexpected exception: " + t, t);
                threadResult = t;
            }
        }

        public void runOnce() throws SchemaException, ObjectNotFoundException {
            OperationResult result = new OperationResult("run");
            long value = repositoryService.advanceSequence(oid, result);
            LOGGER.debug("Advance sequence returned {}", value);
            values.add(value);
            if (returnEach > 0) {
                if (countToReturn > 0) {
                    countToReturn--;
                } else {
                    countToReturn = returnEach;
                    int i = (int) (Math.random() * values.size());
                    long v = values.remove(i);
                    repositoryService.returnUnusedValuesToSequence(oid, Arrays.asList(v), result);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    value = repositoryService.advanceSequence(oid, result);
                    LOGGER.debug("Advance sequence returned {} (after return)", value);
                    values.add(value);
                }
            }
        }

		public void setOid(String oid) {
            this.oid = oid;
        }
   }

}
