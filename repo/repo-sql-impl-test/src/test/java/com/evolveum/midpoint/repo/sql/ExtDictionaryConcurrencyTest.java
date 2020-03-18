/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.xml.namespace.QName;

import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class ExtDictionaryConcurrencyTest extends BaseSQLRepoTest {

    private static final String NAMESPACE = "http://concurrency";

    private static final int WORKER_COUNT = 4;

    @Test
    public void testSerializationExceptions() throws Exception {
        OperationResult result = new OperationResult("testSerializationExceptions");

        Worker<UserType>[] workers = new Worker[WORKER_COUNT];
        for (int i = 0; i < workers.length; i++) {
            UserType userType = new UserType();
            userType.setName(new PolyStringType("" + System.currentTimeMillis() + " worker" + i));
            prismContext.adopt(userType);

            PrismObject user = userType.asPrismObject();
            String oid = repositoryService.addObject(user, new RepoAddOptions(), result);

            workers[i] = new Worker(this, UserType.class, oid, "testAttribute");
        }

        ExecutorService executors = Executors.newFixedThreadPool(workers.length * 2);

        for (int i = 1; i <= 300; i++) {
            List<Future<?>> futures = new ArrayList<>();
            for (Worker worker : workers) {
                worker.setIndex(i);

                futures.add(executors.submit(worker));
            }

            for (Future<?> f : futures) {
                f.get();
            }

            futures.clear();

            Thread.sleep(100);
        }

        executors.shutdownNow();
    }

    private class Worker<T extends ObjectType> implements Runnable {

        private ExtDictionaryConcurrencyTest test;

        private Class<T> type;
        private String oid;
        private String attribute;
        private int index;

        public Worker(ExtDictionaryConcurrencyTest test, Class<T> type,
                String oid, String attribute) {
            this.test = test;
            this.type = type;
            this.oid = oid;
            this.attribute = attribute;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        @Override
        public void run() {
            logger.info("Starting for oid {} index {}", oid, index);

            OperationResult result = new OperationResult("Test: " + attribute + index);
            try {
                ItemPath path = ItemPath.create(UserType.F_EXTENSION, new QName(NAMESPACE, attribute + index));
                ObjectDelta delta = test.prismContext.deltaFactory().object().createModificationAddProperty(type, oid, path,
                        attribute + index);

                test.repositoryService.modifyObject(type, oid, delta.getModifications(), result);

                PrismObject obj = test.repositoryService.getObject(type, oid, new ArrayList<>(), result);
                String ps = (String) obj.getPropertyRealValue(path, String.class);
                AssertJUnit.assertEquals(attribute + index, ps);
            } catch (Exception ex) {
                logger.error("Exception", ex);
                ex.printStackTrace();
                AssertJUnit.fail(ex.getMessage());
            } finally {
                result.computeStatus();
            }

            logger.info("Finished for oid {} index {}", oid, index);
        }
    }
}
