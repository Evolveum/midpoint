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
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavol Mederly
 */

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ConcurrencyTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(ConcurrencyTest.class);

    //private static final long WAIT_TIME = 60000;
    //private static final long WAIT_STEP = 500;

    @Test(enabled = true)
    public void concurrency001_TwoWriters_OneAttributeEach__NoReader() throws Exception {

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, new ItemPath(UserType.F_GIVEN_NAME), true, null, true),
                new ModifierThread(2, new ItemPath(UserType.F_FAMILY_NAME), true, null, true),
//                new ModifierThread(3, oid, UserType.F_DESCRIPTION, false),
//                new ModifierThread(4, oid, UserType.F_EMAIL_ADDRESS, false),
//                new ModifierThread(5, oid, UserType.F_TELEPHONE_NUMBER, false),
//                new ModifierThread(6, oid, UserType.F_EMPLOYEE_NUMBER, false),
//                new ModifierThread(8, oid, UserType.F_EMAIL_ADDRESS),
//                new ModifierThread(9, oid, UserType.F_EMPLOYEE_NUMBER)
        };

        concurrencyUniversal("Test1", 30000L, 500L, mts, null);
    }

    @Test(enabled = true)
    public void concurrency002_FourWriters_OneAttributeEach__NoReader() throws Exception {

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, new ItemPath(UserType.F_GIVEN_NAME), true, null, true),
                new ModifierThread(2, new ItemPath(UserType.F_FAMILY_NAME), true, null, true),
                new ModifierThread(3, new ItemPath(UserType.F_DESCRIPTION), false, null, true),
                new ModifierThread(4, new ItemPath(UserType.F_EMAIL_ADDRESS), false, null, true)
        };

        concurrencyUniversal("Test2", 60000L, 500L, mts, null);
    }

    @Test(enabled = true)
    public void concurrency003_OneWriter_TwoAttributes__OneReader() throws Exception {

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, new ItemPath(UserType.F_GIVEN_NAME), true,
                        new ItemPath(
                                new NameItemPathSegment(UserType.F_ASSIGNMENT),
                                new IdItemPathSegment(1L),
                                new NameItemPathSegment(AssignmentType.F_DESCRIPTION)),
                        true)
        };


        Checker checker = new Checker() {
            @Override
            public void check(int iteration, String oid) throws Exception {

                PrismObject<UserType> userRetrieved = repositoryService.getObject(UserType.class, oid, new OperationResult("dummy"));

                String givenName = userRetrieved.asObjectable().getGivenName().getOrig();
                String assignmentDescription = userRetrieved.asObjectable().getAssignment().get(0).getDescription();
                LOGGER.info("[" + iteration + "] givenName = " + givenName + ", assignment description = " + assignmentDescription);
                if (!givenName.equals(assignmentDescription)) {
                    String msg = "Inconsistent object state: GivenName = " + givenName + ", assignment description = " + assignmentDescription;
                    LOGGER.error(msg);
                    throw new AssertionError(msg);
                }
            }
        };

        concurrencyUniversal("Test3", 60000L, 0L, mts, checker);
    }

    @Test(enabled = true)
    public void concurrency004_TwoWriters_TwoAttributesEach__OneReader() throws Exception {

        ModifierThread[] mts = new ModifierThread[]{
                new ModifierThread(1, new ItemPath(UserType.F_GIVEN_NAME), true,
                        new ItemPath(
                                new NameItemPathSegment(UserType.F_ASSIGNMENT),
                                new IdItemPathSegment(1L),
                                new NameItemPathSegment(AssignmentType.F_DESCRIPTION)),
                        true),

                new ModifierThread(2, new ItemPath(UserType.F_FAMILY_NAME), true,
                        new ItemPath(
                                new NameItemPathSegment(UserType.F_ASSIGNMENT),
                                new IdItemPathSegment(1L),
                                new NameItemPathSegment(AssignmentType.F_ACCOUNT_CONSTRUCTION)),
                        true),
        };


        Checker checker = new Checker() {
            @Override
            public void check(int iteration, String oid) throws Exception {

                PrismObject<UserType> userRetrieved = repositoryService.getObject(UserType.class, oid, new OperationResult("dummy"));

                String givenName = userRetrieved.asObjectable().getGivenName().getOrig();
                String familyName = userRetrieved.asObjectable().getFamilyName().getOrig();
                String assignmentDescription = userRetrieved.asObjectable().getAssignment().get(0).getDescription();
                String referenceDescription = userRetrieved.asObjectable().getAssignment().get(0).getAccountConstruction().getDescription();
                LOGGER.info("[" + iteration + "] givenName = " + givenName + ", assignment description = " + assignmentDescription + ", familyName = " + familyName + ", referenceDescription = " + referenceDescription);
                if (!givenName.equals(assignmentDescription)) {
                    String msg = "Inconsistent object state: GivenName = " + givenName + ", assignment description = " + assignmentDescription;
                    LOGGER.error(msg);
                    throw new AssertionError(msg);
                }
                if (!familyName.equals(referenceDescription)) {
                    String msg = "Inconsistent object state: FamilyName = " + familyName + ", account construction description = " + referenceDescription;
                    LOGGER.error(msg);
                    throw new AssertionError(msg);
                }
            }
        };

        concurrencyUniversal("Test4", 60000L, 0L, mts, checker);
    }

    private interface Checker {
        void check(int iteration, String oid) throws Exception;
    }

    private void concurrencyUniversal(String name, long duration, long waitStep, ModifierThread[] modifierThreads, Checker checker) throws Exception {

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
        user.asObjectable().setName(new PolyStringType(name));

        OperationResult result = new OperationResult("Concurrency Test");
        String oid = repositoryService.addObject(user, null, result);

        LOGGER.info("*** Object added: " + oid + " ***");

        LOGGER.info("*** Starting modifier threads ***");

//        modifierThreads[1].setOid(oid);
//        modifierThreads[1].runOnce();
//        if(true) return;

        for (ModifierThread mt : modifierThreads) {
            mt.setOid(oid);
            mt.start();
        }

        LOGGER.info("*** Waiting " + duration + " ms ***");
        long startTime = System.currentTimeMillis();
        int readIteration = 1;
        main:
        while (System.currentTimeMillis() - startTime < duration) {

            if (checker != null) {
                checker.check(readIteration, oid);
            }

            if (waitStep > 0L) {
                Thread.sleep(waitStep);
            }

            for (ModifierThread mt : modifierThreads) {
                if (!mt.isAlive()) {
                    LOGGER.error("At least one of threads died prematurely, finishing waiting.");
                    break main;
                }
            }

            readIteration++;
        }

        for (ModifierThread mt : modifierThreads) {
            mt.stop = true;             // stop the threads
            System.out.println("Thread " + mt.id + " has done " + (mt.counter - 1) + " iterations");
            LOGGER.info("Thread " + mt.id + " has done " + (mt.counter - 1) + " iterations");
        }

        // we do not have to wait for the threads to be stopped, just examine their results

        Thread.sleep(1000);         // give the threads a chance to finish (before repo will be shut down)

        for (ModifierThread mt : modifierThreads) {
            LOGGER.info("Modifier thread " + mt.id + " finished with an exception: ", mt.threadResult);
        }

        for (ModifierThread mt : modifierThreads) {
            AssertJUnit.assertTrue("Modifier thread " + mt.id + " finished with an exception: " + mt.threadResult, mt.threadResult == null);
        }
    }

    class ModifierThread extends Thread {

        int id;
        String oid;                 // object to modify
        ItemPath attribute1;           // attribute to modify
        ItemPath attribute2;           // attribute to modify
        boolean poly;
        boolean checkValue;
        String lastVersion = null;
        volatile Throwable threadResult;
        volatile int counter = 1;

        ModifierThread(int id, ItemPath attribute1, boolean poly, ItemPath attribute2, boolean checkValue) {
            this.id = id;
            this.attribute1 = attribute1;
            this.attribute2 = attribute2;
            this.poly = poly;
            this.setName("Modifier for " + attributeNames());
            this.checkValue = checkValue;
        }

        private String attributeNames() {
            return lastName(attribute1) + (attribute2 != null ? "/" + lastName(attribute2) : "");
        }

        private String lastName(ItemPath path) {
            List<ItemPathSegment> segments = path.getSegments();
            for (int i = segments.size()-1; i >= 0; i++) {
                if (segments.get(i) instanceof NameItemPathSegment) {
                    return ((NameItemPathSegment) segments.get(i)).getName().getLocalPart();
                }
            }
            return "?";
        }

        public volatile boolean stop = false;

        @Override
        public void run() {
            try {
                while (!stop) {
                    runOnce();
                }
            } catch (Throwable t) {
                LoggingUtils.logException(LOGGER, "Unexpected exception: " + t, t);
                threadResult = t;
            }
        }

        public void runOnce() {

            OperationResult result = new OperationResult("run");

            LOGGER.info(" --- Iteration number " + counter + " for " + attributeNames() + " ---");
            PrismObjectDefinition<?> userPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);

            String prefix = lastName(attribute1);
            String dataWritten = "[" + prefix + ":" + Integer.toString(counter++) + "]";

            PrismPropertyDefinition propertyDefinition1 = userPrismDefinition.findPropertyDefinition(attribute1);
            if (propertyDefinition1 == null) {
                throw new IllegalArgumentException("No definition for " + attribute1 + " in " + userPrismDefinition);
            }
            PropertyDelta delta1 = new PropertyDelta(attribute1, propertyDefinition1);
            delta1.setValueToReplace(new PrismPropertyValue(poly ? new PolyString(dataWritten) : dataWritten));
            List<PropertyDelta> deltas = new ArrayList<PropertyDelta>();
            deltas.add(delta1);

            PrismPropertyDefinition propertyDefinition2 = null;
            if (attribute2 != null) {
                propertyDefinition2 = userPrismDefinition.findPropertyDefinition(attribute2);
                if (propertyDefinition2 == null) {
                    throw new IllegalArgumentException("No definition for " + attribute2 + " in " + userPrismDefinition);
                }
                PropertyDelta delta2 = new PropertyDelta(attribute2, propertyDefinition2);
                if (AccountConstructionType.COMPLEX_TYPE.equals(propertyDefinition2.getTypeName())) {
                    AccountConstructionType act = new AccountConstructionType();
                    act.setDescription(dataWritten);
                    delta2.setValueToReplace(new PrismPropertyValue<AccountConstructionType>(act));
                } else {
                    delta2.setValueToReplace(new PrismPropertyValue(dataWritten));
                }
                deltas.add(delta2);
            }

            try {
                repositoryService.modifyObject(UserType.class, oid, deltas, result);
            } catch (Exception e) {
                String msg = "modifyObject failed while modifying attribute(s) " + attributeNames()
                        + " to value " + dataWritten;
                threadResult = new RuntimeException(msg, e);
                LOGGER.error(msg, e);
                threadResult = e;
                stop = true;
                return;     // finish processing
            }

            if (checkValue) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }

                PrismObject<UserType> user;
                try {
                    user = repositoryService.getObject(UserType.class, oid, result);
                } catch (Exception e) {
                    String msg = "getObject failed while getting attribute(s) " + attributeNames();
                    threadResult = new RuntimeException(msg, e);
                    LOGGER.error(msg, e);
                    threadResult = e;
                    stop = true;
                    return;     // finish processing
                }

                // check the attribute

                String dataRead;
                if (poly) {
                    dataRead = user.findProperty(attribute1).getRealValue(PolyString.class).getOrig();
                } else {
                    dataRead = user.findProperty(attribute1).getRealValue(String.class);
                }

                if (!dataWritten.equals(dataRead)) {
                    threadResult = new RuntimeException("Data read back (" + dataRead + ") does not match the data written (" + dataWritten + ") on attribute " + attribute1);
                    LOGGER.error("compare failed", threadResult);
                    stop = true;
                    return;
                }

                if (attribute2 != null) {
                    if (AccountConstructionType.COMPLEX_TYPE.equals(propertyDefinition2.getTypeName())) {
                        dataRead = user.findProperty(attribute2).getRealValue(AccountConstructionType.class).getDescription();
                    } else {
                        dataRead = user.findProperty(attribute2).getRealValue(String.class);
                    }

                    if (!dataWritten.equals(dataRead)) {
                        threadResult = new RuntimeException("Data read back (" + dataRead + ") does not match the data written (" + dataWritten + ") on attribute " + attribute2);
                        LOGGER.error("compare failed", threadResult);
                        stop = true;
                        return;
                    }
                }
                
                String currentVersion = user.getVersion();
                String versionError = SqlRepoTestUtil.checkVersionProgress(lastVersion, currentVersion);
                if (versionError != null) {
                    threadResult = new RuntimeException(versionError);
                    LOGGER.error(versionError);
                    stop = true;
                    return;
                }
                lastVersion = currentVersion;
            }
        }

		public void setOid(String oid) {
            this.oid = oid;
        }

    }

}
