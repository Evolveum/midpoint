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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ExtDictionaryConcurrencyTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ExtDictionaryConcurrencyTest.class);

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
//        for (int i = 0; i < WORKER_COUNT; i++) {
//            executors.execute(new SelectWorker(this));
//        }

        for (int i = 1; i <= 1000; i++) {
            List<Future> futures = new ArrayList<>();
            for (Worker worker : workers) {
                worker.setIndex(i);

                futures.add(executors.submit(worker));
            }

            for (Future f : futures) {
                f.get();
            }

            futures.clear();

            Thread.sleep(100);
        }

        executors.shutdownNow();
    }

    private static class SelectWorker<T extends ObjectType> implements Runnable {

        private ExtDictionaryConcurrencyTest test;

        public SelectWorker(ExtDictionaryConcurrencyTest test) {
            this.test = test;
        }

        @Override
        public void run() {
            try {
                OperationResult result = new OperationResult("search");

                SchemaRegistry registry = test.prismContext.getSchemaRegistry();

                ObjectQuery query = ObjectQuery.createObjectQuery(
                        SubstringFilter.createSubstring(
                                new ItemPath(UserType.F_NAME),
                                registry.findComplexTypeDefinitionByCompileTimeClass(UserType.class).findPropertyDefinition(UserType.F_NAME),
                                test.prismContext, null, "worker", false, false));
                List<PrismObject<UserType>> res = test.repositoryService.searchObjects(UserType.class, query, new ArrayList<>(), result);
                LOGGER.info("Found {} users", res.size());
            } catch (Exception ex) {
                LOGGER.error("Search exception", ex);
            }
        }
    }

    private static class Worker<T extends ObjectType> implements Runnable {

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
            LOGGER.info("Starting for oid {} index {}", oid, index);

            OperationResult result = new OperationResult("Test: " + attribute + index);
            try {
                ItemPath path = new ItemPath(UserType.F_EXTENSION, new QName(NAMESPACE, attribute + index));
//                ItemPath path = new ItemPath(UserType.F_DESCRIPTION);
                ObjectDelta delta = ObjectDelta.createModificationAddProperty(type, oid, path,
                        test.prismContext, attribute + index);

                test.repositoryService.modifyObject(type, oid, delta.getModifications(), result);

                PrismObject obj = test.repositoryService.getObject(type, oid, new ArrayList<>(), result);
                String ps = (String) obj.getPropertyRealValue(path, String.class);
                AssertJUnit.assertEquals(attribute + index, ps);
            } catch (Exception ex) {
                LOGGER.error("Exception", ex);

                AssertJUnit.fail(ex.getMessage());
            } finally {
                result.computeStatus();
            }

            LOGGER.info("Finished for oid {} index {}", oid, index);
        }
    }
}
