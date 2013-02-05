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

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavol Mederly
 */

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ConcurrencyTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ConcurrencyTest.class);

    private static final long WAIT_TIME = 60000;
    private static final long WAIT_STEP = 500;

    @Test
    public void concurrency001() throws Exception {
        Session session = getFactory().openSession();
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                System.out.println(">>>>" + connection.getTransactionIsolation());
            }
        });
        session.close();

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

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, oid, UserType.F_GIVEN_NAME, true),
                new ModifierThread(2, oid, UserType.F_FAMILY_NAME, true),
//                new ModifierThread(3, oid, UserType.F_DESCRIPTION, false),
//                new ModifierThread(4, oid, UserType.F_EMAIL_ADDRESS, false),
//                new ModifierThread(5, oid, UserType.F_TELEPHONE_NUMBER, false),
//                new ModifierThread(6, oid, UserType.F_EMPLOYEE_NUMBER, false),
//                new ModifierThread(8, oid, UserType.F_EMAIL_ADDRESS),
//                new ModifierThread(9, oid, UserType.F_EMPLOYEE_NUMBER)
        };

        LOGGER.info("*** Starting modifier threads ***");
        for (ModifierThread mt : mts) {
            mt.start();
        }

        LOGGER.info("*** Waiting " + WAIT_TIME + " ms ***");
        main:
        for (long time = 0; time < WAIT_TIME; time += WAIT_STEP) {
            Thread.sleep(WAIT_STEP);
            for (ModifierThread mt : mts) {
                if (!mt.isAlive()) {
                    LOGGER.error("At least one of threads died prematurely, finishing waiting.");
                    break main;
                }
            }
        }

        for (ModifierThread mt : mts) {
            mt.stop = true;             // stop the threads
            System.out.println("Thread " + mt.id + " has done " + (mt.counter - 1) + " iterations");
            LOGGER.info("Thread " + mt.id + " has done " + (mt.counter - 1) + " iterations");
        }

        // we do not have to wait for the threads to be stopped, just examine their results

        Thread.sleep(1000);         // give the threads a chance to finish (before repo will be shut down)

        for (ModifierThread mt : mts) {
            LOGGER.info("Modifier thread " + mt.id + " finished with an exception: ", mt.threadResult);
        }

        for (ModifierThread mt : mts) {
            AssertJUnit.assertTrue("Modifier thread " + mt.id + " finished with an exception: " + mt.threadResult, mt.threadResult == null);
        }
    }

    class ModifierThread extends Thread {

        int id;
        String oid;                 // object to modify
        QName attribute;           // attribute to modify
        boolean poly;
        volatile Throwable threadResult;
        volatile int counter = 1;

        ModifierThread(int id, String oid, QName attribute, boolean poly) {
            this.id = id;
            this.oid = oid;
            this.attribute = attribute;
            this.poly = poly;
            this.setName("Modifier for " + attribute.getLocalPart());
        }

        public volatile boolean stop = false;

        @Override
        public void run() {
            try {
                run1();
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Unexpected exception: " + t, t);
                threadResult = t;
            }
        }

        public void run1() {

            PrismObjectDefinition<?> userPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
            OperationResult result = new OperationResult("run");

            while (!stop) {

                LOGGER.info(" --- Iteration number " + counter + " for " + attribute.getLocalPart() + " ---");

                String prefix = attribute.getLocalPart();

                String dataWritten = "[" + prefix + ":" + Integer.toString(counter++) + "]";

                PropertyDelta<?> delta = PropertyDelta.createReplaceDeltaOrEmptyDelta(userPrismDefinition, attribute, poly ? new PolyString(dataWritten) : new String(dataWritten));
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
                    user = repositoryService.getObject(UserType.class, oid, result);
                } catch (Exception e) {
                    threadResult = new RuntimeException("getObject failed while getting attribute " + attribute.getLocalPart(), e);
                    LOGGER.error("getObject failed while getting attribute " + attribute.getLocalPart(), e);
                    threadResult = e;
                    return;     // finish processing
                }

                // check the attribute

                String dataRead;
                if (poly) {
                    dataRead = user.getPropertyRealValue(attribute, PolyString.class).getOrig();
                } else {
                    dataRead = user.getPropertyRealValue(attribute, String.class);
                }

                if (!dataWritten.equals(dataRead)) {
                    threadResult = new RuntimeException("Data read back (" + dataRead + ") does not match the data written (" + dataWritten + ") on attribute " + attribute.getLocalPart());
                    LOGGER.error("compare failed", threadResult);
                    return;
                }

            }

        }
    }
}
