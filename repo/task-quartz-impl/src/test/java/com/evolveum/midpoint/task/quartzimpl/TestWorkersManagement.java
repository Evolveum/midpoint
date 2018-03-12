/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.quartzimpl.work.WorkStateManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskExecutionStatusType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.util.TestUtil.displayThen;
import static com.evolveum.midpoint.test.util.TestUtil.displayWhen;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */

@ContextConfiguration(locations = {"classpath:ctx-task-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestWorkersManagement extends AbstractTaskManagerTest {

	private static final transient Trace LOGGER = TraceManager.getTrace(TestWorkersManagement.class);
	public static final long DEFAULT_SLEEP_INTERVAL = 250L;
	public static final long DEFAULT_TIMEOUT = 10000L;

	@Autowired private WorkStateManager workStateManager;

	private static String taskFilename(String testName, String subId) {
		return "src/test/resources/workers/task-" + testNumber(testName) + "-" + subId + ".xml";
	}

	private static String taskFilename(String testName) {
		return taskFilename(testName, "0");
	}

	private static String taskOid(String testName, String subId) {
		return "44444444-2222-2222-2223-" + testNumber(testName) + subId + "00000000";
	}

	private static String taskOid(String test) {
		return taskOid(test, "0");
	}

	private static String testNumber(String test) {
		return test.substring(4, 7);
	}

	@NotNull
	protected String workerTaskFilename(String TEST_NAME) {
		return taskFilename(TEST_NAME, "w");
	}

	@NotNull
	protected String coordinatorTaskFilename(String TEST_NAME) {
		return taskFilename(TEST_NAME, "c");
	}

	@NotNull
	protected String workerTaskOid(String TEST_NAME) {
		return taskOid(TEST_NAME, "w");
	}

	@NotNull
	protected String coordinatorTaskOid(String TEST_NAME) {
		return taskOid(TEST_NAME, "c");
	}

	@PostConstruct
	public void initialize() throws Exception {
		super.initialize();
		workStateManager.setFreeBucketWaitInterval(1000L);
		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
	}

    @Test
    public void test000Integrity() {
        AssertJUnit.assertNotNull(repositoryService);
        AssertJUnit.assertNotNull(taskManager);
    }

    @Test
    public void test100CreateWorkersSingle() throws Exception {
        final String TEST_NAME = "test100CreateWorkersSingle";
        OperationResult result = createResult(TEST_NAME, LOGGER);

	    workBucketsTaskHandler1.setDelayProcessor(DEFAULT_SLEEP_INTERVAL);

        // WHEN
	    addObjectFromFile(coordinatorTaskFilename(TEST_NAME));

	    // THEN
	    String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);
	    waitForTaskProgress(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 1);

	    TaskQuartzImpl coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	    List<Task> workers = coordinatorTask.listSubtasks(result);
	    assertEquals("Wrong # of workers", 1, workers.size());

	    display("coordinator task", coordinatorTask);
	    display("worker task", workers.get(0));

	    waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

	    Thread.sleep(1000);         // if workers would be started again, we would get some more processing here
	    assertEquals("Wrong # of items processed", 4, workBucketsTaskHandler1.getItemsProcessed());

	    // TODO some asserts here

	    // WHEN
	    taskManager.scheduleTasksNow(singleton(coordinatorTaskOid), result);

	    // THEN
	    waitForTaskClose(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

	    coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	    workers = coordinatorTask.listSubtasks(result);
	    assertEquals("Wrong # of workers", 1, workers.size());

	    display("coordinator task after re-run", coordinatorTask);
	    display("worker task after re-run", workers.get(0));

	    Thread.sleep(1000);         // if workers would be started again, we would get some more processing here
	    assertEquals("Wrong # of items processed", 8, workBucketsTaskHandler1.getItemsProcessed());

	    suspendAndDeleteTasks(coordinatorTaskOid);
    }

    @Test
    public void test110CreateWorkersRecurring() throws Exception {
        final String TEST_NAME = "test110CreateWorkersRecurring";
        OperationResult result = createResult(TEST_NAME, LOGGER);

	    workBucketsTaskHandler1.setDelayProcessor(DEFAULT_SLEEP_INTERVAL);

        // (1) ------------------------------------------------------------------------------------ WHEN (import task)
	    displayWhen(TEST_NAME, "1: import task");
	    addObjectFromFile(coordinatorTaskFilename(TEST_NAME));

	    // THEN (worker is created and executed)
	    displayThen(TEST_NAME, "1: import task");
	    String coordinatorTaskOid = coordinatorTaskOid(TEST_NAME);
	    waitForTaskProgress(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 1);

	    TaskQuartzImpl coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	    List<Task> workers = coordinatorTask.listSubtasks(result);
	    assertEquals("Wrong # of workers", 1, workers.size());

	    display("coordinator task", coordinatorTask);
	    display("worker task", workers.get(0));

	    waitForTaskClose(workers.get(0).getOid(), result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

	    assertEquals("Wrong # of items processed", 4, workBucketsTaskHandler1.getItemsProcessed());
	    // TODO some asserts here

	    // coordinator should run automatically in cca 2-3 seconds

	    // (2) ------------------------------------------------------------------------------------ WHEN (wait for coordinator next run)
	    displayWhen(TEST_NAME, "2: wait for coordinator next run");
	    // TODO adapt this when the coordinator progress will be reported in other ways
	    waitForTaskProgress(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 2);

	    // THEN (worker is still present and executed)
	    displayThen(TEST_NAME, "2: wait for coordinator next run");
	    coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	    workers = coordinatorTask.listSubtasks(result);
	    assertEquals("Wrong # of workers", 1, workers.size());

	    display("coordinator task after re-run", coordinatorTask);
	    display("worker task after re-run", workers.get(0));

	    waitForTaskClose(workers.get(0).getOid(), result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

	    assertEquals("Wrong # of items processed", 8, workBucketsTaskHandler1.getItemsProcessed());

	    // (3) ------------------------------------------------------------------------------------  WHEN (suspend the tree while work is done)
	    displayWhen(TEST_NAME, "3: suspend the tree while work is done");
	    boolean stopped = taskManager.suspendTaskTree(coordinatorTaskOid, 20000L, result);

	    // THEN (tasks are suspended)
	    displayThen(TEST_NAME, "3: suspend the tree while work is done");
	    coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	    workers = coordinatorTask.listSubtasks(result);
	    assertEquals("Wrong # of workers", 1, workers.size());
	    Task worker = workers.get(0);

	    display("coordinator task after suspend-when-waiting", coordinatorTask);
	    display("worker task after suspend-when-waiting", worker);

	    assertEquals("Wrong execution status of coordinator", TaskExecutionStatus.SUSPENDED, coordinatorTask.getExecutionStatus());
	    assertEquals("Wrong state-before-suspend of coordinator", TaskExecutionStatusType.RUNNABLE, coordinatorTask.getStateBeforeSuspend());
	    assertEquals("Wrong execution status of worker", TaskExecutionStatus.CLOSED, worker.getExecutionStatus());
	    assertEquals("Wrong state-before-suspend of worker", null, worker.getStateBeforeSuspend());

	    assertTrue("tasks were not stopped", stopped);

	    // (4) ------------------------------------------------------------------------------------  WHEN (resume the tree)
	    displayWhen(TEST_NAME, "4: resume the tree");
	    taskManager.resumeTaskTree(coordinatorTaskOid, result);

	    // THEN (tasks are resumed)
	    displayThen(TEST_NAME, "4: resume the tree");
	    coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	    workers = coordinatorTask.listSubtasks(result);
	    assertEquals("Wrong # of workers", 1, workers.size());
	    worker = workers.get(0);

	    display("coordinator task after resume-from-suspend-when-waiting", coordinatorTask);
	    display("worker task after resume-from-suspend-when-waiting", worker);

	    assertEquals("Wrong execution status of coordinator", TaskExecutionStatus.RUNNABLE, coordinatorTask.getExecutionStatus());
	    assertEquals("Wrong state-before-suspend of coordinator", null, coordinatorTask.getStateBeforeSuspend());
	    assertEquals("Wrong state-before-suspend of worker", null, worker.getStateBeforeSuspend());

	    // (5) ------------------------------------------------------------------------------------  WHEN (suspend the tree while worker is executing)
	    displayWhen(TEST_NAME, "5: suspend the tree while worker is executing");
	    waitForTaskProgress(coordinatorTaskOid, result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL, 3);
	    stopped = taskManager.suspendTaskTree(coordinatorTaskOid, 20000L, result);

	    // THEN (tasks are suspended)
	    displayThen(TEST_NAME, "5: suspend the tree while worker is executing");
	    coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	    workers = coordinatorTask.listSubtasks(result);
	    assertEquals("Wrong # of workers", 1, workers.size());
	    worker = workers.get(0);

	    display("coordinator task after suspend-when-running", coordinatorTask);
	    display("worker task after suspend-when-running", worker);

	    assertEquals("Wrong execution status of coordinator", TaskExecutionStatus.SUSPENDED, coordinatorTask.getExecutionStatus());
	    // in theory, the execution could be 'after' at this time; so this assertion might fail
	    assertEquals("Wrong state-before-suspend of coordinator", TaskExecutionStatusType.WAITING, coordinatorTask.getStateBeforeSuspend());
	    assertEquals("Wrong execution status of worker", TaskExecutionStatus.SUSPENDED, worker.getExecutionStatus());
	    assertEquals("Wrong state-before-suspend of worker", TaskExecutionStatusType.RUNNABLE, worker.getStateBeforeSuspend());

	    assertTrue("tasks were not stopped", stopped);

	    // (6) ------------------------------------------------------------------------------------  WHEN (resume after 2nd suspend)
	    displayWhen(TEST_NAME, "6: resume after 2nd suspend");
	    taskManager.resumeTaskTree(coordinatorTaskOid, result);

	    // THEN (tasks are suspended)
	    displayThen(TEST_NAME, "6: resume after 2nd suspend");
	    coordinatorTask = taskManager.getTask(coordinatorTaskOid(TEST_NAME), result);
	    workers = coordinatorTask.listSubtasks(result);
	    assertEquals("Wrong # of workers", 1, workers.size());
	    worker = workers.get(0);

	    display("coordinator task after resume-after-2nd-suspend", coordinatorTask);
	    display("worker task after resume-after-2nd-suspend", worker);

	    waitForTaskClose(worker.getOid(), result, DEFAULT_TIMEOUT, DEFAULT_SLEEP_INTERVAL);

	    // brittle - might fail
	    assertEquals("Wrong # of items processed", 12, workBucketsTaskHandler1.getItemsProcessed());

	    // cleanup

	    suspendAndDeleteTasks(coordinatorTaskOid);
    }

}
