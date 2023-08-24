/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.tasks;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationExclusionReasonType.NOT_APPLICABLE_FOR_TASK;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.LINKED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType.UNMATCHED;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.fromNow;
import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getRootSyncTokenRealValueRequired;
import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME;
import static com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.evolveum.midpoint.test.TestTask;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.model.impl.trigger.ShadowReconcileTriggerHandler;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.model.intest.CommonTasks;
import com.evolveum.midpoint.model.intest.sync.SequenceChecker;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.TaskTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests various aspects of live sync task:
 *
 * 1. interruption of live sync task in various scenarios (see MID-5353, MID-5513),
 * 2. statistics (MID-5999, MID-5920),
 * 3. multiple intents (MID-8537).
 *
 * TODO other aspects
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLiveSyncTask extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/livesync");
    private static final int ERRORS_ACCOUNTS = 30;

    private DummyInterruptedSyncResource interruptedSyncResource;
    private DummyInterruptedSyncImpreciseResource interruptedSyncImpreciseResource;

    private static final DummyTestResource RESOURCE_DUMMY_NO_POLICY = new DummyTestResource(TEST_DIR,
            "resource-dummy-no-policy.xml", "3908fabe-8608-4db0-93ee-e06c5691eb8f", "noPolicy");
    private static final DummyTestResource RESOURCE_DUMMY_XFER1_SOURCE = new DummyTestResource(TEST_DIR,
            "resource-dummy-xfer1-source.xml", "28867569-2ea5-4ea9-9369-da4d4b624dbf", "xfer1-source");
    private static final DummyTestResource RESOURCE_DUMMY_XFER1_TARGET_DELETABLE = new DummyTestResource(TEST_DIR,
            "resource-dummy-xfer1-target-deletable.xml", "2779faac-0116-4dfe-9600-d24e6ba334c5", "xfer1-target-deletable");
    private static final DummyTestResource RESOURCE_DUMMY_XFER2_SOURCE = new DummyTestResource(TEST_DIR,
            "resource-dummy-xfer2-source.xml", "b2ac9c8f-0020-46ab-9e5d-10684900a63c", "xfer2-source");
    private static final DummyTestResource RESOURCE_DUMMY_XFER2_TARGET_NOT_DELETABLE = new DummyTestResource(TEST_DIR,
            "resource-dummy-xfer2-target-not-deletable.xml", "60a5f2d4-1abc-4178-a687-4a9627779676", "xfer2-target-not-deletable");
    private static final DummyTestResource RESOURCE_DUMMY_MULTI_CHANGES = new DummyTestResource(TEST_DIR,
            "resource-dummy-multi-changes.xml", "5448264d-cf1a-497e-bfa1-7aa8972247de", "multi-changes");
    private static final DummyTestResource RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE = new DummyTestResource(TEST_DIR,
            "resource-dummy-errors-source-precise.xml", "a20bb7b7-c5e9-4bbb-94e0-79e7866362e6", "errors-source-precise");
    private static final DummyTestResource RESOURCE_DUMMY_ERRORS_TARGET = new DummyTestResource(TEST_DIR,
            "resource-dummy-errors-target.xml", "10079e05-b98e-4630-92aa-26fa4ed8d0eb", "errors-target");

    private static final TestObject<RoleType> ROLE_XFER1 = TestObject.file(TEST_DIR, "role-xfer1.xml", "4b141ca2-3172-4d8a-8614-97e01ece5a9e");
    private static final TestObject<RoleType> ROLE_XFER2 = TestObject.file(TEST_DIR, "role-xfer2.xml", "59fdad1b-45fa-4a8c-bda4-d8a6ab980671");
    private static final TestObject<TaskType> TASK_XFER1 = TestObject.file(TEST_DIR, "task-xfer1.xml", "c9306381-efa8-499e-8b16-6d071d680451");
    private static final TestObject<TaskType> TASK_XFER2 = TestObject.file(TEST_DIR, "task-xfer2.xml", "d4f8b735-dfdb-450e-a680-dacfac4fafb0");
    private static final TestObject<TaskType> TASK_MULTI_CHANGES = TestObject.file(TEST_DIR, "task-multi-changes.xml", "33d6642a-6251-4b53-b78a-0cf44460e5c9");

    private static final TestObject<RoleType> ROLE_ERRORS_TARGET = TestObject.file(TEST_DIR, "role-errors-target.xml", "582af892-2490-4fb1-bc83-368ade2c5eb4");
    private static final TestObject<TaskType> TASK_ERRORS_PRECISE_IGNORE = TestObject.file(TEST_DIR, "task-errors-precise-ignore.xml", "ac52f0fd-9ff9-4699-87fb-c81b0b290d9f");
    private static final TestObject<TaskType> TASK_ERRORS_PRECISE_IGNORE_PARTIAL_STOP_ON_FATAL = TestObject.file(TEST_DIR, "task-errors-precise-ignore-partial-stop-on-fatal.xml", "6873df7b-e663-4070-868f-c75aa19c391c");
    private static final TestObject<TaskType> TASK_ERRORS_PRECISE_STOP_ON_ANY = TestObject.file(TEST_DIR, "task-errors-precise-stop-on-any.xml", "0e015e1e-aa68-4190-ba4f-adf88c58b162");
    private static final TestObject<TaskType> TASK_ERRORS_PRECISE_RETRY_LATER_ON_ANY = TestObject.file(TEST_DIR, "task-errors-precise-retry-later-on-any.xml", "2d7f0709-3e9b-4b92-891f-c5e1428b6458");
    private static final TestObject<TaskType> TASK_ERRORS_PRECISE_RETRY_LATER_MAX_4 = TestObject.file(TEST_DIR, "task-errors-precise-retry-later-max-4.xml", "0bdfdb9c-ccae-4202-a060-f9aab35bd211");

    private static final TestObject<TaskType> TASK_SLOW_RESOURCE = TestObject.file(TEST_DIR, "task-intsync-slow-resource.xml", "ca51f209-1ef5-42b3-84e7-5f639ee8e300");
    private static final TestObject<TaskType> TASK_SLOW_MODEL = TestObject.file(TEST_DIR, "task-intsync-slow-model.xml", "c37dda96-e547-41c2-b343-b890bc7fade9");
    private static final TestObject<TaskType> TASK_BATCHED = TestObject.file(TEST_DIR, "task-intsync-batched.xml", "ef22bf7b-5d28-4a57-b3a5-6fa58491eeb3");
    private static final TestObject<TaskType> TASK_ERROR = TestObject.file(TEST_DIR, "task-intsync-error.xml", "b697f3a8-9d02-4924-8627-c1f216e88ed3");
    private static final TestObject<TaskType> TASK_SLOW_RESOURCE_IMPRECISE = TestObject.file(TEST_DIR, "task-intsync-slow-resource-imprecise.xml", "82407cd3-7b1f-4054-b45a-fc4d9aed8ae3");
    private static final TestObject<TaskType> TASK_SLOW_MODEL_IMPRECISE = TestObject.file(TEST_DIR, "task-intsync-slow-model-imprecise.xml", "066c6993-8b94-445c-aaff-937184bbe6ca");
    private static final TestObject<TaskType> TASK_BATCHED_IMPRECISE = TestObject.file(TEST_DIR, "task-intsync-batched-imprecise.xml", "dcfe4c53-a851-4fe1-90eb-f75d9c65d2e6");
    private static final TestObject<TaskType> TASK_ERROR_IMPRECISE = TestObject.file(TEST_DIR, "task-intsync-error-imprecise.xml", "c554ec0f-95c3-40ac-b069-876708d28393");

    private static final TestObject<TaskType> TASK_DRY_RUN = TestObject.file(TEST_DIR, "task-intsync-dry-run.xml", "8b5b3b2d-6ef7-4cc8-8507-42778e0d869f");
    private static final TestObject<TaskType> TASK_DRY_RUN_WITH_UPDATE = TestObject.file(TEST_DIR, "task-intsync-dry-run-with-update.xml", "ebcc7393-e886-40ae-8a9f-dfa72230c658");

    private static final TestObject<TaskType> TASK_NO_POLICY = TestObject.file(TEST_DIR, "task-no-policy.xml", "b2aa4e0a-1fce-499d-8502-ece187b24ae4");

    private static final String USER_P = "user-p-";
    private static final String USER_I = "user-i-";

    private static final int ERROR_ON = 4;
    private static final int USERS = 100;

    private static final int XFER_ACCOUNTS = 10;

    private static final String ATTR_TYPE = "type";
    private static final DummyTestResource RESOURCE_DUMMY_MULTI_TYPE_SOURCE = new DummyTestResource(TEST_DIR,
            "resource-dummy-multi-type-source.xml", "1731de0c-b333-4f8e-abb1-eada3d38943d",
            "multi-type-source",
            c -> {
                c.addAttrDef(c.getAccountObjectClass(), ATTR_TYPE, String.class, false, false);
                c.setSyncStyle(DummySyncStyle.DUMB);
            });

    private static final TestTask TASK_MULTI_TYPE_LIVE_SYNC_TYPE_1 = new TestTask(
            TEST_DIR, "task-multi-type-live-sync-type-1.xml", "9ca22b74-bde8-45ca-be4e-cc77d95924c9");
    private static final TestTask TASK_MULTI_TYPE_LIVE_SYNC_TYPE_2 = new TestTask(
            TEST_DIR, "task-multi-type-live-sync-type-2.xml", "e77ef192-0c87-4d76-9424-a93b730c3e22");

    /**
     * Checked on "errors" resources/roles.
     */
    public static boolean produceErrors = true;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DummyInterruptedSyncResource.reset();
        DummyInterruptedSyncImpreciseResource.reset();

        interruptedSyncResource = new DummyInterruptedSyncResource();
        interruptedSyncResource.init(dummyResourceCollection, initTask, initResult);

        interruptedSyncImpreciseResource = new DummyInterruptedSyncImpreciseResource();
        interruptedSyncImpreciseResource.init(dummyResourceCollection, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_NO_POLICY, initTask, initResult).setSyncStyle(DummySyncStyle.DUMB);

        initDummyResource(RESOURCE_DUMMY_XFER1_SOURCE, initTask, initResult).setSyncStyle(DummySyncStyle.DUMB);
        initDummyResource(RESOURCE_DUMMY_XFER1_TARGET_DELETABLE, initTask, initResult);
        repoAdd(ROLE_XFER1, initResult);

        initDummyResource(RESOURCE_DUMMY_XFER2_SOURCE, initTask, initResult).setSyncStyle(DummySyncStyle.DUMB);
        initDummyResource(RESOURCE_DUMMY_XFER2_TARGET_NOT_DELETABLE, initTask, initResult);
        repoAdd(ROLE_XFER2, initResult);

        initDummyResource(RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE, initTask, initResult).setSyncStyle(DummySyncStyle.DUMB);
        initDummyResource(RESOURCE_DUMMY_ERRORS_TARGET, initTask, initResult);
        repoAdd(ROLE_ERRORS_TARGET, initResult);

        initDummyResource(RESOURCE_DUMMY_MULTI_CHANGES, initTask, initResult)
                .setSyncStyle(DummySyncStyle.DUMB);

        initLiveSyncTask(TASK_SLOW_RESOURCE, initTask, initResult);
        initLiveSyncTask(TASK_SLOW_RESOURCE_IMPRECISE, initTask, initResult);
        initLiveSyncTask(TASK_SLOW_MODEL, initTask, initResult);
        initLiveSyncTask(TASK_SLOW_MODEL_IMPRECISE, initTask, initResult);
        initLiveSyncTask(TASK_BATCHED, initTask, initResult);

        addObject(((TestObject.FileBasedTestObjectSource) (TASK_BATCHED_IMPRECISE.source)).getFile(), initTask, initResult,
                workerThreadsCustomizer(getWorkerThreads()));
        // Starting this task results in (expected) exception

        initLiveSyncTask(TASK_ERROR, initTask, initResult);
        initLiveSyncTask(TASK_ERROR_IMPRECISE, initTask, initResult);
        initLiveSyncTask(TASK_DRY_RUN, initTask, initResult);
        initLiveSyncTask(TASK_DRY_RUN_WITH_UPDATE, initTask, initResult);

        initLiveSyncTask(TASK_NO_POLICY, initTask, initResult);
        initLiveSyncTask(TASK_XFER1, initTask, initResult);
        initLiveSyncTask(TASK_XFER2, initTask, initResult);
        initLiveSyncTask(TASK_MULTI_CHANGES, initTask, initResult);

        initLiveSyncTask(TASK_ERRORS_PRECISE_IGNORE, initTask, initResult);
        initLiveSyncTask(TASK_ERRORS_PRECISE_IGNORE_PARTIAL_STOP_ON_FATAL, initTask, initResult);
        initLiveSyncTask(TASK_ERRORS_PRECISE_STOP_ON_ANY, initTask, initResult);
        initLiveSyncTask(TASK_ERRORS_PRECISE_RETRY_LATER_ON_ANY, initTask, initResult);
        initLiveSyncTask(TASK_ERRORS_PRECISE_RETRY_LATER_MAX_4, initTask, initResult);

        addObject(CommonTasks.TASK_TRIGGER_SCANNER_ON_DEMAND, initTask, initResult);

        assertUsers(getNumberOfUsers());
        for (int i = 0; i < USERS; i++) {
            interruptedSyncResource.getController().addAccount(getUserName(i, true));
            interruptedSyncImpreciseResource.getController().addAccount(getUserName(i, false));
        }

        RESOURCE_DUMMY_MULTI_TYPE_SOURCE.initAndTest(this, initTask, initResult);
        initTestObjects(initTask, initResult,
                TASK_MULTI_TYPE_LIVE_SYNC_TYPE_1,
                TASK_MULTI_TYPE_LIVE_SYNC_TYPE_2);

        // Set the sync tokens
        TASK_MULTI_TYPE_LIVE_SYNC_TYPE_1.rerun(initResult);
        TASK_MULTI_TYPE_LIVE_SYNC_TYPE_2.rerun(initResult);

//        setGlobalTracingOverride(createModelLoggingTracingProfile());
    }

    private void initLiveSyncTask(TestObject<TaskType> testResource, Task initTask, OperationResult initResult)
            throws java.io.IOException, CommonException {
        // FIXME remove this hack
        File taskFile = ((TestObject.FileBasedTestObjectSource) (testResource.source)).getFile();
        PrismObject<TaskType> task = addObject(taskFile, initTask, initResult, workerThreadsCustomizer(getWorkerThreads()));
        if (!TaskTypeUtil.isTaskRecurring(task.asObjectable())) {
            waitForTaskFinish(testResource.oid);
        } else {
            rerunTask(testResource.oid, initResult);
        }
    }

    int getWorkerThreads() {
        return 0;
    }

    private int getProcessingThreads() {
        int workerThreads = getWorkerThreads();
        return workerThreads > 0 ? workerThreads : 1;
    }

    private String getUserName(int i, boolean precise) {
        return String.format("%s%06d", precise ? USER_P : USER_I, i);
    }

    /**
     * Original meaning of this test was:
     * Suspends LiveSync task in the first stage when it gathers changes via ICF Sync operation.
     * Token should be 0, because nothing was processed yet (regardless of precise/imprecise token handling).
     *
     * However, now the live sync processing is iterative: changes are fetched and then applied. So this makes no difference.
     * Nevertheless, it might be useful to test suspension in early stages of task run.
     *
     * When dealing with precise resources, the suspension must come before first change is fetched. So we set the delay
     * to 2 seconds.
     */
    @Test
    public void test100SuspendWhileIcfSync() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Resource gives out changes slowly now.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(2000);

        when();
        waitForTaskNextStart(TASK_SLOW_RESOURCE.oid, 2000, true); // starts the task
        boolean suspended = suspendTask(TASK_SLOW_RESOURCE.oid, 10000);

        then();
        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_RESOURCE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertEquals("Wrong token value", (Integer) 0, getIntToken(taskAfter));
        assertThat(taskAfter.getObjectOid())
                .as("object OID")
                .isEqualTo(DummyInterruptedSyncResource.OID);
    }

    /**
     * The same as test100.
     * The delay can be smaller, as even if some changes are fetched and processed, the token will not be updated.
     */
    @Test
    public void test105SuspendWhileIcfSyncImprecise() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Resource gives out changes slowly now.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(500);

        when();
        waitForTaskNextStart(TASK_SLOW_RESOURCE_IMPRECISE.oid, 2000, true);  // starts the task
        boolean suspended = suspendTask(TASK_SLOW_RESOURCE_IMPRECISE.oid, 5000);

        then();
        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_RESOURCE_IMPRECISE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertEquals("Wrong token value", (Integer) 0, getIntToken(taskAfter));
    }

    private Integer getIntToken(Task task) throws SchemaException {
        return (Integer) getRootSyncTokenRealValueRequired(task.getRawTaskObjectClonedIfNecessary().asObjectable());
    }

    /**
     * Original meaning of this test was:
     * Suspends LiveSync task in the second stage when changes are being processed.
     * For precise token providing resource the token should correspond to objects that were actually processed.
     *
     * Now, when the processing is iterative, we simply suspend the task during iterative processing of changes.
     * The result should be the same.
     */
    @Test
    public void test110SuspendWhileProcessing() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Resource gives out changes quickly. But they are processed slowly.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 100;

        Set<String> threads = ConcurrentHashMap.newKeySet();
        DummyInterruptedSyncResource.setExecutionListener(() -> threads.add(Thread.currentThread().getName()));

        when();
        waitForTaskNextStart(TASK_SLOW_MODEL.oid, 2000, true);  // starts the task
        Thread.sleep(4000);
        boolean suspended = suspendTask(TASK_SLOW_MODEL.oid, 5000);

        then();
        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_MODEL.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        Integer token = getIntToken(taskAfter);
        // If we are particularly unfortunate the token value could be zero in multithreaded scenario:
        // This could occur if the first sync delta is processed only after the second, third, etc.
        // If this happens in reality, we need to adapt the assertion here.
        assertTrue("Token value is zero (should be greater)", token > 0);

        int progress = (int) taskAfter.getLegacyProgress();
        displayValue("Token value", token);
        displayValue("Task progress", progress);
        if (getWorkerThreads() <= 1) {
            assertEquals("Wrong task progress", token, (Integer) progress);
            assertObjects(UserType.class, getStartsWithQuery(USER_P), progress);
        } else {
            // TODO
//            assertTrue("Token is too high: " + token + ", while progress is " + progress,
//                    token <= progress);
//            assertObjects(UserType.class, getStartsWithQuery(USER_P), progress);
        }

        DummyInterruptedSyncResource.setExecutionListener(null);
        displayValue("threads", threads);
        assertThat(threads).as("threads").hasSize(getProcessingThreads());
    }

    /**
     * Original meaning of this test was:
     * Suspends LiveSync task in the second stage when changes are being processed.
     * For imprecise token providing resource the token should stay unchanged, i.e. here at 0. (MID-5513)
     *
     * Now, when the processing is iterative, we simply suspend the task during iterative processing of changes.
     * The result should be the same.
     */
    @Test
    public void test115SuspendWhileProcessingImprecise() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Resource gives out changes quickly. But they are processed slowly.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 100;

        when();
        waitForTaskNextStart(TASK_SLOW_MODEL_IMPRECISE.oid, 2000, true);  // starts the task
        Thread.sleep(4000);
        boolean suspended = suspendTask(TASK_SLOW_MODEL_IMPRECISE.oid, 5000);

        then();
        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_MODEL_IMPRECISE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);

        Integer token = getIntToken(taskAfter);
        displayValue("Token value", token);
        assertEquals("Wrong token value", (Integer) 0, token);

        int progress = (int) taskAfter.getLegacyProgress();
        displayValue("Task progress", progress);

        if (getWorkerThreads() <= 1) {
            assertObjects(UserType.class, getStartsWithQuery(USER_I), progress);
        } else {
            // TODO
        }
    }

    /**
     * Batched operation with precise token values. Should fetch exactly 10 records in the first sync run
     * and 10 records in the second sync run.
     */
    @Test
    public void test120Batched() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally. But we will take only first 10 of them.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = getUserName(24, true);

        when();
        waitForTaskNextRun(TASK_BATCHED.oid, 20_000, true);

        then();
        stabilize();
        Task taskAfter = taskManager.getTaskWithResult(TASK_BATCHED.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        Integer token = getIntToken(taskAfter);
        assertEquals("Wrong token value", (Integer) 10, token);

        assertObjects(UserType.class, query, 10);

        when();
        waitForTaskNextRun(TASK_BATCHED.oid, 20_000, true);

        then();
        stabilize();
        taskAfter = taskManager.getTaskWithResult(TASK_BATCHED.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        token = getIntToken(taskAfter);
        assertEquals("Wrong token value", (Integer) 20, token);

        assertObjects(UserType.class, query, 20);

        when("(with error)");
        waitForTaskNextRun(TASK_BATCHED.oid, 20_000, true);

        then("(with error)");
        stabilize();
        taskAfter = taskManager.getTaskWithResult(TASK_BATCHED.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertPartialError(taskAfter.getResult());          // error was "skippable" (retryLiveSyncErrors = false)

        token = getIntToken(taskAfter);
        assertEquals("Wrong token value", (Integer) 30, token);     // therefore we should go on

        assertObjects(UserType.class, query, 29);       // and all records should be imported
    }

    /**
     * Batched operation with imprecise token values. This should not be allowed.
     */
    @Test
    public void test125BatchedImprecise() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Changes are provided and processed normally. But we will take only first 10 of them.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 0;

        when();
        try {
            waitForTaskNextRun(TASK_BATCHED_IMPRECISE.oid, 10000, true);
        } catch (Throwable t) {
            suspendTask(TASK_BATCHED_IMPRECISE.oid, 10000);
            throw t;
        }

        then();
        stabilize();
        Task taskAfter = taskManager.getTaskWithResult(TASK_BATCHED_IMPRECISE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertFailure(taskAfter.getResult());
        assertEquals("Wrong task state", TaskExecutionStateType.SUSPENDED, taskAfter.getExecutionState());
    }

    /**
     * Errored operation or R-th object (i.e. ERROR_ON+1) with precise token values.
     *
     * Single-threaded:
     *
     * - Should process exactly R records in the first sync run and stop.
     *
     * Multi-threaded:
     *
     * - Should process approximately R records (might be more, might be less) and stop.
     *
     * Both:
     *
     * - Token should point to record R-1 (=ERROR_ON), so R is fetched next.
     */
    @Test
    public void test130Error() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = getUserName(ERROR_ON, true);

        when();
        waitForTaskNextRun(TASK_ERROR.oid, 30000, true);

        then();
        stabilize();
        TaskType taskAfter = assertTask(TASK_ERROR.oid, "1st")
                .display()
                .assertSuspended()
                .assertFatalError() // Task was instructed to stop, so this means FATAL_ERROR.
                .getObjectable();

        Integer token = (Integer) getRootSyncTokenRealValueRequired(taskAfter);
        assertEquals("Wrong token value", (Integer) ERROR_ON, token);

        if (getWorkerThreads() <= 1) {
            assertObjects(UserType.class, query, ERROR_ON);     // 0..ERROR_ON-1
        }

        // Another run - should fail the same

        when();
        waitForTaskNextRun(TASK_ERROR.oid, 10000, true);

        then();
        stabilize();
        taskAfter = assertTask(TASK_ERROR.oid, "1st")
                .display()
                .assertSuspended()
                .assertFatalError() // Task was instructed to stop, so this means FATAL_ERROR.
                .getObjectable();

        token = (Integer) getRootSyncTokenRealValueRequired(taskAfter);
        assertEquals("Wrong token value", (Integer) ERROR_ON, token);

        if (getWorkerThreads() <= 1) {
            assertObjects(UserType.class, query, ERROR_ON);
        }
    }

    /**
     * Errored operation or R-th object (i.e. ERROR_ON+1) with imprecise token values.
     * Should process exactly R records in the first sync run and stop. Token should point to the original value (0 in this case).
     */
    @Test
    public void test135ErrorImprecise() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 0;
        DummyInterruptedSyncImpreciseResource.errorOn = getUserName(ERROR_ON, false);

        when();
        try {
            waitForTaskNextRun(TASK_ERROR_IMPRECISE.oid, 30000, true);
        } catch (Throwable t) {
            suspendTask(TASK_ERROR_IMPRECISE.oid, 10000);
            throw t;
        }

        then();
        stabilize();
        TaskType taskAfter = assertTask(TASK_ERROR_IMPRECISE.oid, "1st")
                .display()
                .assertSuspended()
                .assertFatalError() // Task was instructed to stop, so this means FATAL_ERROR.
                .getObject().asObjectable();

        Integer token = (Integer) getRootSyncTokenRealValueRequired(taskAfter);
        assertEquals("Wrong token value", (Integer) 0, token);

        if (getWorkerThreads() <= 1) {
            verbose = true;
            assertObjects(UserType.class, query, ERROR_ON);     // 0..ERROR_ON-1
            verbose = false;
        }

        // Another run - should fail the same

        when();
        waitForTaskNextRun(TASK_ERROR_IMPRECISE.oid, 10000, true);

        then();
        stabilize();
        assertTask(TASK_ERROR_IMPRECISE.oid, "2nd")
                .display()
                .assertSuspended()
                .assertFatalError(); // Task was instructed to stop, so this means FATAL_ERROR.

        if (getWorkerThreads() <= 1) {
            assertObjects(UserType.class, query, ERROR_ON);
        }
    }

    /**
     * Dry run. Should process all records, but create no users and not update the token.
     */
    @Test
    public void test140DryRun() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = null;

        when();
        waitForTaskNextRun(TASK_DRY_RUN.oid, 10_000, true);

        then();
        stabilize();
        Task taskAfter = taskManager.getTaskWithResult(TASK_DRY_RUN.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertSuccess(taskAfter.getResult());
        assertTaskClosed(taskAfter);

        Integer token = getIntToken(taskAfter);
        assertEquals("Wrong token value", (Integer) 0, token);

        assertObjects(UserType.class, query, 0);
    }

    /**
     * Dry run with update. Should process all records, but create no users and then update the token.
     */
    @Test
    public void test150DryRunWithUpdate() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = null;

        when();
        waitForTaskNextRun(TASK_DRY_RUN_WITH_UPDATE.oid, 10_000, true);

        then();
        stabilize();
        Task taskAfter = taskManager.getTaskWithResult(TASK_DRY_RUN_WITH_UPDATE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertSuccess(taskAfter.getResult());
        assertTaskClosed(taskAfter);

        Integer token = getIntToken(taskAfter);
        assertEquals("Wrong token value", (Integer) USERS, token);

        assertObjects(UserType.class, query, 0);
    }

    /**
     * Live sync processing resource object with no synchronization policy (MID-5999)
     */
    @Test
    public void test200NoPolicy() throws Exception {

        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Task noPolicyBefore = taskManager.getTaskPlain(TASK_NO_POLICY.oid, result);
        display("Task before", noPolicyBefore);

        RESOURCE_DUMMY_NO_POLICY.controller.addAccount("no-policy-user");

        when();

        waitForTaskNextRun(TASK_NO_POLICY.oid, 10000, true);

        then();

        stabilize();
        Task taskAfter = taskManager.getTaskWithResult(TASK_NO_POLICY.oid, result);
        display("Task after", taskAfter);
//        ActivitySynchronizationStatisticsType syncInfo = taskAfter.getStoredOperationStatsOrClone().getSynchronizationInformation();
//        displayValue("Sync info", ActivitySynchronizationStatisticsUtil.format(syncInfo));
        assertSuccess(taskAfter.getResult());
        assertTaskClosed(taskAfter);

        assertSyncToken(taskAfter, 1);
//        assertEquals("Wrong noSyncPolicy counter value", 1, (Object) syncInfo.getCountNoSynchronizationPolicy());
//        assertEquals("Wrong noSyncPolicyAfter counter value", 1, (Object) syncInfo.getCountNoSynchronizationPolicyAfter());
    }

    /*
     * Transfer tests: These are created in order to verify "actions executed" counters (MID-5920).
     * There are two transfers:
     *
     * xfer1-source --> xfer1-target-deletable
     * xfer2-source --> xfer2-target-not-deletable
     *
     * In both cases accounts are live synced from the source to repository and then provisioned to the target resource.
     * After initial provisioning of XFER_ACCOUNTS (10) accounts the accounts are renamed, which has an effect that the target
     * accounts are attempted to be deleted. This is implemented in xfer1 but not in xfer2, resulting in (expected) failures.
     *
     * So, test210+test215 do the initial synchronization of accounts.
     * Then, test220+test225 do the renaming and live syncing changed accounts.
     * And test230+test235 repeat the live sync. (Doing nothing in xfer1 case and retrying the failed record in xfer2 case.)
     */

    /**
     * Initial synchronization of accounts from Xfer1 Source (setting the stage for MID-5920 tests).
     */
    @Test
    public void test210Xfer1InitialSync() throws Exception {
        RESOURCE_DUMMY_XFER1_TARGET_DELETABLE.controller.getDummyResource().setOperationDelayOffset(500);
        doXferInitialSync(1, TASK_XFER1, RESOURCE_DUMMY_XFER1_SOURCE);
    }

    /**
     * Initial synchronization of accounts from Xfer2 Source (setting the stage for MID-5920 tests).
     */
    @Test
    public void test215Xfer2InitialSync() throws Exception {
        doXferInitialSync(2, TASK_XFER2, RESOURCE_DUMMY_XFER2_SOURCE);
    }

    private void doXferInitialSync(int index, TestObject<TaskType> xferTask, DummyTestResource xferSource) throws Exception {
        given();

        // @formatter:off
        assertTask(xferTask.oid, "before")
                .rootActivityState()
                    .progress()
                        .display()
                    .end()
                    .itemProcessingStatistics()
                        .display()
                    .end()
                    .synchronizationStatistics()
                    .display()
                        //.assertTotal(0, 0)
                    .end()
                    .actionsExecuted()
                        .display()
                        .assertEmpty()
                    .end();
        // @formatter:on

        for (int i = 0; i < XFER_ACCOUNTS; i++) {
            String name = String.format("xfer%d-%04d", index, i);
            xferSource.controller.addAccount(name, name);
        }

        when();

        waitForTaskNextRun(xferTask.oid, 60000, true);

        then();

        stabilize();
        // @formatter:off
        assertTask(xferTask.oid, "after")
                .rootActivityState()
                    .progress()
                        .display()
                    .end()
                    .itemProcessingStatistics()
                        .display()
                    .end()
                    .synchronizationStatistics()
                        .display()
//                    .assertTotal(XFER_ACCOUNTS, XFER_ACCOUNTS)
//                    .assertUnmatched(XFER_ACCOUNTS, 0)
//                    .assertLinked(0, XFER_ACCOUNTS)
                    .end()
                    .actionsExecuted()
                        .display()
                        .resulting()
                            .assertCount(3*XFER_ACCOUNTS, 0)
                            .assertCount(ADD, UserType.COMPLEX_TYPE, XFER_ACCOUNTS, 0)
                            .assertCount(ADD, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0)
                            .assertCount(MODIFY, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0)
                        .end()
                    .end()
                .end()
                .assertClosed()
                .assertSuccess();
        // @formatter:on
    }

    /**
     * Renaming source accounts that results in target accounts deletion - xfer1 (still MID-5920).
     *
     * Accounts are renamed on the source, causing their deletion on the target.
     * These deletions proceed successfully, as xfer1-target has DELETE capability enabled.
     */
    @Test
    public void test220Xfer1RenameAccounts() throws Exception {
        TaskAsserter<Void> asserter = doXferRenameAndSync(TASK_XFER1, RESOURCE_DUMMY_XFER1_SOURCE);
        assertXfer1StateAfterRename(asserter);
    }

    private void assertXfer1StateAfterRename(TaskAsserter<Void> asserter) {
        // @formatter:off
        asserter
                .assertClosed()
                .assertSuccess()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                    .end()
                    .synchronizationStatistics()
                        .display()
//                    .assertTotal(2*XFER_ACCOUNTS, 2*XFER_ACCOUNTS) // each account was touched twice
//                    .assertUnmatched(XFER_ACCOUNTS, 0) // this is information from the first run
//                    .assertLinked(XFER_ACCOUNTS, 2*XFER_ACCOUNTS) // this is combined from the first and second runs
                    .end()
                    .actionsExecuted()
                        .display()
                        .resulting()
                            .assertCount(6*XFER_ACCOUNTS, 0)
                            .assertCount(ADD, UserType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
                            .assertCount(ADD, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
                            .assertCount(MODIFY, ShadowType.COMPLEX_TYPE, 2*XFER_ACCOUNTS, 0) // from the first+second runs
                            .assertCount(MODIFY, UserType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the second runs
                            .assertCount(DELETE, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the second run
                        .end()
                    .end();
        // @formatter:on
    }

    /**
     * Renaming source accounts that results in target accounts deletion - xfer2 (still MID-5920).
     *
     * Accounts are renamed on the source, causing their deletion on the target.
     * These deletions fail, as xfer2-target has DELETE capability disabled.
     */
    @Test
    public void test225Xfer2RenameAccounts() throws Exception {
        int t = getWorkerThreads() > 0 ? getWorkerThreads() : 1;
        // @formatter:off
        doXferRenameAndSync(TASK_XFER2, RESOURCE_DUMMY_XFER2_SOURCE)
                .assertSuspended()
                .assertFatalError()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                    .end()
                    .synchronizationStatistics()
                        .display()
//                    .assertTotal(XFER_ACCOUNTS+t, XFER_ACCOUNTS+t) // XFER_ACCOUNTS from the first run, t from the second (failed immediately)
//                    .assertUnmatched(XFER_ACCOUNTS, 0) // from the first run
//                    .assertLinked(t, XFER_ACCOUNTS+t) // 1 from the second run
                    .end()
                    .actionsExecuted()
                        .display()
                        .resulting()
                            .assertCount(ADD, UserType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
                            .assertCount(ADD, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
                            .assertSuccessCount(MODIFY, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS+1, XFER_ACCOUNTS+t) // from the first+second runs
                            .assertFailureCount(MODIFY, ShadowType.COMPLEX_TYPE, 0, 0) // from the first+second runs
                            .assertSuccessCount(MODIFY, UserType.COMPLEX_TYPE, 1, t) // from the second runs
                            .assertFailureCount(MODIFY, UserType.COMPLEX_TYPE, 0, 0) // from the second runs
                            .assertSuccessCount(DELETE, ShadowType.COMPLEX_TYPE, 0, 0) // from the second run
                            .assertFailureCount(DELETE, ShadowType.COMPLEX_TYPE, 1, t) // from the second run
                            .end()
                        .end();
        // @formatter:on
    }

    private TaskAsserter<Void> doXferRenameAndSync(TestObject<TaskType> xferTask, DummyTestResource xferSource) throws Exception {
        given();

        // @formatter:off
        assertTask(xferTask.oid, "before")
                .rootActivityState()
                    .itemProcessingStatistics()
                        .display()
                    .end()
                    .synchronizationStatistics()
                        .display()
                    .end()
                    .actionsExecuted()
                        .display()
                    .end();
        // @formatter:on

        DummyResource resource = xferSource.controller.getDummyResource();
        for (DummyAccount account : new ArrayList<>(resource.listAccounts())) {
            account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "_" + account.getName());
        }

        when();

        waitForTaskNextRun(xferTask.oid, 60000, true);

        then();

        stabilize();
        return assertTask(xferTask.oid, "after");
    }

    /**
     * Repeated live sync for xfer1: MID-5920.
     *
     * As all changes are processed, counters should stay unchanged.
     */
    @Test
    public void test230Xfer1RepeatedLiveSync() throws Exception {
        TaskAsserter<Void> asserter = doXferLiveSync(TASK_XFER1);
        assertXfer1StateAfterRename(asserter); // the state is exactly the same as after previous live sync
    }

    /**
     * Repeated live sync for xfer2: MID-5920.
     *
     * The failed record is synced again, resulting in increase of failure counters.
     */
    @Test
    public void test235Xfer2RepeatedLiveSync() throws Exception {
        // @formatter:off
        if (getWorkerThreads() > 0) {
            doXferLiveSync(TASK_XFER2)
                    .assertSuspended()
                    .assertFatalError()
                    .rootActivityState()
                        .itemProcessingStatistics().display().end()
                        .synchronizationStatistics().display().end()
                        .actionsExecuted().display().end();
            // No special asserts here. The number of accounts being processed may depend on the timing.
        } else {
            doXferLiveSync(TASK_XFER2)
                    .assertSuspended()
                    .assertFatalError()
                    .rootActivityState()
                        .itemProcessingStatistics().display().end()
                        .synchronizationStatistics()
                            .display()
//                            .assertTotal(XFER_ACCOUNTS+3, XFER_ACCOUNTS+3) // XFER_ACCOUNTS from the first run, 1 from the second (failed immediately), 2 for the third (first ok, second fails)
//                            .assertUnmatched(XFER_ACCOUNTS, 0) // from the first run
//                            .assertLinked(3, XFER_ACCOUNTS+3) // 1 from the second run, 2 from the third
                        .end()
                        .actionsExecuted()
                            .display()
                            .resulting()
                    // TODO FIXME
//                                .assertCount(3*XFER_ACCOUNTS+5, 2)
//                                .assertCount(ADD, UserType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
//                                .assertCount(ADD, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
//                                .assertCount(MODIFY, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS+3, 0) // from the first+second+third (10+1+2) runs
//                                .assertCount(MODIFY, UserType.COMPLEX_TYPE, 2, 0) // from the second+third runs (1+1)
//                                .assertCount(DELETE, ShadowType.COMPLEX_TYPE, 0, 2) // from the second+third run (1+1)
                            .end()
                        .end();
            // @formatter:on
        }

        // Note: it seems that the failed delete caused unlinking the account, so the next live sync on the "11-th" account
        // proceeds without problems.
    }

    /**
     * Tests for affinity controller functionality.
     *
     * This component caused (most probably) the lockup when multiple changes for given account were processed.
     * See MID-6248.
     */
    @Test
    public void test240TestAffinityController() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Task taskBefore = taskManager.getTaskPlain(TASK_MULTI_CHANGES.oid, result);
        display("Task before", taskBefore);

        int interests = 10;

        DummyAccount annAccount = RESOURCE_DUMMY_MULTI_CHANGES.controller.addAccount("ann");
        for (int i = 0; i < interests; i++) {
            annAccount.addAttributeValue(DummyAccount.ATTR_INTERESTS_NAME, String.valueOf(i));
        }

        SequenceChecker.INSTANCE.reset();

        when();

        waitForTaskNextRun(TASK_MULTI_CHANGES.oid, 30000, true);

        then();

        stabilize();
        Task taskAfter = taskManager.getTaskWithResult(TASK_MULTI_CHANGES.oid, result);
        display("Task after", taskAfter);
//        ActivitySynchronizationStatisticsType syncInfo = taskAfter.getStoredOperationStatsOrClone().getSynchronizationInformation();
//        displayValue("Sync info", ActivitySynchronizationStatisticsUtil.format(syncInfo));
        assertSuccess(taskAfter.getResult());
        assertTaskClosed(taskAfter);
    }

    private TaskAsserter<Void> doXferLiveSync(TestObject<TaskType> xferTask) throws Exception {
        given();

        // @formatter:off
        assertTask(xferTask.oid, "before")
                .rootActivityState()
                    .itemProcessingStatistics().display().end()
                    .synchronizationStatistics().display().end()
                    .actionsExecuted().display().end();
        // @formatter:on

        when();

        waitForTaskNextRun(xferTask.oid, 60000, true);

        then();

        stabilize();
        return assertTask(xferTask.oid, "after");
    }

    @Test
    public void test300ErrorsPreciseIgnore() throws Exception {
        given();
        prepareErrorsScenario(RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE, RESOURCE_DUMMY_ERRORS_TARGET);

        when();
        waitForTaskNextRun(TASK_ERRORS_PRECISE_IGNORE.oid, 60000, true);

        then();
        stabilize();
        assertTask(TASK_ERRORS_PRECISE_IGNORE.oid, "after")
                .display()
                .assertProgress(ERRORS_ACCOUNTS)
                .assertToken(ERRORS_ACCOUNTS);
    }

    @Test
    public void test310ErrorsPreciseIgnorePartialStopOnFatal() throws Exception {
        given();
        prepareErrorsScenario(RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE, RESOURCE_DUMMY_ERRORS_TARGET);

        when();
        waitForTaskNextRun(TASK_ERRORS_PRECISE_IGNORE_PARTIAL_STOP_ON_FATAL.oid, 600000 /* TODO */, true);

        then();
        stabilize();
        assertTask(TASK_ERRORS_PRECISE_IGNORE_PARTIAL_STOP_ON_FATAL.oid, "after")
                .display()
                .assertToken(8); // won't assert process because of multithreaded case
    }

    @Test
    public void test320ErrorsPreciseStopOnAny() throws Exception {
        given();
        prepareErrorsScenario(RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE, RESOURCE_DUMMY_ERRORS_TARGET);

        when();
        waitForTaskNextRun(TASK_ERRORS_PRECISE_STOP_ON_ANY.oid, 60000, true);

        then();
        stabilize();
        assertTask(TASK_ERRORS_PRECISE_STOP_ON_ANY.oid, "after")
                .display()
                .assertToken(2); // won't assert process because of multithreaded case
    }

    @Test
    public void test330ErrorsPreciseRetryLaterOnAny() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareErrorsScenario(RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE, RESOURCE_DUMMY_ERRORS_TARGET);

        when();
        long start = System.currentTimeMillis();
        waitForTaskNextRun(TASK_ERRORS_PRECISE_RETRY_LATER_ON_ANY.oid, 600000 /* TODO */, true);
        long end = System.currentTimeMillis();

        then();
        stabilize();
        assertTask(TASK_ERRORS_PRECISE_RETRY_LATER_ON_ANY.oid, "after")
                .display()
                .assertProgress(30)
                .assertToken(30);

        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE.oid, null, task, result);
        PrismObject<ResourceType> targetResource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_ERRORS_TARGET.oid, null, task, result);
        // @formatter:off
        //noinspection unchecked
        assertShadow("e-000003", resource)
                .display()
                .triggers()
                    .single()
                    .assertHandlerUri(ShadowReconcileTriggerHandler.HANDLER_URI)
                    .assertTimestampBetween(fromNow(start, "PT5M"), fromNow(end, "PT5M"))
                    .extension()
                        .assertItemsExactly(SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT)
                        .containerSingle(SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_NUMBER, 1)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_LIMIT, 3)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_INTERVAL, XmlTypeConverter.createDuration("PT1H"));

        //noinspection unchecked
        assertShadow("e-000009", resource)
                .display()
                .triggers()
                    .single()
                    .assertHandlerUri(ShadowReconcileTriggerHandler.HANDLER_URI)
                    .assertTimestampBetween(fromNow(start, "PT30M"), fromNow(end, "PT30M"))
                    .extension()
                        .assertItemsExactly(SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT)
                        .containerSingle(SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_NUMBER, 1)
                            .assertNoItem(PlannedOperationAttemptType.F_LIMIT)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_INTERVAL, XmlTypeConverter.createDuration("PT1H"));
        // @formatter:on
        assertNoShadow("e-000009", targetResource, result);

        when("retrying partial errors (each 3th except for 9th)");

        clock.overrideDuration("PT5M");
        long triggerFirstStart = System.currentTimeMillis();
        runTriggerScannerOnDemandErrorsOk(result);
        long triggerFirstEnd = System.currentTimeMillis();

        then("retrying partial errors (each 3th except for 9th)");
        stabilize();
        // @formatter:off
        assertTask(CommonTasks.TASK_TRIGGER_SCANNER_ON_DEMAND.oid, "after")
                .display()
                .rootItemProcessingInformation()
                    .display()
                    .assertSuccessCount(0)
                    .assertFailureCount(7)
                    .end()
                .assertProgress(7); // 3, 6, 12, 15, 21, 24, 30

        //noinspection unchecked
        assertShadow("e-000003", resource)
                .display()
                .triggers()
                    .single()
                    .assertHandlerUri(ShadowReconcileTriggerHandler.HANDLER_URI)
                    .assertTimestampBetween(fromNow(triggerFirstStart, "PT1H"), fromNow(triggerFirstEnd, "PT1H")) // from current or from clock.current (overridden)?
                    .extension()
                        .assertItemsExactly(SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT)
                        .containerSingle(SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_NUMBER, 2)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_LIMIT, 3)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_INTERVAL, XmlTypeConverter.createDuration("PT1H"));
        // @formatter:on

        when("retrying fatal errors (each 9th)");

        clock.resetOverride();
        clock.overrideDuration("PT30M");
        long triggerSecondStart = System.currentTimeMillis();
        runTriggerScannerOnDemandErrorsOk(result);
        long triggerSecondEnd = System.currentTimeMillis();

        then("retrying fatal errors (each 9th)");
        stabilize();
        // @formatter:off
        assertTask(CommonTasks.TASK_TRIGGER_SCANNER_ON_DEMAND.oid, "after")
                .assertProgress(7 + 3) // 9, 18, 27
                .display()
                .rootItemProcessingInformation()
                    .assertSuccessCount(0)
                    .assertFailureCount(10) // counters are not cleared between runs (for now)
                    .display();

        //noinspection unchecked
        assertShadow("e-000009", resource)
                .display()
                .triggers()
                    .single()
                    .assertHandlerUri(ShadowReconcileTriggerHandler.HANDLER_URI)
                    .assertTimestampBetween(fromNow(triggerSecondStart, "PT1H"), fromNow(triggerSecondEnd, "PT1H")) // from current or from clock.current (overridden)?
                    .extension()
                        .assertItemsExactly(SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT)
                        .containerSingle(SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_NUMBER, 2)
                            .assertPropertyEquals(PlannedOperationAttemptType.F_INTERVAL, XmlTypeConverter.createDuration("PT1H"));
        // @formatter:on
        assertNoShadow("e-000009", targetResource, result);

        when("retrying everything, errors turned off");
        produceErrors = false;
        try {
            clock.resetOverride();
            clock.overrideDuration("P1D");
            runTriggerScannerOnDemand(result);
        } finally {
            produceErrors = true;
        }

        then("retrying everything, errors turned off");
        stabilize();
        // @formatter:off
        assertTask(CommonTasks.TASK_TRIGGER_SCANNER_ON_DEMAND.oid, "after")
                .assertProgress(7 + 3 + 10) // each 3rd
                .display()
                .rootActivityState()
                    .itemProcessingStatistics()
                        .assertSuccessCount(10)
                        .assertFailureCount(10) // counters are not cleared between runs (for now)
                        .display();
        // @formatter:on

        for (int i = 3; i <= 30; i += 3) {
            assertShadow(String.format("e-%06d", i), resource)
                    .display()
                    .assertNoTrigger();
            assertShadow(String.format("e-%06d", i), targetResource)
                    .display()
                    .assertLive();
            //.assertLifecycleState(null); // For many account we have here "proposed" ... why?!
        }
    }

    @Test
    public void test340ErrorsPreciseRetryLaterMax4() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();
        prepareErrorsScenario(RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE, RESOURCE_DUMMY_ERRORS_TARGET);

        when();
        waitForTaskNextRun(TASK_ERRORS_PRECISE_RETRY_LATER_MAX_4.oid, 600000 /* TODO */, true);

        then();
        stabilize();
        assertTask(TASK_ERRORS_PRECISE_RETRY_LATER_MAX_4.oid, "after")
                .display()
                .assertToken(11) // won't assert progress because of multithreaded case
                .assertSchedulingState(TaskSchedulingStateType.SUSPENDED);

        PrismObject<ResourceType> resource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_ERRORS_SOURCE_PRECISE.oid, null, task, result);
        PrismObject<ResourceType> targetResource = modelService.getObject(ResourceType.class, RESOURCE_DUMMY_ERRORS_TARGET.oid, null, task, result);

        // @formatter:off
        assertShadow("e-000003", resource)
                .display()
                .triggers()
                .single()
                    .assertHandlerUri(ShadowReconcileTriggerHandler.HANDLER_URI);

        assertShadow("e-000009", resource)
                .display()
                .triggers()
                .single()
                    .assertHandlerUri(ShadowReconcileTriggerHandler.HANDLER_URI);
        // @formatter:on

        assertNoShadow("e-000009", targetResource, result);
    }

    /**
     * MID-8537
     */
    @Test
    public void test400SyncIndividualIntents() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("some accounts on test resource");
        // Created this way to provide a single ADD operation, not ADD+MODIFY
        DummyAccount firstAccount = new DummyAccount("first");
        firstAccount.addAttributeValue(ATTR_TYPE, "type1");
        DummyAccount secondAccount = new DummyAccount("second");
        secondAccount.addAttributeValue(ATTR_TYPE, "type2");
        RESOURCE_DUMMY_MULTI_TYPE_SOURCE.getDummyResource().addAccount(firstAccount);
        RESOURCE_DUMMY_MULTI_TYPE_SOURCE.getDummyResource().addAccount(secondAccount);

        when("first LS task is run");
        TASK_MULTI_TYPE_LIVE_SYNC_TYPE_1.rerun(result);

        then("single object processed, single user created");
        TASK_MULTI_TYPE_LIVE_SYNC_TYPE_1.assertAfter()
                .rootActivityState()
                .progress()
                .assertCommitted(1, 0, 1)
                .end()
                .synchronizationStatistics()
                .display()
                .assertTransition(null, UNMATCHED, LINKED, null, 1, 0, 0)
                .assertTransition(null, null, null, NOT_APPLICABLE_FOR_TASK, 0, 0, 1);

        UserType firstUserAfter = assertUserByUsername("first", "after")
                .display()
                .getObjectable();
        assertNoUserByUsername("second");

        when("first user and his shadow is deleted and the second task is run");
        // doing via repository, to keep the resource object intact
        repositoryService.deleteObject(ShadowType.class, firstUserAfter.getLinkRef().iterator().next().getOid(), result);
        repositoryService.deleteObject(UserType.class, firstUserAfter.getOid(), result);
        TASK_MULTI_TYPE_LIVE_SYNC_TYPE_2.rerun(result);

        then("single object processed, single user created");
        TASK_MULTI_TYPE_LIVE_SYNC_TYPE_1.assertAfter()
                .rootActivityState()
                .progress()
                .assertCommitted(1, 0, 1)
                .end()
                .synchronizationStatistics()
                .display()
                .assertTransition(null, UNMATCHED, LINKED, null, 1, 0, 0)
                .assertTransition(null, null, null, NOT_APPLICABLE_FOR_TASK, 0, 0, 1);

        assertUserByUsername("second", "after")
                .display()
                .getOid();
        assertNoUserByUsername("first");
    }

    @SuppressWarnings("SameParameterValue")
    private void prepareErrorsScenario(DummyTestResource source, DummyTestResource target) throws ConnectException,
            ObjectAlreadyExistsException, ConflictException, FileNotFoundException, SchemaViolationException,
            InterruptedException, SchemaException, ObjectNotFoundException {
        deleteEUsers();
        deleteEShadows();
        recreateSourceEAccounts(source);
        deleteTargetEAccounts(target);
    }

    private void deleteTargetEAccounts(DummyTestResource resource) {
        resource.controller.getDummyResource().clear();
    }

    private void recreateSourceEAccounts(DummyTestResource resource) throws ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException, ObjectAlreadyExistsException {
        resource.controller.getDummyResource().clear();
        for (int i = 1; i <= ERRORS_ACCOUNTS; i++) {
            resource.controller.addAccount(String.format("e-%06d", i));
        }
    }

    private void deleteEUsers() throws SchemaException, ObjectNotFoundException {
        deleteUsers(getStartsWithQuery("e-"), getTestOperationResult());
    }

    private void deleteEShadows() throws SchemaException, ObjectNotFoundException {
        deleteShadows(getStartsWithQuery("e-"), getTestOperationResult());
    }

    private ObjectQuery getStartsWithQuery(String s) {
        return prismContext.queryFor(ObjectType.class)
                .item(UserType.F_NAME).startsWith(s)
                .build();
    }

    private void deleteUsers(ObjectQuery query, OperationResult result) throws SchemaException, ObjectNotFoundException {
        for (PrismObject<UserType> user : repositoryService.searchObjects(UserType.class, query, null, result)) {
            System.out.println("Deleting " + user);
            repositoryService.deleteObject(UserType.class, user.getOid(), result);
        }
    }

    private void deleteShadows(ObjectQuery query, OperationResult result) throws SchemaException, ObjectNotFoundException {
        for (PrismObject<ShadowType> shadow : repositoryService.searchObjects(ShadowType.class, query, null, result)) {
            System.out.println("Deleting " + shadow);
            repositoryService.deleteObject(ShadowType.class, shadow.getOid(), result);
        }
    }

    private Consumer<PrismObject<TaskType>> workerThreadsCustomizer(int threads) {
        return rootActivityWorkerThreadsCustomizer(threads);
    }
}
