/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
import static com.evolveum.midpoint.util.MiscUtil.extractSingleton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.PostConstruct;

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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskConstants;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterManager;
import com.evolveum.midpoint.task.quartzimpl.execution.JobExecutor;
import com.evolveum.midpoint.test.Checker;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskManagerContract extends AbstractTaskManagerTest {

    private static final String TASK_OWNER_FILENAME = "src/test/resources/basic/owner.xml";
    private static final String TASK_OWNER2_FILENAME = "src/test/resources/basic/owner2.xml";
    private static final String TASK_OWNER2_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
    private static final String NS_WHATEVER = "http://myself.me/schemas/whatever";

    private String taskFilename(String fileSuffix) {
        return "src/test/resources/basic/task-" + getTestNameShort().substring(4) + fileSuffix + ".xml";
    }

    private String taskFilename() {
        return taskFilename("");
    }

    private String taskOid(String testNumber, String subId) {
        return "91919191-76e0-59e2-86d6-55665566" + subId + testNumber;
    }

    private String taskOid(String subId) {
        return taskOid(getTestNumber(), subId);
    }

    private String taskOid() {
        return taskOid("0");
    }

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
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }

    /**
     * Here we only test setting various task properties.
     */

    @Test
    public void test003GetProgress() throws Exception {
        OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        logger.trace("Retrieving the task and getting its progress...");

        TaskQuartzImpl task = getTaskWithResult(taskOid(), result);
        AssertJUnit.assertEquals("Progress is not 0", 0, task.getProgress());
    }

    @Test
    public void test004TaskBigProperty() throws Exception {
        OperationResult result = createOperationResult();

        String string300 = "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-";
        String string300a = "AAAAAAAAA-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-"
                + "123456789-123456789-123456789-123456789-123456789-";

        addObjectFromFile(taskFilename());

        TaskQuartzImpl task = getTaskWithResult(taskOid(), result);

        // property definition
        ItemName shipStateQName = new ItemName("http://myself.me/schemas/whatever", "shipState");
        //noinspection unchecked
        PrismPropertyDefinition<String> shipStateDefinition = (PrismPropertyDefinition<String>)
                prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(shipStateQName);
        assertNotNull("Cannot find property definition for shipState", shipStateDefinition);

        PrismProperty<String> shipStateProperty = shipStateDefinition.instantiate();
        shipStateProperty.setRealValue(string300);
        task.setExtensionProperty(shipStateProperty);

        task.flushPendingModifications(result);

        System.out.println("1st round: Task = " + task.debugDump());

        logger.trace("Retrieving the task and comparing its properties...");

        Task task001 = getTaskWithResult(taskOid(), result);
        System.out.println("1st round: Task from repo: " + task001.debugDump());

        PrismProperty<String> shipState001 = task001.getExtensionPropertyOrClone(shipStateQName);
        assertEquals("Big string not retrieved correctly (1st round)", shipStateProperty.getRealValue(), shipState001.getRealValue());

        // second round

        shipStateProperty.setRealValue(string300a);
        task001.setExtensionProperty(shipStateProperty);

        System.out.println("2nd round: Task before save = " + task001.debugDump());
        task001.flushPendingModifications(result);

        Task task002 = getTaskWithResult(taskOid(), result);
        System.out.println("2nd round: Task from repo: " + task002.debugDump());

        PrismProperty<String> bigString002 = task002.getExtensionPropertyOrClone(shipStateQName);
        assertEquals("Big string not retrieved correctly (2nd round)", shipStateProperty.getRealValue(), bigString002.getRealValue());
    }

    /*
     * Execute a single-run task.
     */

    @Test
    public void test005Single() throws Exception {
        final OperationResult result = createOperationResult();

        // reset 'has run' flag on the handler
        singleHandler1.resetHasRun();

        // Add single task. This will get picked by task scanner and executed
        addObjectFromFile(taskFilename());

        logger.trace("Retrieving the task...");
        TaskQuartzImpl task = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task);
        logger.trace("Task retrieval OK.");

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task

        waitForTaskClose(taskOid(), result, 10000, 1000);

        logger.info("... done");

        // Check task status

        Task task1 = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task1);
        System.out.println("getTask returned: " + task1.debugDump());

        PrismObject<TaskType> po = repositoryService.getObject(TaskType.class, taskOid(), null, result);
        System.out.println("getObject returned: " + po.debugDump());

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task1.getExecutionStatus());

        assertNotNull(task1.getCompletionTimestamp());
        List<TriggerType> triggers = task1.getUpdatedTaskObject().asObjectable().getTrigger();
        assertEquals(1, triggers.size());
        TriggerType trigger = triggers.get(0);
        long delta = XmlTypeConverter.toMillis(trigger.getTimestamp()) - task1.getCompletionTimestamp();
        if (Math.abs(delta - 10000) > 1000) {
            fail("Auto cleanup timestamp was not computed correctly. Delta should be 10000, is " + delta);
        }

        // .. and released
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task1.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task1.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task1.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull("LastRunFinishTimestamp is null", task1.getLastRunFinishTimestamp());
        assertFalse("LastRunFinishTimestamp is 0", task1.getLastRunFinishTimestamp() == 0);

        // The progress should be more than 0 as the task has run at least once
        AssertJUnit.assertTrue("Task reported no progress", task1.getProgress() > 0);

        // Test for presence of a result. It should be there and it should
        // indicate success
        assertSuccess(task1);

        // Test for no presence of handlers
        //AssertJUnit.assertNull("Handler is still present", task1.getHandlerUri());
        AssertJUnit.assertNotNull("Handler is gone", task1.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present",
                task1.getOtherHandlersUriStack() == null || task1.getOtherHandlersUriStack().getUriStackEntry().isEmpty());

        // Test whether handler has really run
        AssertJUnit.assertTrue("Handler1 has not run", singleHandler1.hasRun());
    }

    /*
     * Executes a cyclic task
     */

    @Test
    public void test006Cycle() throws Exception {
        final OperationResult result = createOperationResult();

        // But before that check sanity ... a known problem with xsi:type
        PrismObject<? extends ObjectType> object = addObjectFromFile(taskFilename());

        ObjectType objectType = object.asObjectable();
        TaskType addedTask = (TaskType) objectType;
        System.out.println("Added task");
        System.out.println(object.debugDump());

        PrismContainer<?> extensionContainer = object.getExtension();
        PrismProperty<Object> deadProperty = extensionContainer.findProperty(new ItemName(NS_WHATEVER, "dead"));
        assertEquals("Bad typed of 'dead' property (add result)", DOMUtil.XSD_INT, deadProperty.getDefinition().getTypeName());

        // Read from repo

        PrismObject<TaskType> repoTask = repositoryService.getObject(TaskType.class, addedTask.getOid(), null, result);

        extensionContainer = repoTask.getExtension();
        deadProperty = extensionContainer.findProperty(new ItemName(NS_WHATEVER, "dead"));
        assertEquals("Bad typed of 'dead' property (from repo)", DOMUtil.XSD_INT, deadProperty.getDefinition().getTypeName());

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this
        // task
        waitForTaskProgress(taskOid(), result, 10000, 2000, 1);

        // Check task status

        Task task = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> t = repositoryService.getObject(TaskType.class, taskOid(), null, result);
        System.out.println(t.debugDump());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // .. and claimed
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull("LastRunFinishTimestamp is null", task.getLastRunFinishTimestamp());
        assertFalse("LastRunFinishTimestamp is 0", task.getLastRunFinishTimestamp() == 0);

        // The progress should be more at least 1 - so small because of lazy testing machine ... (wait time before task runs is 2 seconds)
        AssertJUnit.assertTrue("Task progress is too small (should be at least 1)", task.getProgress() >= 1);

        // Test for presence of a result. It should be there and it should
        // indicate success
        assertSuccess(task);

        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTaskQuietly(task, 100, result);

    }

    private void assertSuccess(Task task) {
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' status: it is " + taskResult.getStatus(), taskResult.isSuccess());
    }

    private void assertSuccessOrInProgress(Task task) {
        OperationResult taskResult = task.getResult();
        AssertJUnit.assertNotNull("Task result is null", taskResult);
        AssertJUnit.assertTrue("Task did not yield 'success' or 'inProgress' status: it is " + taskResult.getStatus(),
                taskResult.isSuccess() || taskResult.isInProgress());
    }

    /*
     * Single-run task with more handlers.
     */

    @Test
    public void test008MoreHandlers() throws Exception {
        final OperationResult result = createOperationResult();

        // reset 'has run' flag on handlers
        singleHandler1.resetHasRun();
        singleHandler2.resetHasRun();
        singleHandler3.resetHasRun();

        addObjectFromFile(taskFilename());

        waitForTaskClose(taskOid(), result, 15000, 2000);

        // Check task status

        Task task = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, taskOid(), null, result);
        System.out.println(ObjectTypeUtil.dump(o.getValue().getValue()));

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        // .. and released
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull("Last run finish timestamp not set", task.getLastRunFinishTimestamp());
        assertFalse("Last run finish timestamp is 0", task.getLastRunFinishTimestamp() == 0);

        // The progress should be more than 0 as the task has run at least once
        AssertJUnit.assertTrue("Task reported no progress", task.getProgress() > 0);

        // Test for presence of a result. It should be there and it should
        // indicate success
        assertSuccess(task);

        // Test for no presence of handlers

        AssertJUnit.assertNotNull("Handler is gone", task.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present",
                task.getOtherHandlersUriStack() == null || task.getOtherHandlersUriStack().getUriStackEntry().isEmpty());

        // Test if all three handlers were run

        AssertJUnit.assertTrue("Handler1 has not run", singleHandler1.hasRun());
        AssertJUnit.assertTrue("Handler2 has not run", singleHandler2.hasRun());
        AssertJUnit.assertTrue("Handler3 has not run", singleHandler3.hasRun());
    }

    @Test
    public void test009CycleLoose() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this task

        waitForTaskProgress(taskOid(), result, 15000, 2000, 1);

        // Check task status

        Task task = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> t = repositoryService.getObject(TaskType.class, taskOid(), null, result);
        System.out.println(t.debugDump());

        // .. it should be running
        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);

        // The progress should be more at least 1 - lazy neptunus... (wait time before task runs is 2 seconds)
        AssertJUnit.assertTrue("Progress is none or too small", task.getProgress() >= 1);

        // The progress should not be too big (indicates fault in scheduling)
        AssertJUnit.assertTrue("Progress is too big (fault in scheduling?)", task.getProgress() <= 7);

        // Test for presence of a result. It should be there and it should
        // indicate success or in-progress
        assertSuccessOrInProgress(task);

        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTaskQuietly(task, 100, result);
    }

    @Test
    public void test010CycleCronLoose() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        waitForTaskProgress(taskOid(), result, 15000, 2000, 2);

        // Check task status

        Task task = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        TaskType t = repositoryService.getObject(TaskType.class, taskOid(), null, result).getValue().getValue();
        System.out.println(ObjectTypeUtil.dump(t));

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);

        // The progress should be at least 2 as the task has run at least twice
        AssertJUnit.assertTrue("Task has not been executed at least twice", task.getProgress() >= 2);

        // Test for presence of a result. It should be there and it should
        // indicate success
        assertSuccessOrInProgress(task);

        // Suspend the task (in order to keep logs clean), without much waiting
        taskManager.suspendTaskQuietly(task, 100, result);
    }

    @Test
    public void test011MoreHandlersAndSchedules() throws Exception {
        final OperationResult result = createOperationResult();

        // reset 'has run' flag on handlers
        l1Handler.resetHasRun();
        l2Handler.resetHasRun();
        l3Handler.resetHasRun();

        addObjectFromFile(taskFilename());

        waitForTaskClose(taskOid(), result, 30000, 2000);

        // Check task status

        Task task = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, taskOid(), null, result);
        System.out.println(ObjectTypeUtil.dump(o.getValue().getValue()));

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull("Last run finish timestamp not set", task.getLastRunFinishTimestamp());
        assertFalse("Last run finish timestamp is 0", task.getLastRunFinishTimestamp() == 0);

        /*
         * Here the execution should be as follows:
         *   progress: 0->1 on first execution of L1 handler
         *   progress: 1->2 on first execution of L2 handler (ASAP after finishing L1)
         *   progress: 2->3 on second execution of L2 handler (2 seconds later)
         *   progress: 3->4 on third execution of L2 handler (2 seconds later)
         *   progress: 4->5 on fourth execution of L2 handler (2 seconds later)
         *   progress: 5->6 on first (and therefore last) execution of L3 handler
         *   progress: 6->7 on last execution of L2 handler (2 seconds later, perhaps)
         *   progress: 7->8 on last execution of L1 handler
         */

        AssertJUnit.assertEquals("Task reported wrong progress", 8, task.getProgress());

        // Test for presence of a result. It should be there and it should
        // indicate success
        assertSuccess(task);

        // Test for no presence of handlers

        AssertJUnit.assertNotNull("Handler is gone", task.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present",
                task.getOtherHandlersUriStack() == null || task.getOtherHandlersUriStack().getUriStackEntry().isEmpty());

        // Test if all three handlers were run

        AssertJUnit.assertTrue("L1 handler has not run", l1Handler.hasRun());
        AssertJUnit.assertTrue("L2 handler has not run", l2Handler.hasRun());
        AssertJUnit.assertTrue("L3 handler has not run", l3Handler.hasRun());
    }

    /*
     * Suspends a running task.
     */

    @Test
    public void test012Suspend() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        // check if we can read the extension (xsi:type issue)

        Task taskTemp = getTaskWithResult(taskOid(), result);
        PrismProperty<?> delay = taskTemp.getExtensionPropertyOrClone(SchemaConstants.NOOP_DELAY_QNAME);
        AssertJUnit.assertEquals("Delay was not read correctly", 2000, delay.getRealValue());

        waitForTaskProgress(taskOid(), result, 10000, 2000, 1);

        // Check task status (task is running 5 iterations where each takes 2000 ms)

        Task task = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        AssertJUnit.assertEquals("Task is not running", TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // Now suspend the task

        boolean stopped = taskManager.suspendTaskQuietly(task, 0, result);

        task.refresh(result);
        System.out.println("After suspend and refresh: " + task.debugDump());

        AssertJUnit.assertTrue("Task is not stopped", stopped);
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());

        AssertJUnit.assertNotNull("Task last start time is null", task.getLastRunStartTimestamp());
        assertFalse("Task last start time is 0", task.getLastRunStartTimestamp() == 0);

        // The progress should be more than 0
        AssertJUnit.assertTrue("Task has not reported any progress", task.getProgress() > 0);

//        Thread.sleep(200);        // give the scheduler a chance to release the task

//        task.refresh(result);
//        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
    }

    @Test
    public void test013ReleaseAndSuspendLooselyBound() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        Task task = getTaskWithResult(taskOid(), result);
        System.out.println("After setup: " + task.debugDump());

        // check if we can read the extension (xsi:type issue)

        PrismProperty<?> delay = task.getExtensionPropertyOrClone(SchemaConstants.NOOP_DELAY_QNAME);
        AssertJUnit.assertEquals("Delay was not read correctly", 1000, delay.getRealValue());

        // let us resume (i.e. start the task)
        taskManager.resumeTask(task, result);

        // task is executing for 1000 ms, so we need to wait slightly longer, in order for the execution to be done
        waitForTaskProgress(taskOid(), result, 10000, 2000, 1);

        task.refresh(result);

        System.out.println("After refresh: " + task.debugDump());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());        // task cycle is 1000 ms, so it should be released now

        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);
        AssertJUnit.assertTrue(task.getProgress() > 0);

        // now let us suspend it (occurs during wait cycle, so we can put short timeout here)

        boolean stopped = taskManager.suspendTaskQuietly(task, 300, result);

        task.refresh(result);

        AssertJUnit.assertTrue("Task is not stopped", stopped);

        AssertJUnit.assertEquals(TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);
        AssertJUnit.assertTrue(task.getProgress() > 0);

//        Thread.sleep(200);        // give the scheduler a chance to release the task
//        task.refresh(result);
//        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());

    }

    @Test
    public void test014SuspendLongRunning() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        Task task = getTaskWithResult(taskOid(), result);
        System.out.println("After setup: " + task.debugDump());

        waitForTaskStart(taskOid(), result, 10000, 2000);

        task.refresh(result);

        System.out.println("After refresh: " + task.debugDump());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
//        AssertJUnit.assertEquals(TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);

        // now let us suspend it, without long waiting

        boolean stopped = taskManager.suspendTaskQuietly(task, 1000, result);

        task.refresh(result);

        assertFalse("Task is stopped (it should be running for now)", stopped);

        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
//        AssertJUnit.assertEquals("Task should be still claimed, as it is not definitely stopped", TaskExclusivityStatus.CLAIMED, task.getExclusivityStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNull(task.getLastRunFinishTimestamp());
        AssertJUnit.assertEquals("There should be no progress reported", 0, task.getProgress());

        // now let us wait for the finish

        stopped = taskManager.suspendTaskQuietly(task, 0, result);

        task.refresh(result);

        AssertJUnit.assertTrue("Task is not stopped", stopped);

        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());

        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull("Last run finish time is null", task.getLastRunStartTimestamp());
        assertFalse("Last run finish time is zero", task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertTrue("Progress is not reported", task.getProgress() > 0);

//        Thread.sleep(200);        // give the scheduler a chance to release the task
//        task.refresh(result);
//        AssertJUnit.assertEquals("Task is not released", TaskExclusivityStatus.RELEASED, task.getExclusivityStatus());
    }

    @Test
    public void test015DeleteTaskFromRepo() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());
        String oid = taskOid();

        // is the task in Quartz?

        final JobKey key = TaskQuartzImplUtil.createJobKeyForTaskOid(oid);
        AssertJUnit.assertTrue("Job in Quartz does not exist", taskManager.getExecutionManager().getQuartzScheduler().checkExists(key));

        // Remove task from repo

        repositoryService.deleteObject(TaskType.class, taskOid(), result);

        // We need to wait for a sync interval, so the task scanner has a chance
        // to pick up this task

        waitFor("Waiting for the job to disappear from Quartz Job Store", new Checker() {
            public boolean check() {
                try {
                    return !taskManager.getExecutionManager().getQuartzScheduler().checkExists(key);
                } catch (SchedulerException e) {
                    throw new SystemException(e);
                }
            }

            @Override
            public void timeout() {
            }
        }, 10000, 2000);
    }

    @Test
    public void test016WaitForSubtasks() throws Exception {
        final OperationResult result = createOperationResult();

        Task rootTask = taskManager.createTaskInstance(addObjectFromFile(taskFilename()), result);
        Task firstChildTask = taskManager.createTaskInstance(addObjectFromFile(taskFilename("-child-1")), result);

        Task firstReloaded = taskManager.getTaskByIdentifier(firstChildTask.getTaskIdentifier(), result);
        assertEquals("Didn't get correct task by identifier", firstChildTask.getOid(), firstReloaded.getOid());

        Task secondChildTask = rootTask.createSubtask();
        secondChildTask.setName("Second child");
        secondChildTask.setOwner(rootTask.getOwner());
        secondChildTask.pushHandlerUri(SINGLE_TASK_HANDLER_URI, new ScheduleType(), null);
        secondChildTask.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);           // will resume it after root starts waiting for tasks
        taskManager.switchToBackground(secondChildTask, result);

        Task firstPrerequisiteTask = taskManager.createTaskInstance(
                addObjectFromFile(taskFilename("-prerequisite-1")), result);

        List<Task> prerequisities = rootTask.listPrerequisiteTasks(result);
        assertEquals("Wrong # of prerequisite tasks", 1, prerequisities.size());
        assertEquals("Wrong OID of prerequisite task", firstPrerequisiteTask.getOid(), prerequisities.get(0).getOid());

        Task secondPrerequisiteTask = taskManager.createTaskInstance();
        secondPrerequisiteTask.setName("Second prerequisite");
        secondPrerequisiteTask.setOwner(rootTask.getOwner());
        secondPrerequisiteTask.addDependent(rootTask.getTaskIdentifier());
        secondPrerequisiteTask.pushHandlerUri(TaskConstants.NOOP_TASK_HANDLER_URI, new ScheduleType(), null);
        secondPrerequisiteTask.setExtensionPropertyValue(SchemaConstants.NOOP_DELAY_QNAME, 1500);
        secondPrerequisiteTask.setExtensionPropertyValue(SchemaConstants.NOOP_STEPS_QNAME, 1);
        secondPrerequisiteTask.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);           // will resume it after root starts waiting for tasks
        secondPrerequisiteTask.addDependent(rootTask.getTaskIdentifier());
        taskManager.switchToBackground(secondPrerequisiteTask, result);

        logger.info("Starting waiting for child/prerequisite tasks");
        rootTask.startWaitingForTasksImmediate(result);

        firstChildTask.refresh(result);
        assertEquals("Parent is not set correctly on 1st child task", rootTask.getTaskIdentifier(), firstChildTask.getParent());
        secondChildTask.refresh(result);
        assertEquals("Parent is not set correctly on 2nd child task", rootTask.getTaskIdentifier(), secondChildTask.getParent());
        firstPrerequisiteTask.refresh(result);
        assertEquals("Dependents are not set correctly on 1st prerequisite task (count differs)", 1, firstPrerequisiteTask.getDependents().size());
        assertEquals("Dependents are not set correctly on 1st prerequisite task (value differs)", rootTask.getTaskIdentifier(), firstPrerequisiteTask.getDependents().get(0));
        List<Task> deps = firstPrerequisiteTask.listDependents(result);
        assertEquals("Dependents are not set correctly on 1st prerequisite task - listDependents - (count differs)", 1, deps.size());
        assertEquals("Dependents are not set correctly on 1st prerequisite task - listDependents - (value differs)", rootTask.getOid(), deps.get(0).getOid());
        secondPrerequisiteTask.refresh(result);
        assertEquals("Dependents are not set correctly on 2nd prerequisite task (count differs)", 1, secondPrerequisiteTask.getDependents().size());
        assertEquals("Dependents are not set correctly on 2nd prerequisite task (value differs)", rootTask.getTaskIdentifier(), secondPrerequisiteTask.getDependents().get(0));
        deps = secondPrerequisiteTask.listDependents(result);
        assertEquals("Dependents are not set correctly on 2nd prerequisite task - listDependents - (count differs)", 1, deps.size());
        assertEquals("Dependents are not set correctly on 2nd prerequisite task - listDependents - (value differs)", rootTask.getOid(), deps.get(0).getOid());

        logger.info("Resuming suspended child/prerequisite tasks");
        taskManager.resumeTask(secondChildTask, result);
        taskManager.resumeTask(secondPrerequisiteTask, result);

        final String rootOid = taskOid();

        waitForTaskClose(rootOid, result, 60000, 3000);

        firstChildTask.refresh(result);
        secondChildTask.refresh(result);
        firstPrerequisiteTask.refresh(result);
        secondPrerequisiteTask.refresh(result);

        assertEquals("1st child task should be closed", TaskExecutionStatus.CLOSED, firstChildTask.getExecutionStatus());
        assertEquals("2nd child task should be closed", TaskExecutionStatus.CLOSED, secondChildTask.getExecutionStatus());
        assertEquals("1st prerequisite task should be closed", TaskExecutionStatus.CLOSED, firstPrerequisiteTask.getExecutionStatus());
        assertEquals("2nd prerequisite task should be closed", TaskExecutionStatus.CLOSED, secondPrerequisiteTask.getExecutionStatus());
    }

    @Test
    public void test017WaitForSubtasksEmpty() throws Exception {
        final OperationResult result = createOperationResult();

        taskManager.getClusterManager().startClusterManagerThread();

        try {
            Task rootTask = taskManager.createTaskInstance(
                    addObjectFromFile(taskFilename()), result);
            display("root task", rootTask);
            waitForTaskClose(taskOid(), result, 40000, 3000);
        } finally {
            taskManager.getClusterManager().stopClusterManagerThread(10000L, result);
        }
    }

    @Test
    public void test018TaskResult() throws Exception {
        final OperationResult result = createOperationResult();

        Task task = taskManager.createTaskInstance();
        task.setInitialExecutionStatus(TaskExecutionStatus.SUSPENDED);
        PrismObject<UserType> owner2 = repositoryService.getObject(UserType.class, TASK_OWNER2_OID, null, result);
        task.setOwner(owner2);
        AssertJUnit.assertEquals("Task result for new task is not correct", OperationResultStatus.UNKNOWN, task.getResult().getStatus());

        taskManager.switchToBackground(task, result);
        AssertJUnit.assertEquals("Background task result is not correct (in memory)", OperationResultStatus.IN_PROGRESS, task.getResult().getStatus());
        PrismObject<TaskType> task1 = repositoryService.getObject(TaskType.class, task.getOid(), retrieveItemsNamed(TaskType.F_RESULT), result);
        AssertJUnit.assertEquals("Background task result is not correct (in repo)", OperationResultStatusType.IN_PROGRESS, task1.asObjectable().getResult().getStatus());

        // now change task's result and check the refresh() method w.r.t. result handling
        task.getResult().recordFatalError("");
        AssertJUnit.assertEquals(OperationResultStatus.FATAL_ERROR, task.getResult().getStatus());
        task.refresh(result);
        AssertJUnit.assertEquals("Refresh does not update task's result", OperationResultStatus.IN_PROGRESS, task.getResult().getStatus());
    }

    /*
     * Recurring task returning FINISHED_HANDLER code.
     */

    @Test
    public void test019FinishedHandler() throws Exception {
        final OperationResult result = createOperationResult();

        // reset 'has run' flag on handlers
        singleHandler1.resetHasRun();

        addObjectFromFile(taskFilename());

        waitForTaskClose(taskOid(), result, 15000, 2000);

        // Check task status

        Task task = getTaskWithResult(taskOid(), result);

        AssertJUnit.assertNotNull(task);
        System.out.println(task.debugDump());

        PrismObject<TaskType> o = repositoryService.getObject(TaskType.class, taskOid(), null, result);
        System.out.println(ObjectTypeUtil.dump(o.getValue().getValue()));

        // .. it should be closed
        AssertJUnit.assertEquals(TaskExecutionStatus.CLOSED, task.getExecutionStatus());

        // .. and last run should not be zero
        AssertJUnit.assertNotNull(task.getLastRunStartTimestamp());
        assertFalse(task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull("Last run finish timestamp not set", task.getLastRunFinishTimestamp());
        assertFalse("Last run finish timestamp is 0", task.getLastRunFinishTimestamp() == 0);

        // The progress should be at least 2 as the task has run at least twice (once in each handler)
        AssertJUnit.assertTrue("Task reported progress lower than 2", task.getProgress() >= 2);

        // Test for presence of a result. It should be there and it should
        // indicate success
        assertSuccess(task);

        // Test for no presence of handlers

        AssertJUnit.assertNotNull("Handler is gone", task.getHandlerUri());
        AssertJUnit.assertTrue("Other handlers are still present",
                task.getOtherHandlersUriStack() == null || task.getOtherHandlersUriStack().getUriStackEntry().isEmpty());

        // Test if "outer" handler has run as well

        AssertJUnit.assertTrue("Handler1 has not run", singleHandler1.hasRun());
    }

    @Test
    public void test020QueryByExecutionStatus() throws Exception {
        final OperationResult result = createOperationResult();

        taskManager.createTaskInstance(addObjectFromFile(taskFilename()), result);

        ObjectFilter filter1 = prismContext.queryFor(TaskType.class).item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.WAITING).buildFilter();
        ObjectFilter filter2 = prismContext.queryFor(TaskType.class).item(TaskType.F_WAITING_REASON).eq(TaskWaitingReasonType.OTHER).buildFilter();
        ObjectFilter filter3 = prismContext.queryFactory().createAnd(filter1, filter2);

        List<PrismObject<TaskType>> prisms1 = repositoryService.searchObjects(TaskType.class, prismContext.queryFactory().createQuery(filter1), null, result);
        List<PrismObject<TaskType>> prisms2 = repositoryService.searchObjects(TaskType.class, prismContext.queryFactory().createQuery(filter2), null, result);
        List<PrismObject<TaskType>> prisms3 = repositoryService.searchObjects(TaskType.class, prismContext.queryFactory().createQuery(filter3), null, result);

        assertFalse("There were no tasks with executionStatus == WAITING found", prisms1.isEmpty());
        assertFalse("There were no tasks with waitingReason == OTHER found", prisms2.isEmpty());
        assertFalse("There were no tasks with executionStatus == WAITING and waitingReason == OTHER found", prisms3.isEmpty());
    }

    @Test
    public void test021DeleteTaskTree() throws Exception {
        final OperationResult result = createOperationResult();

        PrismObject<TaskType> parentTaskPrism = addObjectFromFile(taskFilename());
        PrismObject<TaskType> childTask1Prism = addObjectFromFile(taskFilename("-child1"));
        PrismObject<TaskType> childTask2Prism = addObjectFromFile(taskFilename("-child2"));

        AssertJUnit.assertEquals(TaskExecutionStatusType.WAITING, parentTaskPrism.asObjectable().getExecutionStatus());
        AssertJUnit.assertEquals(TaskExecutionStatusType.SUSPENDED, childTask1Prism.asObjectable().getExecutionStatus());
        AssertJUnit.assertEquals(TaskExecutionStatusType.SUSPENDED, childTask2Prism.asObjectable().getExecutionStatus());

        Task parentTask = taskManager.createTaskInstance(parentTaskPrism, result);
        Task childTask1 = taskManager.createTaskInstance(childTask1Prism, result);
        Task childTask2 = taskManager.createTaskInstance(childTask2Prism, result);

        IntegrationTestTools.display("parent", parentTask);
        IntegrationTestTools.display("child1", childTask1);
        IntegrationTestTools.display("child2", childTask2);

        taskManager.resumeTask(childTask1, result);
        taskManager.resumeTask(childTask2, result);
        parentTask.startWaitingForTasksImmediate(result);

        logger.info("Deleting task {} and its subtasks", parentTask);

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

    @Test
    public void test022ExecuteRecurringOnDemand() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        Task task = getTaskWithResult(taskOid(), result);
        System.out.println("After setup: " + task.debugDump());

        System.out.println("Waiting to see if the task would not start...");
        Thread.sleep(5000L);

        // check the task HAS NOT started
        task.refresh(result);
        System.out.println("After initial wait: " + task.debugDump());

        assertEquals("task is not RUNNABLE", TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
        assertNull("task was started", task.getLastRunStartTimestamp());
        assertEquals("task was achieved some progress", 0L, task.getProgress());

        // now let's start the task
        taskManager.scheduleRunnableTaskNow(task, result);

        // task is executing for 1000 ms, so we need to wait slightly longer, in order for the execution to be done
        waitForTaskProgress(taskOid(), result, 10000, 2000, 1);

        task.refresh(result);
        System.out.println("After refresh: " + task.debugDump());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());
        AssertJUnit.assertNotNull("LastRunStartTimestamp is null", task.getLastRunStartTimestamp());
        assertFalse("LastRunStartTimestamp is 0", task.getLastRunStartTimestamp() == 0);
        AssertJUnit.assertNotNull(task.getLastRunFinishTimestamp());
        assertFalse(task.getLastRunFinishTimestamp() == 0);
        AssertJUnit.assertTrue("no progress", task.getProgress() > 0);

        // now let us suspend it (occurs during wait cycle, so we can put short timeout here)

        boolean stopped = taskManager.suspendTaskQuietly(task, 10000L, result);
        task.refresh(result);
        AssertJUnit.assertTrue("Task is not stopped", stopped);
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());
    }

    @Test
    public void test100LightweightSubtasks() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        String taskOid = taskOid();
        Task task = getTaskWithResult(taskOid, result);
        System.out.println("After setup: " + task.debugDump());

        checkTaskStateRepeatedly(taskOid, result, 15000, 20);

        waitForTaskClose(taskOid, result, 15000, 500);
        task.refresh(result);
        System.out.println("After refresh (task was executed): " + task.debugDump());

        Collection<? extends RunningTask> subtasks = parallelTaskHandler.getLastTaskExecuted().getLightweightAsynchronousSubtasks();
        assertEquals("Wrong number of subtasks", MockParallelTaskHandler.NUM_SUBTASKS, subtasks.size());
        for (RunningTask subtask : subtasks) {
            assertEquals("Wrong subtask state", TaskExecutionStatus.CLOSED, subtask.getExecutionStatus());
            MockParallelTaskHandler.MyLightweightTaskHandler handler = (MockParallelTaskHandler.MyLightweightTaskHandler) subtask.getLightweightTaskHandler();
            assertTrue("Handler has not run", handler.hasRun());
            assertTrue("Handler has not exited", handler.hasExited());
        }
    }

    private void checkTaskStateRepeatedly(String taskOid, OperationResult result, int duration, int checkInterval)
            throws SchemaException, ObjectNotFoundException, InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() < start + duration) {
            Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                    .item(TaskType.F_SUBTASK_REF).retrieve()
                    .build();
            TaskType task = taskManager.getObject(TaskType.class, taskOid, options, result).asObjectable();
            OperationStatsType stats = task.getOperationStats();
            Integer totalSuccessCount = stats != null && stats.getIterativeTaskInformation() != null ?
                    stats.getIterativeTaskInformation().getTotalSuccessCount() : null;
            System.out.println((System.currentTimeMillis() - start) + ": subtasks: " + task.getSubtaskRef().size() +
                    ", progress = " + task.getProgress() + ", objects = " + totalSuccessCount);
            if (task.getExecutionStatus() != TaskExecutionStatusType.RUNNABLE) {
                System.out.println("Done. Status = " + task.getExecutionStatus());
                break;
            }
            Thread.sleep(checkInterval);
        }
    }

    @Test
    public void test105LightweightSubtasksSuspension() throws Exception {
        final OperationResult result = createOperationResult();

        addObjectFromFile(taskFilename());

        Task task = getTaskWithResult(taskOid(), result);
        System.out.println("After setup: " + task.debugDump());

        waitForTaskStart(taskOid(), result, 15000, 500);

        task.refresh(result);
        System.out.println("After refresh (task was started; and it should run now): " + task.debugDump());

        AssertJUnit.assertEquals(TaskExecutionStatus.RUNNABLE, task.getExecutionStatus());

        // check the thread
        List<JobExecutionContext> jobExecutionContexts = taskManager.getExecutionManager().getQuartzScheduler().getCurrentlyExecutingJobs();
        JobExecutionContext found = null;
        for (JobExecutionContext jobExecutionContext : jobExecutionContexts) {
            if (task.getOid().equals(jobExecutionContext.getJobDetail().getKey().getName())) {
                found = jobExecutionContext;
                break;
            }
        }
        assertNotNull("Job for the task was not found", found);
        JobExecutor executor = (JobExecutor) found.getJobInstance();
        assertNotNull("No job executor", executor);
        Thread thread = executor.getExecutingThread();
        assertNotNull("No executing thread", thread);

        // now let us suspend it - the handler should stop, as well as the subtasks

        boolean stopped = taskManager.suspendTaskQuietly(task, 10000L, result);
        task.refresh(result);
        AssertJUnit.assertTrue("Task is not stopped", stopped);
        AssertJUnit.assertEquals("Task is not suspended", TaskExecutionStatus.SUSPENDED, task.getExecutionStatus());

        Collection<? extends RunningTask> subtasks = parallelTaskHandler.getLastTaskExecuted().getLightweightAsynchronousSubtasks();
        assertEquals("Wrong number of subtasks", MockParallelTaskHandler.NUM_SUBTASKS, subtasks.size());
        for (RunningTask subtask : subtasks) {
            assertEquals("Wrong subtask state", TaskExecutionStatus.CLOSED, subtask.getExecutionStatus());
            MockParallelTaskHandler.MyLightweightTaskHandler handler = (MockParallelTaskHandler.MyLightweightTaskHandler) subtask.getLightweightTaskHandler();
            assertTrue("Handler has not run", handler.hasRun());
            assertTrue("Handler has not exited", handler.hasExited());
        }
    }

    @Test
    public void test108SecondaryGroupLimit() throws Exception {
        final OperationResult result = createOperationResult();

        TaskType task1 = (TaskType) addObjectFromFile(taskFilename()).asObjectable();
        waitForTaskStart(task1.getOid(), result, 10000, 500);

        // import second task with the same group (expensive)
        TaskType task2 = (TaskType) addObjectFromFile(taskFilename("-2")).asObjectable();

        Thread.sleep(10000);
        task1 = getTaskType(task1.getOid(), result);
        assertNull("First task should have no retry time", task1.getNextRetryTimestamp());

        String task2Oid = task2.getOid();
        task2 = getTaskType(task2Oid, result);
        assertNull("Second task was started even if it should not be", task2.getLastRunStartTimestamp());
        assertNextRetryTimeSet(task2, result);

        // now finish first task and check the second one is started
        boolean stopped = taskManager.suspendTasks(Collections.singleton(task1.getOid()), 20000L, result);
        assertTrue("Task 1 was not suspended successfully", stopped);

        waitForTaskStart(task2Oid, result, 10000, 500);

        // import third task that has another collision (large-ram) with the second one
        TaskType task3 = (TaskType) addObjectFromFile(taskFilename("-3")).asObjectable();

        Thread.sleep(10000);
        task2 = getTaskType(task2Oid, result);
        assertNull("Second task should have no retry time", task2.getNextRetryTimestamp());

        task3 = getTaskType(task3.getOid(), result);
        assertNull("Third task was started even if it should not be", task3.getLastRunStartTimestamp());
        assertNextRetryTimeSet(task3, result);

        // now finish second task and check the third one is started
        stopped = taskManager.suspendTasks(Collections.singleton(task2Oid), 20000L, result);
        assertTrue("Task 2 was not suspended successfully", stopped);

        waitForTaskStart(task3.getOid(), result, 10000, 500);

        taskManager.suspendTasks(Collections.singleton(task3.getOid()), 20000L, result);
    }

    protected void assertNextRetryTimeSet(TaskType task, OperationResult result)
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
    public void test110GroupLimit() throws Exception {
        final OperationResult result = createOperationResult();

        taskManager.getExecutionManager().setLocalExecutionLimitations((TaskExecutionLimitationsType) null);

        TaskType task1 = (TaskType) addObjectFromFile(taskFilename()).asObjectable();
        waitForTaskStart(task1.getOid(), result, 10000, 500);

        // import second task with the same group
        TaskType task2 = (TaskType) addObjectFromFile(taskFilename("-2")).asObjectable();

        Thread.sleep(10000);
        task1 = getTaskType(task1.getOid(), result);
        assertNull("First task should have no retry time", task1.getNextRetryTimestamp());

        String task2Oid = task2.getOid();
        task2 = getTaskType(task2Oid, result);
        assertNull("Second task was started even if it should not be", task2.getLastRunStartTimestamp());
        assertNextRetryTimeSet(task2, result);

        // now finish first task and check the second one is started
        boolean stopped = taskManager.suspendTasks(Collections.singleton(task1.getOid()), 20000L, result);
        assertTrue("Task 1 was not suspended successfully", stopped);

        waitForTaskStart(task2Oid, result, 10000, 500);
        taskManager.suspendTasks(Collections.singleton(task2Oid), 20000L, result);
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
    public void test120NodeAllowed() throws Exception {
        final OperationResult result = createOperationResult();

        taskManager.getExecutionManager().setLocalExecutionLimitations(
                new TaskExecutionLimitationsType()
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName("lightweight-tasks"))
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName(null))
                        .groupLimitation(new TaskGroupExecutionLimitationType().groupName(TaskConstants.LIMIT_FOR_OTHER_GROUPS).limit(0)));

        TaskType task = (TaskType) addObjectFromFile(taskFilename()).asObjectable();
        waitForTaskStart(task.getOid(), result, 10000, 500);
        task = getTaskType(task.getOid(), result);
        assertNotNull("Task was not started even if it should be", task.getLastRunStartTimestamp());
    }

    @Test
    public void test130NodeNotAllowed() throws Exception {
        final OperationResult result = createOperationResult();

        TaskType task = (TaskType) addObjectFromFile(taskFilename()).asObjectable();
        Thread.sleep(10000);
        task = getTaskType(task.getOid(), result);
        assertNull("Task was started even if it shouldn't be", task.getLastRunStartTimestamp());
        taskManager.suspendTasks(Collections.singleton(task.getOid()), 1000L, result);
    }

    @Test
    public void test200RetrieveSubtasks() throws Exception {
        final OperationResult result = createOperationResult();

        String rootOid = addObjectFromFile(taskFilename()).getOid();
        String child1Oid = addObjectFromFile(taskFilename("-child-1")).getOid();
        String child11Oid = addObjectFromFile(taskFilename("-child-1-1")).getOid();
        String child2Oid = addObjectFromFile(taskFilename("-child-2")).getOid();

        Collection<SelectorOptions<GetOperationOptions>> withChildren = schemaHelper.getOperationOptionsBuilder()
                .item(TaskType.F_SUBTASK_REF).retrieve()
                .build();

        TaskType getTaskPlain_rootNoChildren = taskManager.getTaskPlain(rootOid, null, result).getUpdatedTaskObject().asObjectable();
        TaskType getTaskPlain_rootWithChildren = taskManager.getTaskPlain(rootOid, withChildren, result).getUpdatedTaskObject().asObjectable();
        assertEquals("Wrong # of children", 0, getTaskPlain_rootNoChildren.getSubtaskRef().size());
        assertEquals("Wrong # of children", 0, getTaskPlain_rootWithChildren.getSubtaskRef().size());

        TaskType getTask_rootNoChildren = taskManager.getTask(rootOid, null, result).getUpdatedTaskObject().asObjectable();
        TaskType getTask_rootWithChildren = taskManager.getTask(rootOid, withChildren, result).getUpdatedTaskObject().asObjectable();
        assertEquals("Wrong # of children", 0, getTask_rootNoChildren.getSubtaskRef().size());
        assertTaskTree(getTask_rootWithChildren, child1Oid, child2Oid, child11Oid);

        TaskType getObject_rootNoChildren = taskManager.getObject(TaskType.class, rootOid, null, result).asObjectable();
        TaskType getObject_rootWithChildren = taskManager.getObject(TaskType.class, rootOid, withChildren, result).asObjectable();
        assertEquals("Wrong # of children", 0, getObject_rootNoChildren.getSubtaskRef().size());
        assertTaskTree(getObject_rootWithChildren, child1Oid, child2Oid, child11Oid);

        ObjectQuery query = prismContext.queryFor(TaskType.class).id(rootOid).build();
        TaskType searchObjects_rootNoChildren = extractSingleton(taskManager.searchObjects(TaskType.class, query, null, result)).asObjectable();
        TaskType searchObjects_rootWithChildren = extractSingleton(taskManager.searchObjects(TaskType.class, query, withChildren, result)).asObjectable();
        assertEquals("Wrong # of children", 0, searchObjects_rootNoChildren.getSubtaskRef().size());
        assertTaskTree(searchObjects_rootWithChildren, child1Oid, child2Oid, child11Oid);
    }

    private void assertTaskTree(TaskType rootWithChildren, String child1Oid, String child2Oid, String child11Oid) {
        assertEquals("Wrong # of children of root", 2, rootWithChildren.getSubtaskRef().size());
        TaskType child1 = TaskTypeUtil.findChild(rootWithChildren, child1Oid);
        TaskType child2 = TaskTypeUtil.findChild(rootWithChildren, child2Oid);
        assertNotNull(child1);
        assertNotNull(child2);
        assertEquals("Wrong # of children of child1", 1, child1.getSubtaskRef().size());
        assertEquals("Wrong # of children of child2", 0, child2.getSubtaskRef().size());
        TaskType child11 = TaskTypeUtil.findChild(child1, child11Oid);
        assertNotNull(child11);
    }

    @Test
    public void test999CheckingLeftovers() throws Exception {
        OperationResult result = createOperationResult();

        ArrayList<String> leftovers = new ArrayList<>();
        checkLeftover(leftovers, "test005", result);
        checkLeftover(leftovers, "test006", result);
        checkLeftover(leftovers, "test008", result);
        checkLeftover(leftovers, "test009", result);
        checkLeftover(leftovers, "test010", result);
        checkLeftover(leftovers, "test011", result);
        checkLeftover(leftovers, "test012", result);
        checkLeftover(leftovers, "test013", result);
        checkLeftover(leftovers, "test014", result);
        checkLeftover(leftovers, "test015", result);
        checkLeftover(leftovers, "test016", result);
        checkLeftover(leftovers, "test017", result);
        checkLeftover(leftovers, "test019", result);
        checkLeftover(leftovers, "test021", result);
        checkLeftover(leftovers, "test021", "1", result);
        checkLeftover(leftovers, "test021", "2", result);
        checkLeftover(leftovers, "test022", result);
        checkLeftover(leftovers, "test100", result);
        checkLeftover(leftovers, "test105", result);
        checkLeftover(leftovers, "test108", result);
        checkLeftover(leftovers, "test108", "a", result);
        checkLeftover(leftovers, "test108", "b", result);
        checkLeftover(leftovers, "test110", result);
        checkLeftover(leftovers, "test110", "a", result);
        checkLeftover(leftovers, "test120", result);
        checkLeftover(leftovers, "test130", result);
        checkLeftover(leftovers, "test200", result);

        StringBuilder message = new StringBuilder("Leftover task(s) found:");
        for (String leftover : leftovers) {
            message.append(" ").append(leftover);
        }

        AssertJUnit.assertTrue(message.toString(), leftovers.isEmpty());
    }

    private void checkLeftover(ArrayList<String> leftovers, String testNumber, OperationResult result) throws Exception {
        checkLeftover(leftovers, testNumber, "0", result);
    }

    private void checkLeftover(ArrayList<String> leftovers, String testNumber, String subId, OperationResult result) throws Exception {
        String oid = taskOid(testNumber, subId);
        Task t;
        try {
            t = getTaskWithResult(oid, result);
        } catch (ObjectNotFoundException e) {
            // this is OK, test probably did not start
            logger.info("Check leftovers: Task {} does not exist.", oid);
            return;
        }

        logger.info("Check leftovers: Task " + oid + " state: " + t.getExecutionStatus());

        if (t.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
            logger.info("Leftover task: {}", t);
            leftovers.add(t.getOid());
        }
    }

    private TaskQuartzImpl getTaskWithResult(String oid, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return taskManager.getTaskWithResult(oid, result);
    }
}
