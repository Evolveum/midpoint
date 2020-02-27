/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.sync;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.function.Consumer;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 *  MID-5353, MID-5513
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLiveSyncTaskMechanics extends AbstractInitializedModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/sync");

    private DummyInterruptedSyncResource interruptedSyncResource;
    private DummyInterruptedSyncImpreciseResource interruptedSyncImpreciseResource;

    private static final File TASK_SLOW_RESOURCE_FILE = new File(TEST_DIR, "task-intsync-slow-resource.xml");
    private static final String TASK_SLOW_RESOURCE_OID = "ca51f209-1ef5-42b3-84e7-5f639ee8e300";

    private static final File TASK_SLOW_MODEL_FILE = new File(TEST_DIR, "task-intsync-slow-model.xml");
    private static final String TASK_SLOW_MODEL_OID = "c37dda96-e547-41c2-b343-b890bc7fade9";

    private static final File TASK_BATCHED_FILE = new File(TEST_DIR, "task-intsync-batched.xml");
    private static final String TASK_BATCHED_OID = "ef22bf7b-5d28-4a57-b3a5-6fa58491eeb3";

    private static final File TASK_ERROR_FILE = new File(TEST_DIR, "task-intsync-error.xml");
    private static final String TASK_ERROR_OID = "b697f3a8-9d02-4924-8627-c1f216e88ed3";

    private static final File TASK_SLOW_RESOURCE_IMPRECISE_FILE = new File(TEST_DIR, "task-intsync-slow-resource-imprecise.xml");
    private static final String TASK_SLOW_RESOURCE_IMPRECISE_OID = "82407cd3-7b1f-4054-b45a-fc4d9aed8ae3";

    private static final File TASK_SLOW_MODEL_IMPRECISE_FILE = new File(TEST_DIR, "task-intsync-slow-model-imprecise.xml");
    private static final String TASK_SLOW_MODEL_IMPRECISE_OID = "066c6993-8b94-445c-aaff-937184bbe6ca";

    private static final File TASK_BATCHED_IMPRECISE_FILE = new File(TEST_DIR, "task-intsync-batched-imprecise.xml");
    private static final String TASK_BATCHED_IMPRECISE_OID = "dcfe4c53-a851-4fe1-90eb-f75d9c65d2e6";

    private static final File TASK_ERROR_IMPRECISE_FILE = new File(TEST_DIR, "task-intsync-error-imprecise.xml");
    private static final String TASK_ERROR_IMPRECISE_OID = "c554ec0f-95c3-40ac-b069-876708d28393";

    private static final TestResource TASK_DRY_RUN = new TestResource(TEST_DIR, "task-intsync-dry-run.xml", "8b5b3b2d-6ef7-4cc8-8507-42778e0d869f");
    private static final TestResource TASK_DRY_RUN_WITH_UPDATE = new TestResource(TEST_DIR, "task-intsync-dry-run-with-update.xml", "ebcc7393-e886-40ae-8a9f-dfa72230c658");

    private static final String USER_P = "user-p-";
    private static final String USER_I = "user-i-";

    private static final int ERROR_ON = 4;
    private static final int USERS = 100;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        interruptedSyncResource = new DummyInterruptedSyncResource();
        interruptedSyncResource.init(dummyResourceCollection, initTask, initResult);

        interruptedSyncImpreciseResource = new DummyInterruptedSyncImpreciseResource();
        interruptedSyncImpreciseResource.init(dummyResourceCollection, initTask, initResult);

        // Initial run of these tasks must come before accounts are created.

        Consumer<PrismObject<TaskType>> workerThreadsCustomizer = workerThreadsCustomizer(getWorkerThreads());

        addObject(TASK_SLOW_RESOURCE_FILE, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_SLOW_RESOURCE_OID, false);

        addObject(TASK_SLOW_RESOURCE_IMPRECISE_FILE, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_SLOW_RESOURCE_IMPRECISE_OID, false);

        addObject(TASK_SLOW_MODEL_FILE, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_SLOW_MODEL_OID, false);

        addObject(TASK_SLOW_MODEL_IMPRECISE_FILE, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_SLOW_MODEL_IMPRECISE_OID, false);

        addObject(TASK_BATCHED_FILE, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_BATCHED_OID, false);

        addObject(TASK_BATCHED_IMPRECISE_FILE, initTask, initResult, workerThreadsCustomizer);
        // Starting this task results in (expected) exception

        addObject(TASK_ERROR_FILE, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_ERROR_OID, false);

        addObject(TASK_ERROR_IMPRECISE_FILE, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_ERROR_IMPRECISE_OID, false);

        addObject(TASK_DRY_RUN.file, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_DRY_RUN.oid, false);

        addObject(TASK_DRY_RUN_WITH_UPDATE.file, initTask, initResult, workerThreadsCustomizer);
        waitForTaskFinish(TASK_DRY_RUN_WITH_UPDATE.oid, false);

        assertUsers(getNumberOfUsers());
        for (int i = 0; i < USERS; i++) {
            interruptedSyncResource.getController().addAccount(getUserName(i, true));
            interruptedSyncImpreciseResource.getController().addAccount(getUserName(i, false));
        }
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
        final String TEST_NAME = "test100SuspendWhileIcfSync";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Resource gives out changes slowly now.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(2000);

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextStart(TASK_SLOW_RESOURCE_OID, false, 2000, true);  // starts the task
        boolean suspended = suspendTask(TASK_SLOW_RESOURCE_OID, 10000);

        // THEN
        displayThen(TEST_NAME);

        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_RESOURCE_OID, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertEquals("Wrong token value", (Integer) 0, taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN));
    }

    /**
     * The same as test100.
     * The delay can be smaller, as even if some changes are fetched and processed, the token will not be updated.
     */
    @Test
    public void test105SuspendWhileIcfSyncImprecise() throws Exception {
        final String TEST_NAME = "test105SuspendWhileIcfSyncImprecise";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Resource gives out changes slowly now.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(500);

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextStart(TASK_SLOW_RESOURCE_IMPRECISE_OID, false, 2000, true);  // starts the task
        boolean suspended = suspendTask(TASK_SLOW_RESOURCE_IMPRECISE_OID, 5000);

        // THEN
        displayThen(TEST_NAME);

        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_RESOURCE_IMPRECISE_OID, result);
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
        final String TEST_NAME = "test110SuspendWhileProcessing";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Resource gives out changes quickly. But they are processed slowly.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 100;

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextStart(TASK_SLOW_MODEL_OID, false, 2000, true);  // starts the task
        Thread.sleep(4000);
        boolean suspended = suspendTask(TASK_SLOW_MODEL_OID, 5000);

        // THEN
        displayThen(TEST_NAME);

        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_MODEL_OID, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        // If we are particularly unfortunate the token value could be zero in multithreaded scenario:
        // This could occur if the first sync delta is processed only after the second, third, etc.
        // If this happens in reality, we need to adapt the assertion here.
        assertTrue("Token value is zero (should be greater)", token != null && token > 0);

        int progress = (int) taskAfter.getProgress();
        display("Token value", token);
        display("Task progress", progress);
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
        final String TEST_NAME = "test115SuspendWhileProcessingImprecise";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Resource gives out changes quickly. But they are processed slowly.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 100;

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextStart(TASK_SLOW_MODEL_IMPRECISE_OID, false, 2000, true);  // starts the task
        Thread.sleep(4000);
        boolean suspended = suspendTask(TASK_SLOW_MODEL_IMPRECISE_OID, 5000);

        // THEN
        displayThen(TEST_NAME);

        assertTrue("Task was not suspended", suspended);
        Task taskAfter = taskManager.getTaskWithResult(TASK_SLOW_MODEL_IMPRECISE_OID, result);
        displayTaskWithOperationStats("Task after", taskAfter);

        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        display("Token value", token);
        assertEquals("Wrong token value", (Integer) 0, token);

        int progress = (int) taskAfter.getProgress();
        display("Task progress", progress);

        assertObjects(UserType.class, getStartsWithQuery(USER_I), progress);
    }

    /**
     * Batched operation with precise token values. Should fetch exactly 10 records in the first sync run
     * and 10 records in the second sync run.
     */
    @Test
    public void test120Batched() throws Exception {
        final String TEST_NAME = "test120Batched";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally. But we will take only first 10 of them.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = getUserName(24, true);

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextRun(TASK_BATCHED_OID, false, 10000, true);

        // THEN
        displayThen(TEST_NAME);

        Task taskAfter = taskManager.getTaskWithResult(TASK_BATCHED_OID, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) 10, token);

        assertObjects(UserType.class, query, 10);

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextRun(TASK_BATCHED_OID, false, 10000, true);

        // THEN
        displayThen(TEST_NAME);

        taskAfter = taskManager.getTaskWithResult(TASK_BATCHED_OID, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) 20, token);

        assertObjects(UserType.class, query, 20);

        // WHEN 3 (with error)
        displayWhen(TEST_NAME);

        waitForTaskNextRun(TASK_BATCHED_OID, false, 10000, true);

        // THEN 3 (with error)
        displayThen(TEST_NAME);

        taskAfter = taskManager.getTaskWithResult(TASK_BATCHED_OID, result);
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
        final String TEST_NAME = "test125BatchedImprecise";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Changes are provided and processed normally. But we will take only first 10 of them.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 0;

        // WHEN
        displayWhen(TEST_NAME);

        try {
            waitForTaskNextRun(TASK_BATCHED_IMPRECISE_OID, false, 10000, true);
        } catch (Throwable t) {
            suspendTask(TASK_BATCHED_IMPRECISE_OID, 10000);
            throw t;
        }

        // THEN
        displayThen(TEST_NAME);

        Task taskAfter = taskManager.getTaskWithResult(TASK_BATCHED_IMPRECISE_OID, result);
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
        final String TEST_NAME = "test130Error";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = getUserName(ERROR_ON, true);

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextRun(TASK_ERROR_OID, false, 10000, true);

        // THEN
        displayThen(TEST_NAME);

        Task taskAfter = taskManager.getTaskWithResult(TASK_ERROR_OID, result);
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
        displayWhen(TEST_NAME);

        waitForTaskNextRun(TASK_ERROR_OID, false, 10000, true);

        // THEN
        displayThen(TEST_NAME);

        taskAfter = taskManager.getTaskWithResult(TASK_ERROR_OID, result);
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
        final String TEST_NAME = "test135ErrorImprecise";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_I);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncImpreciseResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncImpreciseResource.delay = 0;
        DummyInterruptedSyncImpreciseResource.errorOn = getUserName(ERROR_ON, false);

        // WHEN
        displayWhen(TEST_NAME);

        try {
            waitForTaskNextRun(TASK_ERROR_IMPRECISE_OID, false, 10000, true);
        } catch (Throwable t) {
            suspendTask(TASK_ERROR_IMPRECISE_OID, 10000);
            throw t;
        }

        // THEN
        displayThen(TEST_NAME);

        Task taskAfter = taskManager.getTaskWithResult(TASK_ERROR_IMPRECISE_OID, result);
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
        displayWhen(TEST_NAME);

        waitForTaskNextRun(TASK_ERROR_IMPRECISE_OID, false, 10000, true);

        // THEN
        displayThen(TEST_NAME);

        taskAfter = taskManager.getTaskWithResult(TASK_ERROR_IMPRECISE_OID, result);
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
        final String TEST_NAME = "test140DryRun";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = null;

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextRun(TASK_DRY_RUN.oid, false, 10000, true);

        // THEN
        displayThen(TEST_NAME);

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
        final String TEST_NAME = "test150DryRunWithUpdate";

        // GIVEN
        AbstractSynchronizationStoryTest.class.getName();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = getStartsWithQuery(USER_P);
        deleteUsers(query, result);

        // Changes are provided and processed normally.
        interruptedSyncResource.getDummyResource().setOperationDelayOffset(0);
        DummyInterruptedSyncResource.delay = 0;
        DummyInterruptedSyncResource.errorOn = null;

        // WHEN
        displayWhen(TEST_NAME);

        waitForTaskNextRun(TASK_DRY_RUN_WITH_UPDATE.oid, false, 10000, true);

        // THEN
        displayThen(TEST_NAME);

        Task taskAfter = taskManager.getTaskWithResult(TASK_DRY_RUN_WITH_UPDATE.oid, result);
        displayTaskWithOperationStats("Task after", taskAfter);
        assertSuccess(taskAfter.getResult());
        assertTaskClosed(taskAfter);

        Integer token = taskAfter.getExtensionPropertyRealValue(SchemaConstants.SYNC_TOKEN);
        assertEquals("Wrong token value", (Integer) USERS, token);

        assertObjects(UserType.class, query, 0);
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
