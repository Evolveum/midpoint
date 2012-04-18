/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavol Mederly
 */

@ContextConfiguration(locations = {
        "../../../../../application-context-sql-server-mode-test.xml",
        "../../../../../application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "../../../../../application-context-configuration-sql-test.xml"})

public class ConcurrencyTest extends AbstractTestNGSpringContextTests {

    private static final Trace LOGGER = TraceManager.getTrace(ConcurrencyTest.class);

    @Autowired(required = true)
    RepositoryService repositoryService;
    @Autowired(required = true)
    PrismContext prismContext;
    @Autowired
    SessionFactory factory;
    private static final long WAIT_TIME = 15000;
    private static final long WAIT_STEP =   500;

    @Test
    public void concurrency001() throws Exception {

        final File file = new File("src/test/resources/concurrency/user.xml");
        PrismObject<UserType> user = prismContext.getPrismDomProcessor().parseObject(file);

        OperationResult result = new OperationResult("Concurrency Test 001");
        String oid = repositoryService.addObject(user, result);

        LOGGER.info("*** Object added: " + oid + " ***");

        /*
         *  Two threads: each repeatedly modifies its own attribute of the user
         *   - thread1: givenName
         *   - thread2: familyName
         *  ... and checks whether its modifications are preserved.
         *
         *  Modifications are following:
         *  - from empty value to "1", "2", ... and so on
         *  (for both attributes)
         *
         */

        ModifierThread mt1 = new ModifierThread(oid, UserType.F_GIVEN_NAME);
        ModifierThread mt2 = new ModifierThread(oid, UserType.F_FAMILY_NAME);

        LOGGER.info("*** Starting modifier threads ***");
        mt1.start();
        mt2.start();

        LOGGER.info("*** Waiting " + WAIT_TIME + " ms ***");
        for (long time = 0; time < WAIT_TIME; time += WAIT_STEP) {
            Thread.sleep(WAIT_STEP);
            if (!mt1.isAlive() || !mt2.isAlive()) {
                LOGGER.error("At least one of threads died prematurely, finishing waiting.");
                break;
            }
        }

        mt1.stop = true;            // stop the threads
        mt2.stop = true;

        // we do not have to wait for the threads to be stopped, just examine their results

        System.out.println("Thread 1 has done " + (mt1.counter-1) + " iterations, thread 2 has done " + (mt2.counter-1) + " iterations.");
        LOGGER.info("*** Thread 1 has done " + (mt1.counter-1) + " iterations, thread 2 has done " + (mt2.counter-1) + " iterations. ***");

        Thread.sleep(1000);         // give the threads a chance to finish (before repo will be shut down)

        AssertJUnit.assertTrue("Modifier thread 1 finished with an exception: " + mt1.threadResult, mt1.threadResult == null);
        AssertJUnit.assertTrue("Modifier thread 2 finished with an exception: " + mt2.threadResult, mt2.threadResult == null);
    }

    class ModifierThread extends Thread {

        String oid;                 // object to modify
        QName attribute;           // attribute to modify
        volatile Exception threadResult;
        volatile int counter = 1;

        ModifierThread(String oid, QName attribute) {
            this.oid = oid;
            this.attribute = attribute;
            this.setName("Modifier for " + attribute.getLocalPart());
        }

        public volatile boolean stop = false;

        @Override
        public void run() {

            PrismObjectDefinition<?> userPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
            OperationResult result = new OperationResult("run");

            while (!stop) {

                LOGGER.info(" --- Iteration number " + counter + " for " + attribute.getLocalPart() + " ---");

                String dataWritten = Integer.toString(counter++);

                PropertyDelta<?> delta = PropertyDelta.createReplaceDeltaOrEmptyDelta(userPrismDefinition, attribute, dataWritten);
                List<? extends PropertyDelta<?>> deltas = Arrays.asList(delta);
                try {
                    repositoryService.modifyObject(UserType.class, oid, deltas, result);
                } catch (Exception e) {
                    threadResult = new RuntimeException("modifyObject failed while modifying attribute " + attribute.getLocalPart() + " to value " + dataWritten, e);
                    LOGGER.error("modifyObject failed while modifying attribute " + attribute.getLocalPart() + " to value " + dataWritten, e);
                    threadResult = e;
                    return;     // finish processing
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                PrismObject<UserType> user;
                try {
                    user = repositoryService.getObject(UserType.class, oid, new PropertyReferenceListType(), result);
                } catch (Exception e) {
                    threadResult = new RuntimeException("getObject failed while getting attribute " + attribute.getLocalPart(), e);
                    LOGGER.error("getObject failed while getting attribute " + attribute.getLocalPart(), e);
                    threadResult = e;
                    return;     // finish processing
                }

                // check the attribute

                String dataRead = user.getPropertyRealValue(attribute, String.class);
                if (!dataWritten.equals(dataRead)) {
                    threadResult = new RuntimeException("Data read back (" + dataRead + ") does not match the data written (" + dataWritten + ") on attribute " + attribute.getLocalPart());
                    LOGGER.error("compare failed", threadResult);
                    return;
                }

            }

        }
    }
}
