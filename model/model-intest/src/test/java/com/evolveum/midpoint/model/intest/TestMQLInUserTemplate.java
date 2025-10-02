package com.evolveum.midpoint.model.intest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMQLInUserTemplate extends AbstractInitializedModelIntegrationTest {

    private static final int WAIT_FOR_STOP = 10_000;

    public static final File TEST_DIR = new File("src/test/resources/object-template");

    private static final TestObject<ObjectTemplateType> USER_TEMPLATE =
            TestObject.file(TEST_DIR, "user-mql-template.xml", "6b7218fb-418a-4756-a254-792593c6e779");

    private static final TestObject<ServiceType> SERVICE_A =
            TestObject.file(TEST_DIR, "service-10843-a.xml", "1b523c19-a814-455a-af50-89b2c2a9dad4");
    private static final TestObject<ServiceType> SERVICE_B =
            TestObject.file(TEST_DIR, "service-10843-b.xml", "eb2f8eb2-139f-4637-b78d-86da3752b8dc");

    private static final TestObject<UserType> USER_MIKE =
            TestObject.file(TEST_DIR, "user-mike.xml", "2e5fdc85-ed4c-477a-9cad-89734b9d1702");
    private static final TestObject<UserType> USER_JACK =
            TestObject.file(TEST_DIR, "user-jack.xml", "f13323fa-76f8-4300-93b7-0117bfbc2e7e");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(USER_TEMPLATE, initResult);

        repoAdd(SERVICE_A, initResult);
        repoAdd(SERVICE_B, initResult);

        repoAdd(USER_MIKE, initResult);
        repoAdd(USER_JACK, initResult);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE.oid, initResult);
    }

    /**
     * MID-10483 MQL randomly fails in assignmentTargetSearch
     */
    @Test(enabled = false)
    public void test100MQLInUserTemplate() throws Exception {
        OperationResult result = getTestOperationResult();

        List<WorkerThread> threads = new ArrayList<>();
        threads.add(new WorkerThread(USER_MIKE.getObjectable(), SERVICE_A.oid));
        threads.add(new WorkerThread(USER_JACK.getObjectable(), SERVICE_B.oid));

        final int USER_COUNT = 15;

        for (int i = 0; i < USER_COUNT; i++) {
            UserType user = new UserType();
            user.setOid(UUID.randomUUID().toString());
            user.setName(PolyStringType.fromOrig(getTestName() + "-user-" + i));

            repoAddObject(user.asPrismObject(), "Creating user", RepoAddOptions.createOverwrite(), result);

            threads.add(new WorkerThread(user, null));
        }

        executeTest(240, threads);
    }

    private void executeTest(long timeSeconds, List<WorkerThread> threads) throws InterruptedException {
        for (WorkerThread thread : threads) {
            thread.start();
        }

        Thread.sleep(timeSeconds * 1000);

        for (WorkerThread thread : threads) {
            thread.stop = true;
        }

        long endTime = System.currentTimeMillis() + WAIT_FOR_STOP;
        for (; ; ) {
            long remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }
            for (WorkerThread t : threads) {
                t.join(remaining);
                remaining = endTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
            }
            if (threads.stream().noneMatch(Thread::isAlive)) {
                break;
            }
        }

        for (WorkerThread t : threads) {
            display("Worker thread " + t.getIdentifier() + " finished after " + t.counter
                    + " iterations with result: " + (t.threadResult != null ? t.threadResult : "OK"));
        }

        for (WorkerThread t : threads) {
            if (t.threadResult != null) {
                throw new AssertionError(
                        "Worker thread " + t.getIdentifier() + " finished with an exception: " + t.threadResult,
                        t.threadResult);
            }
        }
    }

    private class WorkerThread extends Thread {

        private final UserType user;

        private final String assignmentTargetRefOid;

        private volatile Throwable threadResult;
        private volatile int counter = 0;
        private volatile boolean stop = false;

        public WorkerThread(UserType user, String assignmentTargetRefOid) {
            this.user = user;
            this.assignmentTargetRefOid = assignmentTargetRefOid;
        }

        private String getIdentifier() {
            return user.getName().getOrig();
        }

        @Override
        public void run() {
            try {
                loginUser();

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

        private synchronized void loginUser()
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException {
            login(userAdministrator.clone());
        }

        private void runOnce() {
            Task task = getTestTask();
            OperationResult result = task.getResult();

            try {
                UserType user = getObject(UserType.class, this.user.getOid()).asObjectable();
//                Assertions.assertThat(user.getAssignment())
//                        .withFailMessage("Number of assignments BEFORE does not match expected")
//                        .isEmpty();

                ObjectDelta<UserType> delta = user.clone().asPrismObject().createModifyDelta();
                modelService.executeChanges(
                        List.of(delta),
                        ModelExecuteOptions.create()
                                .reconcile(),
                        task,
                        result
                );

                user = getObject(UserType.class, this.user.getOid()).asObjectable();
                if (assignmentTargetRefOid == null) {
                    Assertions.assertThat(user.getAssignment())
                            .withFailMessage("Number of assignments AFTER does not match expected")
                            .isEmpty();
                } else {
                    Assertions.assertThat(user.getAssignment())
                            .withFailMessage("Number of assignments AFTER does not match expected")
                            .hasSize(1);
                    Assertions.assertThat(user.getAssignment().get(0).getTargetRef().getOid())
                            .withFailMessage("Assignment target ref oid does not match expected")
                            .isEqualTo(assignmentTargetRefOid);
                }

                if (Math.random() < 0.5) {
                    // replace and again
                    repoAddObject(this.user.asPrismObject(), "Resetting user", RepoAddOptions.createOverwrite(), result);
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
