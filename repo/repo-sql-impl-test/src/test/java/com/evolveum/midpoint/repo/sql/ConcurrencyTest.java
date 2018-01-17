/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.repo.sql.testing.SqlRepoTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.hibernate.Session;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
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


        Checker checker = (iteration, oid) -> {
            PrismObject<UserType> userRetrieved = repositoryService.getObject(UserType.class, oid, null, new OperationResult("dummy"));
            String givenName = userRetrieved.asObjectable().getGivenName().getOrig();
            String assignmentDescription = userRetrieved.asObjectable().getAssignment().get(0).getDescription();
            LOGGER.info("[" + iteration + "] givenName = " + givenName + ", assignment description = " + assignmentDescription);
            if (!givenName.equals(assignmentDescription)) {
                String msg = "Inconsistent object state: GivenName = " + givenName + ", assignment description = " + assignmentDescription;
                LOGGER.error(msg);
                throw new AssertionError(msg);
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
                                new NameItemPathSegment(AssignmentType.F_CONSTRUCTION)),
                        true),
        };


        Checker checker = (iteration, oid) -> {
            PrismObject<UserType> userRetrieved = repositoryService.getObject(UserType.class, oid, null, new OperationResult("dummy"));
            String givenName = userRetrieved.asObjectable().getGivenName().getOrig();
            String familyName = userRetrieved.asObjectable().getFamilyName().getOrig();
            String assignmentDescription = userRetrieved.asObjectable().getAssignment().get(0).getDescription();
            String referenceDescription = userRetrieved.asObjectable().getAssignment().get(0).getConstruction().getDescription();
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
        };

        concurrencyUniversal("Test4", 60000L, 0L, mts, checker);
    }

    @FunctionalInterface
    private interface Checker {
        void check(int iteration, String oid) throws Exception;
    }

    private void concurrencyUniversal(String name, long duration, long waitStep, ModifierThread[] modifierThreads, Checker checker) throws Exception {

        Session session = getFactory().openSession();
        session.doWork(connection -> System.out.println(">>>>" + connection.getTransactionIsolation()));
        session.close();

        final File file = new File("src/test/resources/concurrency/user.xml");
        PrismObject<UserType> user = prismContext.parseObject(file);
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
            if (mt.threadResult != null) {
                throw new AssertionError("Modifier thread " + mt.id + " finished with an exception: " + mt.threadResult, mt.threadResult);
            }
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

        void runOnce() {

            OperationResult result = new OperationResult("run");

            LOGGER.info(" --- Iteration number " + counter + " for " + attributeNames() + " ---");
            PrismObjectDefinition<?> userPrismDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);

            String prefix = lastName(attribute1);
            String dataWritten = "[" + prefix + ":" + Integer.toString(counter++) + "]";

            PrismPropertyDefinition<?> propertyDefinition1 = userPrismDefinition.findPropertyDefinition(attribute1);
            if (propertyDefinition1 == null) {
                throw new IllegalArgumentException("No definition for " + attribute1 + " in " + userPrismDefinition);
            }
            PropertyDelta<?> delta1 = new PropertyDelta<>(attribute1, propertyDefinition1, prismContext);
            //noinspection unchecked
            delta1.setValueToReplace(new PrismPropertyValue(poly ? new PolyString(dataWritten) : dataWritten));
            List<ItemDelta> deltas = new ArrayList<>();
            deltas.add(delta1);

            ItemDefinition propertyDefinition2 = null;
            if (attribute2 != null) {
                propertyDefinition2 = userPrismDefinition.findItemDefinition(attribute2);
                if (propertyDefinition2 == null) {
                    throw new IllegalArgumentException("No definition for " + attribute2 + " in " + userPrismDefinition);
                }
                
                ItemDelta delta2;
                if (propertyDefinition2 instanceof PrismContainerDefinition) {
                	delta2 = new ContainerDelta(attribute2, (PrismContainerDefinition) propertyDefinition2, prismContext);
                } else {
                    delta2 = new PropertyDelta(attribute2, (PrismPropertyDefinition) propertyDefinition2, prismContext);
                }
                if (ConstructionType.COMPLEX_TYPE.equals(propertyDefinition2.getTypeName())) {
                    ConstructionType act = new ConstructionType();
                    act.setDescription(dataWritten);
                    delta2.setValueToReplace(act.asPrismContainerValue());
                } else {
                    delta2.setValueToReplace(new PrismPropertyValue(dataWritten));
                }
                deltas.add(delta2);
            }

            try {
                repositoryService.modifyObject(UserType.class, oid, deltas, result);
				result.computeStatus();
				if (result.isError()) {
					LOGGER.error("Error found in operation result:\n{}", result.debugDump());
					throw new IllegalStateException("Error found in operation result");
				}
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
                    user = repositoryService.getObject(UserType.class, oid, null, result);
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
                    if (ConstructionType.COMPLEX_TYPE.equals(propertyDefinition2.getTypeName())) {
                        dataRead = ((ConstructionType)user.findContainer(attribute2).getValue().getValue()).getDescription();
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


    @Test(enabled = true)
    public void concurrency010_SearchIterative() throws Exception {

        String name = "Test10";
        final String newFullName = "new-full-name";

        final File file = new File("src/test/resources/concurrency/user.xml");
        PrismObject<UserType> user = prismContext.parseObject(file);
        user.asObjectable().setName(new PolyStringType(name));

        final OperationResult result = new OperationResult("Concurrency Test10");
        String oid = repositoryService.addObject(user, null, result);

        repositoryService.searchObjectsIterative(UserType.class,
                QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(UserType.F_NAME).eqPoly(name).matchingOrig().build(),
                (object, parentResult) -> {
                    LOGGER.info("Handling " + object + "...");
                    ObjectDelta delta = ObjectDelta.createModificationReplaceProperty(UserType.class, object.getOid(),
		                    UserType.F_FULL_NAME, prismContext, new PolyString(newFullName));
                    try {
                        repositoryService.modifyObject(UserType.class,
                            object.getOid(),
                            delta.getModifications(),
                            parentResult);
                    } catch (Exception e) {
                        throw new RuntimeException("Exception in handle method", e);
                    }
                    return true;
                },
                null, false, result);

        PrismObject<UserType> reloaded = repositoryService.getObject(UserType.class, oid, null, result);
        AssertJUnit.assertEquals("Full name was not changed", newFullName, reloaded.asObjectable().getFullName().getOrig());
    }
}
