/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static com.evolveum.midpoint.test.DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME;

import static com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.schema.statistics.SynchronizationInformation;

import com.evolveum.midpoint.test.DummyTestResource;

import com.evolveum.midpoint.test.asserter.TaskAsserter;
import com.evolveum.midpoint.util.exception.CommonException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests interruption of live sync task in various scenarios (see MID-5353, MID-5513).
 * In the second part tests various task statistics (MID-5999, MID-5920).
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLiveSyncTaskMechanics extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/sync");

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
    private static final TestResource ROLE_XFER1 = new TestResource(TEST_DIR, "role-xfer1.xml", "4b141ca2-3172-4d8a-8614-97e01ece5a9e");
    private static final TestResource ROLE_XFER2 = new TestResource(TEST_DIR, "role-xfer2.xml", "59fdad1b-45fa-4a8c-bda4-d8a6ab980671");
    private static final TestResource TASK_XFER1 = new TestResource(TEST_DIR, "task-xfer1.xml", "c9306381-efa8-499e-8b16-6d071d680451");
    private static final TestResource TASK_XFER2 = new TestResource(TEST_DIR, "task-xfer2.xml", "d4f8b735-dfdb-450e-a680-dacfac4fafb0");

    private static final TestResource TASK_SLOW_RESOURCE = new TestResource(TEST_DIR, "task-intsync-slow-resource.xml", "ca51f209-1ef5-42b3-84e7-5f639ee8e300");
    private static final TestResource TASK_SLOW_MODEL = new TestResource(TEST_DIR, "task-intsync-slow-model.xml", "c37dda96-e547-41c2-b343-b890bc7fade9");
    private static final TestResource TASK_BATCHED = new TestResource(TEST_DIR, "task-intsync-batched.xml", "ef22bf7b-5d28-4a57-b3a5-6fa58491eeb3");
    private static final TestResource TASK_ERROR = new TestResource(TEST_DIR, "task-intsync-error.xml", "b697f3a8-9d02-4924-8627-c1f216e88ed3");
    private static final TestResource TASK_SLOW_RESOURCE_IMPRECISE = new TestResource(TEST_DIR, "task-intsync-slow-resource-imprecise.xml", "82407cd3-7b1f-4054-b45a-fc4d9aed8ae3");
    private static final TestResource TASK_SLOW_MODEL_IMPRECISE = new TestResource(TEST_DIR, "task-intsync-slow-model-imprecise.xml", "066c6993-8b94-445c-aaff-937184bbe6ca");
    private static final TestResource TASK_BATCHED_IMPRECISE = new TestResource(TEST_DIR, "task-intsync-batched-imprecise.xml", "dcfe4c53-a851-4fe1-90eb-f75d9c65d2e6");
    private static final TestResource TASK_ERROR_IMPRECISE = new TestResource(TEST_DIR, "task-intsync-error-imprecise.xml", "c554ec0f-95c3-40ac-b069-876708d28393");

    private static final TestResource TASK_DRY_RUN = new TestResource(TEST_DIR, "task-intsync-dry-run.xml", "8b5b3b2d-6ef7-4cc8-8507-42778e0d869f");
    private static final TestResource TASK_DRY_RUN_WITH_UPDATE = new TestResource(TEST_DIR, "task-intsync-dry-run-with-update.xml", "ebcc7393-e886-40ae-8a9f-dfa72230c658");

    private static final TestResource TASK_NO_POLICY = new TestResource(TEST_DIR, "task-no-policy.xml", "b2aa4e0a-1fce-499d-8502-ece187b24ae4");

    private static final String USER_P = "user-p-";
    private static final String USER_I = "user-i-";

    private static final int ERROR_ON = 4;
    private static final int USERS = 100;

    private static final int XFER_ACCOUNTS = 10;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

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

        initLiveSyncTask(TASK_SLOW_RESOURCE, initTask, initResult);
        initLiveSyncTask(TASK_SLOW_RESOURCE_IMPRECISE, initTask, initResult);
        initLiveSyncTask(TASK_SLOW_MODEL, initTask, initResult);
        initLiveSyncTask(TASK_SLOW_MODEL_IMPRECISE, initTask, initResult);
        initLiveSyncTask(TASK_BATCHED, initTask, initResult);

        addObject(TASK_BATCHED_IMPRECISE.file, initTask, initResult, workerThreadsCustomizer(getWorkerThreads()));
        // Starting this task results in (expected) exception

        initLiveSyncTask(TASK_ERROR, initTask, initResult);
        initLiveSyncTask(TASK_ERROR_IMPRECISE, initTask, initResult);
        initLiveSyncTask(TASK_DRY_RUN, initTask, initResult);
        initLiveSyncTask(TASK_DRY_RUN_WITH_UPDATE, initTask, initResult);
        initLiveSyncTask(TASK_NO_POLICY, initTask, initResult);
        initLiveSyncTask(TASK_XFER1, initTask, initResult);
        initLiveSyncTask(TASK_XFER2, initTask, initResult);

        assertUsers(getNumberOfUsers());
        for (int i = 0; i < USERS; i++) {
            interruptedSyncResource.getController().addAccount(getUserName(i, true));
            interruptedSyncImpreciseResource.getController().addAccount(getUserName(i, false));
        }

        //setGlobalTracingOverride(createModelLoggingTracingProfile());
    }

    private void initLiveSyncTask(TestResource testResource, Task initTask, OperationResult initResult)
            throws java.io.IOException, CommonException {
        addObject(testResource.file, initTask, initResult, workerThreadsCustomizer(getWorkerThreads()));
        waitForTaskFinish(testResource.oid, false);
    }

    int getWorkerThreads() {
        return 0;
    }

    private String getUserName(int i, boolean precise) {
        return String.format("%s%06d", precise ? USER_P : USER_I, i);
    }

    /**
     * Original meaning of this test was:
     *      Suspends LiveSync task in the first stage when it gathers changes via ICF Sync operation.
     *      Token should be 0, because nothing was processed yet (regardless of precise/imprecise token handling).
     *
     * However, now the live sync processing is iterative: changes are fetched and then applied. So this makes no difference.
     * Nevertheless, it might be useful to test suspension in early stages of task run.
     *
     * When dealing with precise resources, the suspension must come before first change is fetched. So we set the delay
     * to 2 seconds.
     */
    @Test
    public void test100SuspendWhileIcfSync() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Resource gives out changes slowly now.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(2000);

        // WHEN
        when();

        waitForTaskNextStart(TASK_SLOW_RESOURCE.oid, false, 2000, true);  // starts the task
        boolean suspended = suspendTask(TASK_SLOW_RESOURCE.oid, 10000);

        // THEN
        then();

        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_RESOURCE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertEquals("Wrong token value", (Integer) 0, taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN));
    }

    /**
     * The same as test100.
     * The delay can be smaller, as even if some changes are fetched and processed, the token will not be updated.
     */
    @Test
    public void test105SuspendWhileIcfSyncImprecise() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Resource gives out changes slowly now.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(500);

        // WHEN
        when();

        waitForTaskNextStart(TASK_SLOW_RESOURCE_IMPRECISE.oid, false, 2000, true);  // starts the task
        boolean suspended = suspendTask(TASK_SLOW_RESOURCE_IMPRECISE.oid, 5000);

        // THEN
        then();

        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_RESOURCE_IMPRECISE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertEquals("Wrong token value", (Integer) 0, taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN));
    }

    /**
     * Original meaning of this test was:
     *      Suspends LiveSync task in the second stage when changes are being processed.
     *      For precise token providing resource the token should correspond to objects that were actually processed.
     *
     * Now, when the processing is iterative, we simply suspend the task during iterative processing of changes.
     * The result should be the same.
     */
    @Test
    public void test110SuspendWhileProcessing() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Resource gives out changes quickly. But they are processed slowly.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 100;

        // WHEN
        when();

        waitForTaskNextStart(TASK_SLOW_MODEL.oid, false, 2000, true);  // starts the task
        Thread.sleep(4000);
        boolean suspended = suspendTask(TASK_SLOW_MODEL.oid, 5000);

        // THEN
        then();

        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_MODEL.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        // If we are particularly unfortunate the token value could be zero in multithreaded scenario:
        // This could occur if the first sync delta is processed only after the second, third, etc.
        // If this happens in reality, we need to adapt the assertion here.
        assertTrue("Token value is zero (should be greater)", token != null && token > 0);

        int progress = (int) taskAfter.getProgress();
        displayValue("Token value", token);
        displayValue("Task progress", progress);
        if (getWorkerThreads() <= 1) {
            assertEquals("Wrong task progress", token, (Integer) progress);
        } else {
            assertTrue("Token is too high: " + token + ", while progress is " + progress,
                    token <= progress);
        }

        assertObjects(UserType.class, getStartsWithQuery(USER_P), progress);
    }

    /**
     * Original meaning of this test was:
     *      Suspends LiveSync task in the second stage when changes are being processed.
     *      For imprecise token providing resource the token should stay unchanged, i.e. here at 0. (MID-5513)
     *
     * Now, when the processing is iterative, we simply suspend the task during iterative processing of changes.
     * The result should be the same.
     */
    @Test
    public void test115SuspendWhileProcessingImprecise() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Resource gives out changes quickly. But they are processed slowly.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 100;

        // WHEN
        when();

        waitForTaskNextStart(TASK_SLOW_MODEL_IMPRECISE.oid, false, 2000, true);  // starts the task
        Thread.sleep(4000);
        boolean suspended = suspendTask(TASK_SLOW_MODEL_IMPRECISE.oid, 5000);

        // THEN
        then();

        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_MODEL_IMPRECISE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);

        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        displayValue("Token value", token);
        assertEquals("Wrong token value", (Integer) 0, token);

        int progress = (int) taskAfter.getProgress();
        displayValue("Task progress", progress);

        assertObjects(UserType.class, getStartsWithQuery(USER_I), progress);
    }

    /**
     * Batched operation with precise token values. Should fetch exactly 10 records in the first sync run
     * and 10 records in the second sync run.
     */
    @Test
    public void test120Batched() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally. But we will take only first 10 of them.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = getUserName(24, true);

        // WHEN
        when();

        waitForTaskNextRun(TASK_BATCHED.oid, false, 10000, true);

        // THEN
        then();

        Task taskAfter = taskManager.getTaskWithResult(TASK_BATCHED.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) 10, token);

        assertObjects(UserType.class, query, 10);

        // WHEN
        when();

        waitForTaskNextRun(TASK_BATCHED.oid, false, 10000, true);

        // THEN
        then();

        taskAfter = taskManager.getTaskWithResult(TASK_BATCHED.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) 20, token);

        assertObjects(UserType.class, query, 20);

        // WHEN 3 (with error)
        when();

        waitForTaskNextRun(TASK_BATCHED.oid, false, 10000, true);

        // THEN 3 (with error)
        then();

        taskAfter = taskManager.getTaskWithResult(TASK_BATCHED.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertPartialError(taskAfter.getResult());          // error was "skippable" (retryLiveSyncErrors = false)

        token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) 30, token);     // therefore we should go on

        assertObjects(UserType.class, query, 29);       // and all records should be imported
    }

    /**
     * Batched operation with imprecise token values. This should not be allowed.
     */
    @Test
    public void test125BatchedImprecise() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Changes are provided and processed normally. But we will take only first 10 of them.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 0;

        // WHEN
        when();

        try {
            waitForTaskNextRun(TASK_BATCHED_IMPRECISE.oid, false, 10000, true);
        } catch (Throwable t) {
            suspendTask(TASK_BATCHED_IMPRECISE.oid, 10000);
            throw t;
        }

        // THEN
        then();

        Task taskAfter = taskManager.getTaskWithResult(TASK_BATCHED_IMPRECISE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertFailure(taskAfter.getResult());
        assertEquals("Wrong task state", TaskExecutionStatus.CLOSED, taskAfter.getExecutionStatus());
    }

    /**
     * Errored operation or R-th object (i.e. ERROR_ON+1) with precise token values.
     *
     * Single-threaded:
     * - Should process exactly R records in the first sync run and stop.
     *
     * Multi-threaded:
     * - Should process approximately R records (might be more, might be less) and stop.
     *
     * Both:
     * - Token should point to record R-1 (=ERROR_ON), so R is fetched next.
     */
    @Test
    public void test130Error() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = getUserName(ERROR_ON, true);

        // WHEN
        when();

        waitForTaskNextRun(TASK_ERROR.oid, false, 10000, true);

        // THEN
        then();

        Task taskAfter = taskManager.getTaskWithResult(TASK_ERROR.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertPartialError(taskAfter.getResult());      // the task should continue (i.e. not suspend) - TODO reconsider this
        assertTaskClosed(taskAfter);

        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) ERROR_ON, token);

        if (getWorkerThreads() <= 1) {
            assertObjects(UserType.class, query, ERROR_ON);     // 0..ERROR_ON-1
        }

        // Another run - should fail the same

        // WHEN
        when();

        waitForTaskNextRun(TASK_ERROR.oid, false, 10000, true);

        // THEN
        then();

        taskAfter = taskManager.getTaskWithResult(TASK_ERROR.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 0;
        DummyInterruptedSyncImpreciseResource.errorOn = getUserName(ERROR_ON, false);

        // WHEN
        when();

        try {
            waitForTaskNextRun(TASK_ERROR_IMPRECISE.oid, false, 10000, true);
        } catch (Throwable t) {
            suspendTask(TASK_ERROR_IMPRECISE.oid, 10000);
            throw t;
        }

        // THEN
        then();

        Task taskAfter = taskManager.getTaskWithResult(TASK_ERROR_IMPRECISE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertPartialError(taskAfter.getResult());            // the task should continue (i.e. not suspend) - TODO reconsider this
        assertTaskClosed(taskAfter);

        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) 0, token);

        if (getWorkerThreads() <= 1) {
            verbose = true;
            assertObjects(UserType.class, query, ERROR_ON);     // 0..ERROR_ON-1
            verbose = false;
        }

        // Another run - should fail the same

        // WHEN
        when();

        waitForTaskNextRun(TASK_ERROR_IMPRECISE.oid, false, 10000, true);

        // THEN
        then();

        taskAfter = taskManager.getTaskWithResult(TASK_ERROR_IMPRECISE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) 0, token);

        if (getWorkerThreads() <= 1) {
            assertObjects(UserType.class, query, ERROR_ON);
        }
    }

    /**
     * Dry run. Should process all records, but create no users and not update the token.
     */
    @Test
    public void test140DryRun() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = null;

        // WHEN
        when();

        waitForTaskNextRun(TASK_DRY_RUN.oid, false, 10000, true);

        // THEN
        then();

        Task taskAfter = taskManager.getTaskWithResult(TASK_DRY_RUN.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertSuccess(taskAfter.getResult());
        assertTaskClosed(taskAfter);

        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) 0, token);

        assertObjects(UserType.class, query, 0);
    }

    /**
     * Dry run with update. Should process all records, but create no users and then update the token.
     */
    @Test
    public void test150DryRunWithUpdate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = null;

        // WHEN
        when();

        waitForTaskNextRun(TASK_DRY_RUN_WITH_UPDATE.oid, false, 10000, true);

        // THEN
        then();

        Task taskAfter = taskManager.getTaskWithResult(TASK_DRY_RUN_WITH_UPDATE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertSuccess(taskAfter.getResult());
        assertTaskClosed(taskAfter);

        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
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

        waitForTaskNextRun(TASK_NO_POLICY.oid, false, 10000, true);

        then();

        Task taskAfter = taskManager.getTaskWithResult(TASK_NO_POLICY.oid, result);
        display("Task after", taskAfter);
        SynchronizationInformationType syncInfo = taskAfter.getStoredOperationStats().getSynchronizationInformation();
        displayValue("Sync info", SynchronizationInformation.format(syncInfo));
        assertSuccess(taskAfter.getResult());
        assertTaskClosed(taskAfter);

        assertSyncToken(taskAfter, 1);
        assertEquals("Wrong noSyncPolicy counter value", 1, syncInfo.getCountNoSynchronizationPolicy());
        assertEquals("Wrong noSyncPolicyAfter counter value", 1, syncInfo.getCountNoSynchronizationPolicyAfter());
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

    private void doXferInitialSync(int index, TestResource xferTask, DummyTestResource xferSource) throws Exception {
        given();

        assertTask(xferTask.oid, "before")
                .synchronizationInformation()
                    .display()
                    .assertTotal(0, 0)
                    .end()
                .actionsExecutedInformation()
                    .display()
                    .assertEmpty()
                    .end();

        for (int i = 0; i < XFER_ACCOUNTS; i++) {
            String name = String.format("xfer%d-%04d", index, i);
            xferSource.controller.addAccount(name, name);
        }

        when();

        waitForTaskNextRun(xferTask.oid, false, 60000, true);

        then();

        assertTask(xferTask.oid, "after")
                .synchronizationInformation()
                    .display()
                    .assertTotal(XFER_ACCOUNTS, XFER_ACCOUNTS)
                    .assertUnmatched(XFER_ACCOUNTS, 0)
                    .assertLinked(0, XFER_ACCOUNTS)
                    .end()
                .actionsExecutedInformation()
                    .display()
                    .resulting()
                        .assertCount(3*XFER_ACCOUNTS, 0)
                        .assertCount(ADD, UserType.COMPLEX_TYPE, XFER_ACCOUNTS, 0)
                        .assertCount(ADD, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0)
                        .assertCount(MODIFY, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0)
                        .end()
                    .end()
                .assertClosed()
                .assertSuccess();
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
        asserter
                .assertSuccess()
                .synchronizationInformation()
                    .display()
                    .assertTotal(2*XFER_ACCOUNTS, 2*XFER_ACCOUNTS) // each account was touched twice
                    .assertUnmatched(XFER_ACCOUNTS, 0) // this is information from the first run
                    .assertLinked(XFER_ACCOUNTS, 2*XFER_ACCOUNTS) // this is combined from the first and second runs
                    .end()
                .actionsExecutedInformation()
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
        doXferRenameAndSync(TASK_XFER2, RESOURCE_DUMMY_XFER2_SOURCE)
                .assertPartialError()
                .synchronizationInformation()
                    .display()
                    .assertTotal(XFER_ACCOUNTS+t, XFER_ACCOUNTS+t) // XFER_ACCOUNTS from the first run, t from the second (failed immediately)
                    .assertUnmatched(XFER_ACCOUNTS, 0) // from the first run
                    .assertLinked(t, XFER_ACCOUNTS+t) // 1 from the second run
                .end()
                .actionsExecutedInformation()
                    .display()
                    .resulting()
                        .assertCount(3*XFER_ACCOUNTS+2*t, t)
                        .assertCount(ADD, UserType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
                        .assertCount(ADD, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
                        .assertCount(MODIFY, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS+t, 0) // from the first+second runs
                        .assertCount(MODIFY, UserType.COMPLEX_TYPE, t, 0) // from the second runs
                        .assertCount(DELETE, ShadowType.COMPLEX_TYPE, 0, t) // from the second run
                        .end()
                    .end();
    }

    private TaskAsserter<Void> doXferRenameAndSync(TestResource xferTask, DummyTestResource xferSource) throws Exception {
        given();

        assertTask(xferTask.oid, "before")
                .synchronizationInformation().display().end()
                .actionsExecutedInformation().display().end();

        DummyResource resource = xferSource.controller.getDummyResource();
        for (DummyAccount account : new ArrayList<>(resource.listAccounts())) {
            account.replaceAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "_" + account.getName());
        }

        when();

        waitForTaskNextRun(xferTask.oid, false, 60000, true);

        then();

        return assertTask(xferTask.oid, "after")
                .assertClosed();
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
        if (getWorkerThreads() > 0) {
            doXferLiveSync(TASK_XFER2)
                    .assertPartialError()
                    .synchronizationInformation().display().end()
                    .actionsExecutedInformation().display().end();
            // No special asserts here. The number of accounts being processed may depend on the timing.
        } else {
            doXferLiveSync(TASK_XFER2)
                    .assertPartialError()
                    .synchronizationInformation()
                        .display()
                            .assertTotal(XFER_ACCOUNTS+3, XFER_ACCOUNTS+3) // XFER_ACCOUNTS from the first run, 1 from the second (failed immediately), 2 for the third (first ok, second fails)
                            .assertUnmatched(XFER_ACCOUNTS, 0) // from the first run
                            .assertLinked(3, XFER_ACCOUNTS+3) // 1 from the second run, 2 from the third
                        .end()
                    .actionsExecutedInformation()
                        .display()
                        .resulting()
                            .assertCount(3*XFER_ACCOUNTS+5, 2)
                            .assertCount(ADD, UserType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
                            .assertCount(ADD, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS, 0) // from the first run
                            .assertCount(MODIFY, ShadowType.COMPLEX_TYPE, XFER_ACCOUNTS+3, 0) // from the first+second+third (10+1+2) runs
                            .assertCount(MODIFY, UserType.COMPLEX_TYPE, 2, 0) // from the second+third runs (1+1)
                            .assertCount(DELETE, ShadowType.COMPLEX_TYPE, 0, 2) // from the second+third run (1+1)
                        .end()
                    .end();
        }

        // Note: it seems that the failed delete caused unlinking the account, so the next live sync on the "11-th" account
        // proceeds without problems.
    }

    private TaskAsserter<Void> doXferLiveSync(TestResource xferTask) throws Exception {
        given();

        assertTask(xferTask.oid, "before")
                .synchronizationInformation().display().end()
                .actionsExecutedInformation().display().end();

        when();

        waitForTaskNextRun(xferTask.oid, false, 60000, true);

        then();

        return assertTask(xferTask.oid, "after")
                .assertClosed();
    }

    private ObjectQuery getStartsWithQuery(String s) {
        return prismContext.queryFor(UserType.class)
                .item(UserType.F_NAME).startsWith(s)
                .build();
    }

    private void deleteUsers(ObjectQuery query, OperationResult result) throws SchemaException, ObjectNotFoundException {
        for (PrismObject<UserType> user: repositoryService.searchObjects(UserType.class, query, null, result)) {
            System.out.println("Deleting " + user);
            repositoryService.deleteObject(UserType.class, user.getOid(), result);
        }
    }
}
