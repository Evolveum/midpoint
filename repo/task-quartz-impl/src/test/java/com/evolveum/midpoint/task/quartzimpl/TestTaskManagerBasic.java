/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
import static com.evolveum.midpoint.util.MiscUtil.extractSingleton;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.schema.util.task.TaskTreeUtil;
import com.evolveum.midpoint.task.api.RunningLightweightTask;

import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.NotNull;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.quartz.QuartzUtil;
import com.evolveum.midpoint.task.quartzimpl.run.JobExecutor;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskManagerBasic extends AbstractTaskManagerTest {

    private static final String TASK_OWNER_FILENAME = "src/test/resources/basic/owner.xml";
    private static final String TASK_OWNER2_FILENAME = "src/test/resources/basic/owner2.xml";
    private static final String TASK_OWNER2_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
    protected static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/task-manager-test/extension";
    private static final ItemName ITEM_DEAD = new ItemName(NS_EXT, "dead");

    private static final File TEST_DIR = new File("src/test/resources/basic");
    private static final TestResource<TaskType> TASK_WITHOUT_PROGRESS = new TestResource<>(TEST_DIR, "task-without-progress.xml", "91919191-76e0-59e2-86d6-556655660003");
    private static final TestResource<TaskType> TASK_FOR_EXTENSION_TEST = new TestResource<>(TEST_DIR, "task-for-extension-test.xml", "91919191-76e0-59e2-86d6-556655660004");
    private static final TestResource<TaskType> TASK_SINGLE_RUN = new TestResource<>(TEST_DIR, "task-single-run.xml", "91919191-76e0-59e2-86d6-556655660005");
    private static final TestResource<TaskType> TASK_CYCLE_TIGHT = new TestResource<>(TEST_DIR, "task-cycle-tight.xml", "91919191-76e0-59e2-86d6-556655660006");
    private static final TestResource<TaskType> TASK_CYCLE_LOOSE = new TestResource<>(TEST_DIR, "task-cycle-loose.xml", "91919191-76e0-59e2-86d6-556655660009");
    private static final TestResource<TaskType> TASK_CYCLE_LOOSE_CRON = new TestResource<>(TEST_DIR, "task-cycle-loose-cron.xml", "91919191-76e0-59e2-86d6-556655660010");
    private static final TestResource<TaskType> TASK_TO_SUSPEND = new TestResource<>(TEST_DIR, "task-to-suspend.xml", "91919191-76e0-59e2-86d6-556655660012");
    private static final TestResource<TaskType> TASK_TO_RESUME_AND_SUSPEND = new TestResource<>(TEST_DIR, "task-to-resume-and-suspend.xml", "91919191-76e0-59e2-86d6-556655660013");
    private static final TestResource<TaskType> TASK_LONG_RUNNING = new TestResource<>(TEST_DIR, "task-long-running.xml", "91919191-76e0-59e2-86d6-556655660014");
    private static final TestResource<TaskType> TASK_TO_DELETE = new TestResource<>(TEST_DIR, "task-to-delete.xml", "91919191-76e0-59e2-86d6-556655660015");
    private static final TestResource<TaskType> TASK_WAITING_FOR_NO_ONE = new TestResource<>(TEST_DIR, "task-waiting-for-no-one.xml", "91919191-76e0-59e2-86d6-556655660017");
    private static final TestResource<TaskType> TASK_SIMPLE_WAITING = new TestResource<>(TEST_DIR, "task-simple-waiting.xml", "91919191-76e0-59e2-86d6-556655660020");
    private static final TestResource<TaskType> TASK_TREE_ROOT = new TestResource<>(TEST_DIR, "task-tree-root.xml", "91919191-76e0-59e2-86d6-556655660021");
    private static final TestResource<TaskType> TASK_TREE_CHILD_1 = new TestResource<>(TEST_DIR, "task-tree-child-1.xml", "91919191-76e0-59e2-86d6-556655661021");
    private static final TestResource<TaskType> TASK_TREE_CHILD_2 = new TestResource<>(TEST_DIR, "task-tree-child-2.xml", "91919191-76e0-59e2-86d6-556655662021");
    private static final TestResource<TaskType> TASK_RUN_ON_DEMAND = new TestResource<>(TEST_DIR, "task-run-on-demand.xml", "91919191-76e0-59e2-86d6-556655660022");
    private static final TestResource<TaskType> TASK_WITH_THREADS = new TestResource<>(TEST_DIR, "task-with-threads.xml", "91919191-76e0-59e2-86d6-556655660100");
    private static final TestResource<TaskType> TASK_WITH_THREADS_TO_SUSPEND = new TestResource<>(TEST_DIR, "task-with-threads-to-suspend.xml", "91919191-76e0-59e2-86d6-556655660105");
    private static final TestResource<TaskType> TASK_SEC_GROUP_LIMIT_EXP_1 = new TestResource<>(TEST_DIR, "task-sec-group-limit-exp-1.xml", "91919191-76e0-59e2-86d6-556655660108");
    private static final TestResource<TaskType> TASK_SEC_GROUP_LIMIT_EXP_1_RAM_1 = new TestResource<>(TEST_DIR, "task-sec-group-limit-exp-1-ram-1.xml", "91919191-76e0-59e2-86d6-55665566a108");
    private static final TestResource<TaskType> TASK_SEC_GROUP_LIMIT_RAM_NULL = new TestResource<>(TEST_DIR, "task-sec-group-limit-ram-null.xml", "91919191-76e0-59e2-86d6-55665566b108");
    private static final TestResource<TaskType> TASK_GROUP_LIMIT = new TestResource<>(TEST_DIR, "task-group-limit.xml", "91919191-76e0-59e2-86d6-556655660110");
    private static final TestResource<TaskType> TASK_GROUP_LIMIT_CONCURRENT = new TestResource<>(TEST_DIR, "task-group-limit-concurrent.xml", "91919191-76e0-59e2-86d6-55665566a110");
    private static final TestResource<TaskType> TASK_ALLOWED = new TestResource<>(TEST_DIR, "task-allowed.xml", "91919191-76e0-59e2-86d6-556655660120");
    private static final TestResource<TaskType> TASK_ALLOWED_NOT = new TestResource<>(TEST_DIR, "task-allowed-not.xml", "91919191-76e0-59e2-86d6-556655660130");
    private static final TestResource<TaskType> TASK_SUSPENDED_TREE_ROOT = new TestResource<>(TEST_DIR, "task-suspended-tree-root.xml", "91919191-76e0-59e2-86d6-556655660200");
    private static final TestResource<TaskType> TASK_SUSPENDED_TREE_CHILD_1 = new TestResource<>(TEST_DIR, "task-suspended-tree-child-1.xml", "10000000-76e0-59e2-86d6-556655660200");
    private static final TestResource<TaskType> TASK_SUSPENDED_TREE_CHILD_1_1 = new TestResource<>(TEST_DIR, "task-suspended-tree-child-1-1.xml", "11000000-76e0-59e2-86d6-556655660200");
    private static final TestResource<TaskType> TASK_SUSPENDED_TREE_CHILD_2 = new TestResource<>(TEST_DIR, "task-suspended-tree-child-2.xml", "20000000-76e0-59e2-86d6-556655660200");
    private static final TestResource<TaskType> TASK_DUMMY = new TestResource<>(TEST_DIR, "task-dummy.xml", "89bf08ec-c5b8-4641-95ca-37559c1f3896");
    private static final TestResource<TaskType> TASK_NON_EXISTING_OWNER = new TestResource<>(TEST_DIR, "task-non-existing-owner.xml", "d1320df4-e5b7-43ec-af53-f74ee0b62345");

    private static final ItemName ITEM_SHIP_STATE = new ItemName(NS_EXT, "shipState");

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        super.setup();
        ClusterManager.setUpdateNodeExecutionLimitations(false);
    }

    @PostConstruct
    public void initialize() throws Exception {
        super.initialize();
        addObjectFromFile(TASK_OWNER_FILENAME);
        addObjectFromFile(TASK_OWNER2_FILENAME);
    }

    /**
     * Test integrity of the test setup.
     */
    @Test
    public void test000Integrity() {
        assertNotNull(repositoryService);
        assertNotNull(taskManager);
    }

    /**
     * Can we create task and retrieve its progress?
     */
    @Test
    public void test100GetProgress() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        add(TASK_WITHOUT_PROGRESS, result);
        TaskQuartzImpl task = getTaskWithResult(TASK_WITHOUT_PROGRESS.oid, result);

        then();
        assertEquals(0, task.getLegacyProgress());
    }

    /**
     * Can we store large extension property in the task?
     */
    @Test
    public void test110TaskBigProperty() throws Exception {
        given();
        OperationResult result = createOperationResult();

        //noinspection unchecked
        PrismPropertyDefinition<String> shipStateDefinition =
                (PrismPropertyDefinition<String>) requireNonNull(
                        prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(ITEM_SHIP_STATE));

        add(TASK_FOR_EXTENSION_TEST, result);
        TaskQuartzImpl taskBefore = getTaskWithResult(TASK_FOR_EXTENSION_TEST.oid, result);
        PrismProperty<String> shipStateProperty = shipStateDefinition.instantiate();

        when("property add");

        String longStateValue = "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-";

        shipStateProperty.setRealValue(longStateValue);
        taskBefore.setExtensionProperty(shipStateProperty);
        taskBefore.flushPendingModifications(result);
        displayDumpable("Task after property add (in memory)", taskBefore);

        then("property add");

        Task taskAfterAdd = getTaskWithResult(TASK_FOR_EXTENSION_TEST.oid, result);
        displayDumpable("Task after property add (from repo)", taskAfterAdd);

        String shipStateAfterAdd = taskAfterAdd.getExtensionPropertyRealValue(ITEM_SHIP_STATE);
        assertEquals(longStateValue, shipStateAfterAdd);

        when("property change");

        String changedLongStateValue = "AAAAAAAAA-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-";

        shipStateProperty.setRealValue(changedLongStateValue);
        taskAfterAdd.setExtensionProperty(shipStateProperty);
        taskAfterAdd.flushPendingModifications(result);

        then("property change");

        Task taskAfterChange = getTaskWithResult(TASK_FOR_EXTENSION_TEST.oid, result);
        displayDumpable("Task after property change", taskAfterChange);

        String shipStateAfterChange = taskAfterChange.getExtensionPropertyRealValue(ITEM_SHIP_STATE);
        assertEquals(changedLongStateValue, shipStateAfterChange);
    }

    /**
     * Execute a single-run task. Tests also auto-cleanup feature.
     */
    @Test
    public void test120Single() throws Exception {
        given();

        OperationResult result = createOperationResult();

        mockTaskHandler.reset();

        when();

        add(TASK_SINGLE_RUN, result); // Task will get picked by task scanner and executed
        waitForTaskClose(TASK_SINGLE_RUN.oid, result, 10000);

        then();

        Task task = getTaskWithResult(TASK_SINGLE_RUN.oid, result);
        displayDumpable("Task (from task manager) after execution", task);

        PrismObject<TaskType> repoTask = repositoryService.getObject(TaskType.class, TASK_SINGLE_RUN.oid, null, result);
        displayDumpable("Task (from repo) after execution", repoTask);

        assertClosed(task);
        assertNotNull(task.getCompletionTimestamp());

        assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp() == 0);
        assertNotNull("LastRunFinishTimestamp is null", task.getLastRunFinishTimestamp());
        assertFalse("LastRunFinishTimestamp is 0", task.getLastRunFinishTimestamp() == 0);

        List<TriggerType> triggers = task.getUpdatedTaskObject().asObjectable().getTrigger();
        assertEquals(1, triggers.size());
        TriggerType trigger = triggers.get(0);
        long delta = XmlTypeConverter.toMillis(trigger.getTimestamp()) - task.getCompletionTimestamp();
        if (Math.abs(delta - 10000) > 1000) {
            fail("Auto cleanup timestamp was not computed correctly. Delta should be 10000, is " + delta);
        }

        assertTrue("Task reported no progress", task.getLegacyProgress() > 0);
        assertSuccess(task);

        assertNotNull("Handler is gone", task.getHandlerUri());
        assertTrue("Handler1 has not run", mockTaskHandler.hasRun());
        assertThat(task.getChannel()).isEqualTo(Channel.USER.getUri());
    }

    private void assertClosed(Task task) {
        assertEquals(TaskExecutionStateType.CLOSED, task.getExecutionState());
        assertEquals(TaskSchedulingStateType.CLOSED, task.getSchedulingState());
    }

    /**
     * Executes a cyclic task (tight binding).
     */
    @Test
    public void test130CycleTight() throws Exception {
        given();

        OperationResult result = createOperationResult();

        when();

        PrismObject<? extends ObjectType> object = add(TASK_CYCLE_TIGHT, result);
        displayDumpable("Added task", object);
        assertDeadPropertySanity(result, object); // A known problem with xsi:type

        waitForTaskProgress(TASK_CYCLE_TIGHT.oid, result, 10000, 500, 1);

        then();

        TaskQuartzImpl task = getTaskWithResult(TASK_CYCLE_TIGHT.oid, result);
        displayDumpable("Task after", task);

        PrismObject<TaskType> repoTask = repositoryService.getObject(TaskType.class, TASK_CYCLE_TIGHT.oid, null, result);
        displayDumpable("Task after (repo)", repoTask);

        assertThat(task.getExecutionState()).satisfiesAnyOf(
                value -> assertThat(value).isEqualTo(TaskExecutionStateType.RUNNABLE),
                value -> assertThat(value).isEqualTo(TaskExecutionStateType.RUNNING));
        assertThat(task.getSchedulingState()).isEqualTo(TaskSchedulingStateType.READY);

        assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp() == 0);
        assertNotNull("LastRunFinishTimestamp is null", task.getLastRunFinishTimestamp());
        assertFalse("LastRunFinishTimestamp is 0", task.getLastRunFinishTimestamp() == 0);

        assertTrue("Task progress is too small (should be at least 1)", task.getLegacyProgress() >= 1);
        assertSuccessOrInProgress(task);

        boolean stopped = taskStateManager.suspendTaskNoException(task, 2000, result);
        assertThat(stopped).as("Task was stopped").isTrue();
    }

    private void assertDeadPropertySanity(OperationResult result, PrismObject<? extends ObjectType> object)
            throws ObjectNotFoundException, SchemaException {
        PrismContainer<?> extensionContainer = object.getExtension();
        PrismProperty<Object> deadProperty = extensionContainer.findProperty(ITEM_DEAD);
        assertEquals("Bad type of 'dead' property (add result)", DOMUtil.XSD_INT, deadProperty.getDefinition().getTypeName());

        PrismObject<TaskType> repoTask = repositoryService.getObject(TaskType.class, TASK_CYCLE_TIGHT.oid, null, result);
        PrismContainer<?> extensionContainerRepo = repoTask.getExtension();
        PrismProperty<Object> deadPropertyRepo = extensionContainerRepo.findProperty(ITEM_DEAD);
        assertEquals("Bad type of 'dead' property (from repo)", DOMUtil.XSD_INT, deadPropertyRepo.getDefinition().getTypeName());
    }

    private void assertSuccessOrInProgress(Task task) {
        OperationResult taskResult = task.getResult();
        assertNotNull("Task result is null", taskResult);
        assertTrue("Task did not yield 'success' or 'inProgress' status: it is " + taskResult.getStatus(),
                taskResult.isSuccess() || taskResult.isInProgress());
    }

    /**
     * Tests cyclic task (loose binding)
     */
    @Test
    public void test140CycleLoose() throws Exception {
        given();

        OperationResult result = createOperationResult();

        when();

        add(TASK_CYCLE_LOOSE, result);
        waitForTaskProgress(TASK_CYCLE_LOOSE.oid, result, 15000, 500, 1);

        then();

        TaskQuartzImpl task = getTaskWithResult(TASK_CYCLE_LOOSE.oid, result);
        displayDumpable("Task after", task);

        assertThat(task.getExecutionState()).satisfiesAnyOf(
                value -> assertThat(value).isEqualTo(TaskExecutionStateType.RUNNABLE),
                value -> assertThat(value).isEqualTo(TaskExecutionStateType.RUNNING));
        assertThat(task.getSchedulingState()).isEqualTo(TaskSchedulingStateType.READY);

        assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);

        assertTrue("Progress is none or too small", task.getLegacyProgress() >= 1);
        assertTrue("Progress is too big (fault in scheduling?)", task.getLegacyProgress() <= 7);

        assertSuccessOrInProgress(task);

        taskStateManager.suspendTaskNoException(task, 100, result);
    }

    /**
     * Cyclic task with cron-based schedule.
     */
    @Test
    public void test150CycleLooseCron() throws Exception {
        given();

        OperationResult result = createOperationResult();

        when();

        add(TASK_CYCLE_LOOSE_CRON, result);
        waitForTaskProgress(TASK_CYCLE_LOOSE_CRON.oid, result, 15000, 500, 2);

        then();

        TaskQuartzImpl task = getTaskWithResult(TASK_CYCLE_LOOSE_CRON.oid, result);
        displayDumpable("Task after", task);

        assertThat(task.getExecutionState()).satisfiesAnyOf(
                value -> assertThat(value).isEqualTo(TaskExecutionStateType.RUNNABLE),
                value -> assertThat(value).isEqualTo(TaskExecutionStateType.RUNNING));
        assertThat(task.getSchedulingState()).isEqualTo(TaskSchedulingStateType.READY);

        assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);

        assertTrue("Task has not been executed at least twice", task.getLegacyProgress() >= 2);
        assertSuccessOrInProgress(task);

        taskStateManager.suspendTaskNoException(task, 100, result);
    }

    /**
     * Suspends a running task.
     */
    @Test
    public void test160Suspend() throws Exception {
        given();

        OperationResult result = createOperationResult();

        add(TASK_TO_SUSPEND, result);
        assertDelayExtensionProperty(TASK_TO_SUSPEND.oid, result);
        waitForTaskProgress(TASK_TO_SUSPEND.oid, result, 10000, 300, 1);
        TaskQuartzImpl task = getTaskWithResult(TASK_TO_SUSPEND.oid, result);
        displayDumpable("task after progress 1 reached", task);
        assertThat(task.getSchedulingState()).isEqualTo(TaskSchedulingStateType.READY);

        when();
        boolean stopped = taskStateManager.suspendTaskNoException(task, 0, result);

        then();

        task.refresh(result);
        displayDumpable("After suspend and refresh", task);

        assertTrue("Task is not stopped", stopped);
        assertSuspended(task);

        assertNotNull("Task last start time is null", task.getLastRunStartTimestamp());
        assertFalse("Task last start time is 0", task.getLastRunStartTimestamp() == 0);

        assertTrue("Task has not reported any progress", task.getLegacyProgress() > 0);
    }

    private void assertDelayExtensionProperty(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Task task = getTaskWithResult(oid, result);
        Object delay = task.getExtensionPropertyRealValue(MockTaskHandler.ITEM_DELAY);
        assertEquals("Delay was not read correctly", 2000, delay);
    }

    /**
     * Resumes (originally suspended) task and then suspends it again.
     */
    @Test
    public void test170ResumeAndSuspendLooselyBound() throws Exception {
        given();

        OperationResult result = createOperationResult();

        add(TASK_TO_RESUME_AND_SUSPEND, result);
        TaskQuartzImpl task = getTaskWithResult(TASK_TO_RESUME_AND_SUSPEND.oid, result);

        when("resume");

        taskManager.resumeTask(task, result);

        then("resume");

        waitForTaskProgress(TASK_TO_RESUME_AND_SUSPEND.oid, result, 10000, 300, 1);
        task.refresh(result);

        assertThat(task.getSchedulingState()).isEqualTo(TaskSchedulingStateType.READY);

        assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp() == 0);
        assertTrue(task.getLegacyProgress() > 0);

        when("suspend");

        // Task most probably does not run here, so we can have small wait time.
        boolean stopped = taskStateManager.suspendTaskNoException(task, 300, result);

        then("suspend");

        task.refresh(result);
        assertTrue("Task is not stopped", stopped);

        assertSuspended(task);

        assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);
        assertTrue(task.getLegacyProgress() > 0);
    }

    @Test
    public void test180SuspendLongRunning() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_LONG_RUNNING, result);
        TaskQuartzImpl task = getTaskWithResult(TASK_LONG_RUNNING.oid, result);
        waitForTaskStart(TASK_LONG_RUNNING.oid, result, 10000, 300);
        task.refresh(result);
        assertThat(task.getSchedulingState()).isEqualTo(TaskSchedulingStateType.READY);
        assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);

        when("Suspend short wait");

        boolean stopped = taskManager.suspendTask(task, 1000, result);

        then("Suspend short wait");

        task.refresh(result);
        assertFalse("Task is stopped (it should be running for now)", stopped);
        assertEquals("Task is not suspended", TaskSchedulingStateType.SUSPENDED, task.getSchedulingState());
        assertSuspended(task);

        assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNull(task.getLastRunFinishTimestamp());
        assertEquals("There should be no progress reported", 0, task.getLegacyProgress());

        when("Suspend long wait");

        stopped = taskStateManager.suspendTaskNoException(task, 0, result);

        then("Suspend long wait");

        task.refresh(result);
        assertTrue("Task is not stopped", stopped);
        assertSuspended(task);

        assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        assertNotNull("Last run finish time is null", task.getLastRunStartTimestamp());
        assertFalse("Last run finish time is zero", task.getLastRunStartTimestamp() == 0);

        // The task is not interruptible. It finishes the first run, not taking care of !canRun nor the interrupt signal.
        assertTrue("Progress is not reported", task.getLegacyProgress() > 0);
    }

    @Test
    public void test190DeleteTaskFromRepo() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_TO_DELETE, result);

        // Task is runnable, so it will be scheduled
        JobKey key = QuartzUtil.createJobKeyForTaskOid(TASK_TO_DELETE.oid);
        assertTrue("Job in Quartz does not exist", localScheduler.getQuartzScheduler().checkExists(key));

        when();

        repositoryService.deleteObject(TaskType.class, TASK_TO_DELETE.oid, result);

        then();

        // Task will be scheduled and then it will be detected that it no longer exists.
        waitFor("Waiting for the job to disappear from Quartz Job Store", new Checker() {
            public boolean check() {
                try {
                    return !localScheduler.getQuartzScheduler().checkExists(key);
                } catch (SchedulerException e) {
                    throw new SystemException(e);
                }
            }

            @Override
            public void timeout() {
            }
        }, 10000, 200);
    }

    /**
     * Waiting task is created. There's nothing that would kick it to run. Cluster management thread will do that.
     */
    @Test
    public void test200WaitingForNoOne() throws Exception {
        given();

        OperationResult result = createOperationResult();
        taskManager.getClusterManager().startClusterManagerThread();

        try {

            when();
            add(TASK_WAITING_FOR_NO_ONE, result);

            then();
            waitForTaskClose(TASK_WAITING_FOR_NO_ONE.oid, result, 40000);

        } finally {
            taskManager.getClusterManager().stopClusterManagerThread(10000L, result);
        }
    }

    /**
     * Checks handling of the task operation result.
     */
    @Test
    public void test210TaskResultHandling() throws Exception {
        given();

        OperationResult result = createOperationResult();

        Task task = taskManager.createTaskInstance();
        task.setInitiallySuspended();
        PrismObject<UserType> owner2 = repositoryService.getObject(UserType.class, TASK_OWNER2_OID, null, result);
        task.setOwner(owner2);
        assertEquals("Task result for new task is not correct", OperationResultStatus.UNKNOWN, task.getResult().getStatus());

        when();

        taskManager.switchToBackground(task, result);

        then();

        assertEquals("Background task result is not correct (in memory)", OperationResultStatus.IN_PROGRESS, task.getResult().getStatus());
        PrismObject<TaskType> task1 = repositoryService.getObject(TaskType.class, task.getOid(), retrieveItemsNamed(TaskType.F_RESULT), result);
        assertEquals("Background task result is not correct (in repo)", OperationResultStatusType.IN_PROGRESS, task1.asObjectable().getResult().getStatus());

        when("refresh");

        // now change task's result and check the refresh() method w.r.t. result handling
        task.getResult().recordFatalError("");
        assertEquals(OperationResultStatus.FATAL_ERROR, task.getResult().getStatus());
        task.refresh(result);

        then("refresh");
        assertEquals("Refresh does not update task's result", OperationResultStatus.IN_PROGRESS, task.getResult().getStatus());
    }

    /**
     * Very simple repo query test.
     */
    @Test
    public void test220QueryByExecutionStatus() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_SIMPLE_WAITING, result);

        when();

        ObjectFilter filter1 = prismContext.queryFor(TaskType.class).item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.WAITING).buildFilter();
        ObjectFilter filter2 = prismContext.queryFor(TaskType.class).item(TaskType.F_WAITING_REASON).eq(TaskWaitingReasonType.OTHER).buildFilter();
        ObjectFilter filter3 = prismContext.queryFactory().createAnd(filter1, filter2);

        List<PrismObject<TaskType>> prisms1 = repositoryService.searchObjects(TaskType.class, prismContext.queryFactory().createQuery(filter1), null, result);
        List<PrismObject<TaskType>> prisms2 = repositoryService.searchObjects(TaskType.class, prismContext.queryFactory().createQuery(filter2), null, result);
        List<PrismObject<TaskType>> prisms3 = repositoryService.searchObjects(TaskType.class, prismContext.queryFactory().createQuery(filter3), null, result);

        then();

        assertFalse("There were no tasks with executionStatus == WAITING found", prisms1.isEmpty());
        assertFalse("There were no tasks with waitingReason == OTHER found", prisms2.isEmpty());
        assertFalse("There were no tasks with executionStatus == WAITING and waitingReason == OTHER found", prisms3.isEmpty());
    }

    @Test
    public void test230DeleteTaskTree() throws Exception {
        given();

        OperationResult result = createOperationResult();

        PrismObject<TaskType> parentTaskPrism = add(TASK_TREE_ROOT, result); // waiting
        PrismObject<TaskType> childTask1Prism = add(TASK_TREE_CHILD_1, result); // suspended
        PrismObject<TaskType> childTask2Prism = add(TASK_TREE_CHILD_2, result); // suspended

        assertEquals(TaskExecutionStateType.RUNNING, parentTaskPrism.asObjectable().getExecutionState());
        assertEquals(TaskSchedulingStateType.WAITING, parentTaskPrism.asObjectable().getSchedulingState());
        assertEquals(TaskExecutionStateType.SUSPENDED, childTask1Prism.asObjectable().getExecutionState());
        assertEquals(TaskExecutionStateType.SUSPENDED, childTask2Prism.asObjectable().getExecutionState());

        Task parentTask = taskManager.createTaskInstance(parentTaskPrism, result);
        Task childTask1 = taskManager.createTaskInstance(childTask1Prism, result);
        Task childTask2 = taskManager.createTaskInstance(childTask2Prism, result);

        displayDumpable("parent", parentTask);
        displayDumpable("child1", childTask1);
        displayDumpable("child2", childTask2);

        when();

        taskManager.resumeTask(childTask1, result);
        taskManager.resumeTask(childTask2, result);

        taskManager.suspendAndDeleteTasks(Collections.singletonList(parentTask.getOid()), 2000L, true, result);

        IntegrationTestTools.display("after suspendAndDeleteTasks", result.getLastSubresult());
        TestUtil.assertSuccessOrWarning("suspendAndDeleteTasks result is not success/warning", result.getLastSubresult());

        try {
            repositoryService.getObject(TaskType.class, childTask1.getOid(), null, result);
            fail("Task " + childTask1 + " was not deleted from the repository");
        } catch (ObjectNotFoundException e) {
            // ok!
        }

        try {
            repositoryService.getObject(TaskType.class, childTask2.getOid(), null, result);
            fail("Task " + childTask2 + " was not deleted from the repository");
        } catch (ObjectNotFoundException e) {
            // ok!
        }

        try {
            repositoryService.getObject(TaskType.class, parentTask.getOid(), null, result);
            fail("Task " + parentTask + " was not deleted from the repository");
        } catch (ObjectNotFoundException e) {
            // ok!
        }
    }

    /**
     * Recurring with no schedule: running on demand.
     */
    @Test
    public void test240RunOnDemand() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_RUN_ON_DEMAND, result);

        TaskQuartzImpl task = getTaskWithResult(TASK_RUN_ON_DEMAND.oid, result);

        System.out.println("Waiting to see if the task would not start...");
        Thread.sleep(2000L);

        // check the task HAS NOT started
        task.refresh(result);

        assertEquals("task is not READY", TaskSchedulingStateType.READY, task.getSchedulingState());
        assertNull("task was started", task.getLastRunStartTimestamp());
        assertEquals("task was achieved some progress", 0L, task.getLegacyProgress());

        when();

        taskStateManager.scheduleTaskNow(task, result);

        then();

        waitForTaskProgress(TASK_RUN_ON_DEMAND.oid, result, 10000, 300, 1);
        task.refresh(result);

        assertEquals(TaskSchedulingStateType.READY, task.getSchedulingState());
        assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp() == 0);
        assertTrue("no progress", task.getLegacyProgress() > 0);

        // cleaning up
        boolean stopped = taskManager.suspendTask(task, 10000L, result);
        task.refresh(result);
        assertTrue("Task is not stopped", stopped);
        assertSuspended(task);

        // This is written after progress is updated. So it is safe to assert it after the task is stopped.
        assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);
    }

    /**
     * Tests task with lightweight subtasks.
     */
    @Test
    public void test250TaskWithThreads() throws Exception {
        given();

        OperationResult result = createOperationResult();

        when();

        add(TASK_WITH_THREADS, result);

        checkTreadSafety(TASK_WITH_THREADS.oid, 1000L, result);

        waitUntilDone(TASK_WITH_THREADS.oid, result, 15000, 100);
        waitForTaskClose(TASK_WITH_THREADS.oid, result, 15000);

        then();

        Task task = getTaskWithResult(TASK_WITH_THREADS.oid, result);
        displayDumpable("Task after", task);

        Collection<? extends RunningLightweightTask> subtasks = mockParallelTaskHandler.getLastTaskExecuted().getLightweightAsynchronousSubtasks();
        assertEquals("Wrong number of subtasks", MockParallelTaskHandler.NUM_SUBTASKS, subtasks.size());
        for (RunningLightweightTask subtask : subtasks) {
            assertEquals("Wrong subtask state", TaskExecutionStateType.CLOSED, subtask.getExecutionState());
            MockParallelTaskHandler.MyLightweightTaskHandler handler = (MockParallelTaskHandler.MyLightweightTaskHandler) subtask.getLightweightTaskHandler();
            assertTrue("Handler has not run in " + subtask, handler.hasRun());
            assertTrue("Handler has not exited in " + subtask, handler.hasExited());
        }
    }

    /**
     * A simple test for MID-6910.
     */
    @SuppressWarnings("SameParameterValue")
    private void checkTreadSafety(String oid, long duration, OperationResult result)
            throws CommonException, InterruptedException {

        PrismObject<TaskType> retrievedTask;
        long start = System.currentTimeMillis();
        for (;;) {
            Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                    .item(TaskType.F_SUBTASK_REF).retrieve()
                    .build();
            retrievedTask = taskManager.getObject(TaskType.class, oid, options, result);
            if (!retrievedTask.asObjectable().getSubtaskRef().isEmpty()) {
                break;
            }
            if (System.currentTimeMillis() - start > 3000) {
                throw new AssertionError("Timed out waiting for the task to create subtasks");
            }
            //noinspection BusyWait
            Thread.sleep(200);
        }

        display("Subtasks: " + retrievedTask.asObjectable().getSubtaskRef().size());
        long startCloning = System.currentTimeMillis();
        int cloneIterations = 0;
        while (System.currentTimeMillis() - startCloning < duration) {
            retrievedTask.clone();
            cloneIterations++;
        }
        display("Clone iterations done: " + cloneIterations);
    }

    @SuppressWarnings("SameParameterValue")
    private void waitUntilDone(String taskOid, OperationResult result, int duration, int checkInterval)
            throws SchemaException, ObjectNotFoundException, InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + duration) {
            Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                    .item(TaskType.F_SUBTASK_REF).retrieve()
                    .build();
            TaskType task = taskManager.getObject(TaskType.class, taskOid, options, result).asObjectable();
            System.out.println((System.currentTimeMillis() - start) + ": subtasks: " + task.getSubtaskRef().size() +
                    ", progress = " + task.getProgress());
            if (task.getSchedulingState() != TaskSchedulingStateType.READY) {
                System.out.println("Done. Status = " + task.getExecutionState());
                break;
            }
            //noinspection BusyWait
            Thread.sleep(checkInterval);
        }
    }

    @Test
    public void test260LightweightSubtasksSuspension() throws Exception {
        given();

        OperationResult result = createOperationResult();

        when();

        add(TASK_WITH_THREADS_TO_SUSPEND, result);

        waitForTaskStart(TASK_WITH_THREADS_TO_SUSPEND.oid, result, 15000, 500);

        TaskQuartzImpl task = getTaskWithResult(TASK_WITH_THREADS_TO_SUSPEND.oid, result);
        displayDumpable("taskAfterStart", task);
        //assertEquals(TaskExecutionStateType.RUNNING, task.getExecutionState()); // todo
        assertEquals(TaskSchedulingStateType.READY, task.getSchedulingState());

        // check the thread
        JobExecutionContext found = findJobForTask(task);
        JobExecutor executor = (JobExecutor) found.getJobInstance();
        assertNotNull("No job executor", executor);
        Thread thread = executor.getExecutingThread();
        assertNotNull("No executing thread", thread);

        // now let us suspend it - the handler should stop, as well as the subtasks
        boolean stopped = taskManager.suspendTask(task, 10000L, result);

        then();
        task.refresh(result);
        assertTrue("Task is not stopped", stopped);

        assertSuspended(task);

        Collection<? extends RunningLightweightTask> subtasks = mockParallelTaskHandler.getLastTaskExecuted().getLightweightAsynchronousSubtasks();
        assertEquals("Wrong number of subtasks", MockParallelTaskHandler.NUM_SUBTASKS, subtasks.size());
        for (RunningLightweightTask subtask : subtasks) {
            assertEquals("Wrong subtask state", TaskExecutionStateType.CLOSED, subtask.getExecutionState());
            MockParallelTaskHandler.MyLightweightTaskHandler handler = (MockParallelTaskHandler.MyLightweightTaskHandler) subtask.getLightweightTaskHandler();
            assertTrue("Handler has not run", handler.hasRun());
            assertTrue("Handler has not exited", handler.hasExited());
        }
    }

    @NotNull
    private JobExecutionContext findJobForTask(TaskQuartzImpl task) throws SchedulerException {
        List<JobExecutionContext> jobExecutionContexts = localScheduler.getQuartzScheduler().getCurrentlyExecutingJobs();
        JobExecutionContext found = null;
        for (JobExecutionContext jobExecutionContext : jobExecutionContexts) {
            if (task.getOid().equals(jobExecutionContext.getJobDetail().getKey().getName())) {
                found = jobExecutionContext;
                break;
            }
        }
        assertNotNull("Job for the task was not found", found);
        return found;
    }

    /**
     * Tests the secondary group limits (three tasks that cannot run at once).
     */
    @Test
    public void test300SecondaryGroupLimit() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_SEC_GROUP_LIMIT_EXP_1, result);
        waitForTaskStart(TASK_SEC_GROUP_LIMIT_EXP_1.oid, result, 10000, 200);

        when();

        // import second task with the same group (expensive)
        add(TASK_SEC_GROUP_LIMIT_EXP_1_RAM_1, result);

        then();

        Thread.sleep(1000);
        TaskType task1 = getTaskType(TASK_SEC_GROUP_LIMIT_EXP_1.oid, result);
        assertNull("First task should have no retry time", task1.getNextRetryTimestamp());

        TaskType task2 = getTaskType(TASK_SEC_GROUP_LIMIT_EXP_1_RAM_1.oid, result);
        assertNull("Second task was started even if it should not be", task2.getLastRunStartTimestamp());
        assertNextRetryTimeSet(task2, result);

        when("stop 1st task");

        // now finish first task and check the second one is started
        boolean stopped = taskManager.suspendTask(task1.getOid(), 20000L, result);

        then("stop 1st task");

        assertTrue("Task 1 was not suspended successfully", stopped);
        waitForTaskStart(task2.getOid(), result, 10000, 500);

        when("import 3rd task");

        // import third task that has another collision (large-ram) with the second one
        add(TASK_SEC_GROUP_LIMIT_RAM_NULL, result);

        then("import 3rd task");

        Thread.sleep(10000);
        task2 = getTaskType(task2.getOid(), result);
        assertNull("Second task should have no retry time", task2.getNextRetryTimestamp());

        TaskType task3 = getTaskType(TASK_SEC_GROUP_LIMIT_RAM_NULL.oid, result);
        assertNull("Third task was started even if it should not be", task3.getLastRunStartTimestamp());
        assertNextRetryTimeSet(task3, result);

        when("stop 2nd task");

        // now finish second task and check the third one is started
        stopped = taskManager.suspendTask(task2.getOid(), 20000L, result);

        then("stop 2nd task");

        assertTrue("Task 2 was not suspended successfully", stopped);
        waitForTaskStart(task3.getOid(), result, 10000, 500);

        taskManager.suspendTask(task3.getOid(), 20000L, result);
    }

    private void assertNextRetryTimeSet(TaskType task, OperationResult result)
            throws InterruptedException, SchemaException, ObjectNotFoundException {
        // this one may occasionally fail because of a race condition (nextRetryTimestamp is derived from quartz scheduling data;
        // and if the task is just being rescheduled because of a group limitation it might be temporarily null)
        // -- so if this is the case we check a little later
        if (task.getNextRetryTimestamp() == null) {
            Thread.sleep(1000L);
            task = getTaskType(task.getOid(), result);
            assertNull("Second task was started even if it should not be", task.getLastRunStartTimestamp());
            assertNotNull("Next retry time is not set for second task", task.getNextRetryTimestamp());
        }
    }

    @Test
    public void test310GroupLimit() throws Exception {
        given();

        OperationResult result = createOperationResult();

        localScheduler.setLocalExecutionLimitations(null);
        add(TASK_GROUP_LIMIT, result);
        waitForTaskStart(TASK_GROUP_LIMIT.oid, result, 10000, 200);

        when();

        // import second task with the same group
        add(TASK_GROUP_LIMIT_CONCURRENT, result);

        then();

        Thread.sleep(1000);
        TaskType task1 = getTaskType(TASK_GROUP_LIMIT.oid, result);
        assertNull("First task should have no retry time", task1.getNextRetryTimestamp());

        TaskType task2 = getTaskType(TASK_GROUP_LIMIT_CONCURRENT.oid, result);
        assertNull("Second task was started even if it should not be", task2.getLastRunStartTimestamp());
        assertNextRetryTimeSet(task2, result);

        when("stop 1st task");

        // now finish first task and check the second one is started
        boolean stopped = taskManager.suspendTask(task1.getOid(), 20000L, result);

        then("stop 1st task");

        assertTrue("Task 1 was not suspended successfully", stopped);
        waitForTaskStart(task2.getOid(), result, 10000, 200);
        taskManager.suspendTask(task2.getOid(), 20000L, result);
    }

    private TaskType getTaskType(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Collection<SelectorOptions<GetOperationOptions>> options = retrieveItemsNamed(
                TaskType.F_SUBTASK_REF,
                TaskType.F_NODE_AS_OBSERVED,
                TaskType.F_NEXT_RUN_START_TIMESTAMP,
                TaskType.F_NEXT_RETRY_TIMESTAMP);
        return taskManager.getObject(TaskType.class, oid, options, result).asObjectable();
    }

    @Test
    public void test320NodeAllowed() throws Exception {
        given();

        OperationResult result = createOperationResult();

        localScheduler.setLocalExecutionLimitations(
                new TaskExecutionLimitationsType()
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName("lightweight-tasks"))
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName(null))
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName(TaskConstants.LIMIT_FOR_OTHER_GROUPS).limit(0)));

        when();

        add(TASK_ALLOWED, result);

        then();

        waitForTaskStart(TASK_ALLOWED.oid, result, 10000, 200);
        TaskQuartzImpl task = getTaskWithResult(TASK_ALLOWED.oid, result);
        assertNotNull("Task was not started even if it should be", task.getLastRunStartTimestamp());
    }

    @Test
    public void test330NodeNotAllowed() throws Exception {
        given();

        OperationResult result = createOperationResult();

        when();

        add(TASK_ALLOWED_NOT, result);

        then();

        Thread.sleep(3000);
        TaskQuartzImpl task = getTaskWithResult(TASK_ALLOWED_NOT.oid, result);
        assertNull("Task was started even if it shouldn't be", task.getLastRunStartTimestamp());
        taskManager.suspendTask(task.getOid(), 1000L, result);
    }

    @Test
    public void test400RetrieveSubtasks() throws Exception {
        given();

        OperationResult result = createOperationResult();

        String rootOid = add(TASK_SUSPENDED_TREE_ROOT, result).getOid();
        String child1Oid = add(TASK_SUSPENDED_TREE_CHILD_1, result).getOid();
        String child11Oid = add(TASK_SUSPENDED_TREE_CHILD_1_1, result).getOid();
        String child2Oid = add(TASK_SUSPENDED_TREE_CHILD_2, result).getOid();

        Collection<SelectorOptions<GetOperationOptions>> withChildren = schemaService.getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();

        when();

        TaskType getTaskPlain_rootNoChildren = taskManager.getTaskPlain(rootOid, null, result).getUpdatedTaskObject().asObjectable();
        TaskType getTaskPlain_rootWithChildren = taskManager.getTaskPlain(rootOid, withChildren, result).getUpdatedTaskObject().asObjectable(); // no children will be fetched
        TaskType getTask_rootNoChildren = taskManager.getTask(rootOid, null, result).getUpdatedTaskObject().asObjectable();
        TaskType getTask_rootWithChildren = taskManager.getTask(rootOid, withChildren, result).getUpdatedTaskObject().asObjectable();
        TaskType getObject_rootNoChildren = taskManager.getObject(TaskType.class, rootOid, null, result).asObjectable();
        TaskType getObject_rootWithChildren = taskManager.getObject(TaskType.class, rootOid, withChildren, result).asObjectable();
        ObjectQuery query = prismContext.queryFor(TaskType.class).id(rootOid).build();

        then();

        assertEquals("Wrong # of children", 0, getTaskPlain_rootNoChildren.getSubtaskRef().size());
        assertEquals("Wrong # of children", 0, getTaskPlain_rootWithChildren.getSubtaskRef().size());

        assertEquals("Wrong # of children", 0, getTask_rootNoChildren.getSubtaskRef().size());
        assertTaskTree(getTask_rootWithChildren, child1Oid, child2Oid, child11Oid);

        assertEquals("Wrong # of children", 0, getObject_rootNoChildren.getSubtaskRef().size());
        assertTaskTree(getObject_rootWithChildren, child1Oid, child2Oid, child11Oid);

        TaskType searchObjects_rootNoChildren = extractSingleton(taskManager.searchObjects(TaskType.class, query, null, result)).asObjectable();
        TaskType searchObjects_rootWithChildren = extractSingleton(taskManager.searchObjects(TaskType.class, query, withChildren, result)).asObjectable();
        assertEquals("Wrong # of children", 0, searchObjects_rootNoChildren.getSubtaskRef().size());
        assertTaskTree(searchObjects_rootWithChildren, child1Oid, child2Oid, child11Oid);
    }

    @Test
    public void test410CheckResultStatus() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_DUMMY, result);

        when();

        PrismObject<TaskType> dummy = taskManager.getObject(TaskType.class, TASK_DUMMY.oid, null, result);

        then();

        assertEquals("Wrong result status", OperationResultStatusType.SUCCESS, dummy.asObjectable().getResultStatus());
    }

    /**
     * Non-existing owner should be treated gracefully: MID-6918.
     */
    @Test
    public void test420NonExistingOwner() throws Exception {
        given();

        OperationResult result = createOperationResult();
        add(TASK_NON_EXISTING_OWNER, result);

        when();

        waitForTaskSuspend(TASK_NON_EXISTING_OWNER.oid, result, 10000, 200);

        Task task = taskManager.getTaskWithResult(TASK_NON_EXISTING_OWNER.oid, result);

        then();

        displayDumpable("task", task);
        assertThat(task.getResult().getMessage())
                .as("operation result message")
                .isEqualTo("Task owner couldn't be resolved: 8e6943e0-848d-4bf0-8c9e-4ba6bdf6c518");
    }

    private void assertTaskTree(TaskType rootWithChildren, String child1Oid, String child2Oid, String child11Oid) {
        assertEquals("Wrong # of children of root", 2, rootWithChildren.getSubtaskRef().size());
        TaskType child1 = TaskTreeUtil.findChild(rootWithChildren, child1Oid);
        TaskType child2 = TaskTreeUtil.findChild(rootWithChildren, child2Oid);
        assertNotNull(child1);
        assertNotNull(child2);
        assertEquals("Wrong # of children of child1", 1, child1.getSubtaskRef().size());
        assertEquals("Wrong # of children of child2", 0, child2.getSubtaskRef().size());
        TaskType child11 = TaskTreeUtil.findChild(child1, child11Oid);
        assertNotNull(child11);
    }

    @NotNull private TaskQuartzImpl getTaskWithResult(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return taskManager.getTaskWithResult(oid, result);
    }

    private void assertSuspended(TaskQuartzImpl task) {
        assertEquals("Task is not suspended (execution state)", TaskExecutionStateType.SUSPENDED, task.getExecutionState()); // todo or suspending?
        assertEquals("Task is not suspended (scheduling state)", TaskSchedulingStateType.SUSPENDED, task.getSchedulingState());
    }
}
