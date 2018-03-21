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
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskDebugUtil;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 */
public class AbstractTaskManagerTest extends AbstractTestNGSpringContextTests {

	protected static final String CYCLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-task-handler";
	protected static final String CYCLE_FINISHING_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-finishing-task-handler";
	protected static final String SINGLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-task-handler";
	protected static final String SINGLE_TASK_HANDLER_2_URI = "http://midpoint.evolveum.com/test/single-task-handler-2";
	protected static final String SINGLE_TASK_HANDLER_3_URI = "http://midpoint.evolveum.com/test/single-task-handler-3";
	protected static final String SINGLE_WB_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-wb-task-handler";
	protected static final String PARTITIONED_WB_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/partitioned-wb-task-handler";
	protected static final String PARTITIONED_WB_TASK_HANDLER_URI_1 = PARTITIONED_WB_TASK_HANDLER_URI + "#1";
	protected static final String PARTITIONED_WB_TASK_HANDLER_URI_2 = PARTITIONED_WB_TASK_HANDLER_URI + "#2";
	protected static final String PARTITIONED_WB_TASK_HANDLER_URI_3 = PARTITIONED_WB_TASK_HANDLER_URI + "#3";
	protected static final String L1_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l1-task-handler";
	protected static final String L2_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l2-task-handler";
	protected static final String L3_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l3-task-handler";
	protected static final String WAIT_FOR_SUBTASKS_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/wait-for-subtasks-task-handler";
	protected static final String PARALLEL_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/parallel-task-handler";
	protected static final String LONG_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/long-task-handler";

	private static final String USER_ADMINISTRATOR_FILE = "src/test/resources/common/user-administrator.xml";

	// TODO make configurable. Due to a race condition there can be a small number of unoptimized complete buckets
	// (it should not exceed the number of workers ... at least not by much amount :)
	private static final int OPTIMIZED_BUCKETS_THRESHOLD = 8;

	@Autowired protected RepositoryService repositoryService;
	@Autowired protected TaskManagerQuartzImpl taskManager;
	@Autowired protected PrismContext prismContext;

	protected MockSingleTaskHandler singleHandler1, singleHandler2, singleHandler3;
	protected MockWorkBucketsTaskHandler workBucketsTaskHandler;
	protected MockWorkBucketsTaskHandler partitionedWorkBucketsTaskHandler;
	protected MockSingleTaskHandler l1Handler, l2Handler, l3Handler;
	protected MockSingleTaskHandler waitForSubtasksTaskHandler;
	protected MockCycleTaskHandler cycleFinishingHandler;
	protected MockParallelTaskHandler parallelTaskHandler;
	protected MockLongTaskHandler longTaskHandler;

	protected static OperationResult createResult(String test, Trace logger) {
		TestUtil.displayTestTitle(test);
		return new OperationResult(TestQuartzTaskManagerContract.class.getName() + ".test" + test);
	}

	protected void initHandlers() {
		MockCycleTaskHandler cycleHandler = new MockCycleTaskHandler(false);    // ordinary recurring task
		taskManager.registerHandler(CYCLE_TASK_HANDLER_URI, cycleHandler);
		cycleFinishingHandler = new MockCycleTaskHandler(true);                 // finishes the handler
		taskManager.registerHandler(CYCLE_FINISHING_TASK_HANDLER_URI, cycleFinishingHandler);

		singleHandler1 = new MockSingleTaskHandler("1", taskManager);
		taskManager.registerHandler(SINGLE_TASK_HANDLER_URI, singleHandler1);
		singleHandler2 = new MockSingleTaskHandler("2", taskManager);
		taskManager.registerHandler(SINGLE_TASK_HANDLER_2_URI, singleHandler2);
		singleHandler3 = new MockSingleTaskHandler("3", taskManager);
		taskManager.registerHandler(SINGLE_TASK_HANDLER_3_URI, singleHandler3);

		workBucketsTaskHandler = new MockWorkBucketsTaskHandler(null, taskManager);
		taskManager.registerHandler(SINGLE_WB_TASK_HANDLER_URI, workBucketsTaskHandler);

		new PartitionedMockWorkBucketsTaskHandlerCreator(taskManager, prismContext)
				.initializeAndRegister(PARTITIONED_WB_TASK_HANDLER_URI);

		partitionedWorkBucketsTaskHandler = new MockWorkBucketsTaskHandler("p", taskManager);
		taskManager.registerHandler(PARTITIONED_WB_TASK_HANDLER_URI_1, partitionedWorkBucketsTaskHandler);
		taskManager.registerHandler(PARTITIONED_WB_TASK_HANDLER_URI_2, partitionedWorkBucketsTaskHandler);
		taskManager.registerHandler(PARTITIONED_WB_TASK_HANDLER_URI_3, partitionedWorkBucketsTaskHandler);

		l1Handler = new MockSingleTaskHandler("L1", taskManager);
		l2Handler = new MockSingleTaskHandler("L2", taskManager);
		l3Handler = new MockSingleTaskHandler("L3", taskManager);
		taskManager.registerHandler(L1_TASK_HANDLER_URI, l1Handler);
		taskManager.registerHandler(L2_TASK_HANDLER_URI, l2Handler);
		taskManager.registerHandler(L3_TASK_HANDLER_URI, l3Handler);

		waitForSubtasksTaskHandler = new MockSingleTaskHandler("WFS", taskManager);
		taskManager.registerHandler(WAIT_FOR_SUBTASKS_TASK_HANDLER_URI, waitForSubtasksTaskHandler);
		parallelTaskHandler = new MockParallelTaskHandler("1", taskManager);
		taskManager.registerHandler(PARALLEL_TASK_HANDLER_URI, parallelTaskHandler);
		longTaskHandler = new MockLongTaskHandler("1", taskManager);
		taskManager.registerHandler(LONG_TASK_HANDLER_URI, longTaskHandler);
	}

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	public void initialize() throws Exception {
		initHandlers();
		addObjectFromFile(USER_ADMINISTRATOR_FILE);
	}

	protected <T extends ObjectType> PrismObject<T> unmarshallJaxbFromFile(String filePath) throws IOException, JAXBException, SchemaException {
		File file = new File(filePath);
		return PrismTestUtil.parseObject(file);
	}

	protected <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath) throws Exception {
		PrismObject<T> object = unmarshallJaxbFromFile(filePath);
		System.out.println("obj: " + object.getElementName());
		OperationResult result = new OperationResult(TestQuartzTaskManagerContract.class.getName() + ".addObjectFromFile");
		try {
			add(object, result);
		} catch(ObjectAlreadyExistsException e) {
			delete(object, result);
			add(object, result);
		}
		logger.trace("Object from " + filePath + " added to repository.");
		return object;
	}

	protected void add(PrismObject<? extends ObjectType> object, OperationResult result)
			throws ObjectAlreadyExistsException, SchemaException {
		if (object.canRepresent(TaskType.class)) {
			taskManager.addTask((PrismObject)object, result);
		} else {
			repositoryService.addObject(object, null, result);
		}
	}

	protected void delete(PrismObject<? extends ObjectType> object, OperationResult result) throws ObjectNotFoundException, SchemaException {
		if (object.canRepresent(TaskType.class)) {
			taskManager.deleteTask(object.getOid(), result);
		} else {
			repositoryService.deleteObject(ObjectType.class, object.getOid(), result);			// correct?
		}
	}

	protected void waitForTaskClose(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval) throws
			CommonException {
	    waitFor("Waiting for task manager to execute the task", () -> {
	        Task task = taskManager.getTask(taskOid, result);
	        IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
	        return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
	    }, timeoutInterval, sleepInterval);
	}

	protected void waitForTaskCloseCheckingSubtasks(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval) throws
			CommonException {
	    waitFor("Waiting for task manager to execute the task", () -> {
	        Task task = taskManager.getTask(taskOid, result);
		    display("Task tree while waiting", TaskDebugUtil.dumpTaskTree(task, result));
	        if (task.getExecutionStatus() == TaskExecutionStatus.CLOSED) {
	        	display("Task is closed, finishing waiting: " + task);
	        	return true;
	        }
		    List<Task> subtasks = task.listSubtasksDeeply(result);
		    for (Task subtask : subtasks) {
			    if (subtask.getResult().isError()) {
			    	display("Error detected in subtask, finishing waiting: " + subtask);
			    	return true;
			    }
		    }
		    return false;
	    }, timeoutInterval, sleepInterval);
	}

	protected void waitForTaskStart(String oid, OperationResult result, long timeoutInterval, long sleepInterval) throws CommonException {
			waitFor("Waiting for task manager to start the task", () -> {
				Task task = taskManager.getTask(oid, result);
				IntegrationTestTools.display("Task while waiting for task manager to start the task", task);
				return task.getLastRunStartTimestamp() != null && task.getLastRunStartTimestamp() != 0L;
			}, timeoutInterval, sleepInterval);
		}

	protected void waitForTaskProgress(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval,
	        int threshold) throws CommonException {
	    waitFor("Waiting for task manager to execute the task", () -> {
	        Task task = taskManager.getTask(taskOid, result);
	        IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
	        return task.getProgress() >= threshold;
	    }, timeoutInterval, sleepInterval);
	}

	protected void waitForTaskNextRun(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval) throws Exception {
		TaskQuartzImpl taskBefore = taskManager.getTask(taskOid, result);
		waitFor("Waiting for task manager to execute the task", () -> {
			Task task = taskManager.getTask(taskOid, result);
			IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
			return task.getLastRunStartTimestamp() != null &&
					(taskBefore.getLastRunStartTimestamp() == null || task.getLastRunStartTimestamp() > taskBefore.getLastRunStartTimestamp());
		}, timeoutInterval, sleepInterval);
	}


	protected void suspendAndDeleteTasks(String... oids) {
		taskManager.suspendAndDeleteTasks(Arrays.asList(oids), 20000L, true, new OperationResult("dummy"));
	}

	protected void sleepChecked(long delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// nothing to do here
		}
	}

	protected void assertTotalSuccessCount(int expectedCount, Collection<? extends Task> workers) {
		int total = 0;
		for (Task worker : workers) {
			total += worker.getStoredOperationStats().getIterativeTaskInformation().getTotalSuccessCount();
		}
		assertEquals("Wrong total success count", expectedCount, total);
	}

	protected void assertNoWorkBuckets(TaskWorkStateType ws) {
		assertTrue(ws == null || ws.getBucket().isEmpty());
	}

	protected void assertNumericBucket(WorkBucketType bucket, WorkBucketStateType state, int seqNumber, Integer start, Integer end) {
		assertBucket(bucket, state, seqNumber);
		AbstractWorkBucketContentType content = bucket.getContent();
		assertEquals("Wrong bucket content class", NumericIntervalWorkBucketContentType.class, content.getClass());
		NumericIntervalWorkBucketContentType numContent = (NumericIntervalWorkBucketContentType) content;
		assertEquals("Wrong bucket start", toBig(start), numContent.getFrom());
		assertEquals("Wrong bucket end", toBig(end), numContent.getTo());
	}

	protected void assertBucket(WorkBucketType bucket, WorkBucketStateType state, int seqNumber) {
		if (state != null) {
			assertEquals("Wrong bucket state", state, bucket.getState());
		}
		assertEquals("Wrong bucket seq number", seqNumber, bucket.getSequentialNumber());
	}

	protected BigInteger toBig(Integer integer) {
		return integer != null ? BigInteger.valueOf(integer) : null;
	}

	protected void assertOptimizedCompletedBuckets(TaskQuartzImpl task) {
		if (task.getWorkState() == null) {
			return;
		}
		long completed = task.getWorkState().getBucket().stream()
				.filter(b -> b.getState() == WorkBucketStateType.COMPLETE)
				.count();
		if (completed > OPTIMIZED_BUCKETS_THRESHOLD) {
			display("Task with more than one completed bucket", task);
			fail("More than one completed bucket found in task: " + completed + " in " + task);
		}
	}

	protected int getTotalItemsProcessed(String coordinatorTaskOid) {
		OperationResult result = new OperationResult("getTotalItemsProcessed");
		try {
			Task coordinatorTask = taskManager.getTask(coordinatorTaskOid, result);
			List<Task> tasks = coordinatorTask.listSubtasks(result);
			int total = 0;
			for (Task task : tasks) {
				OperationStatsType opStat = task.getStoredOperationStats();
				if (opStat == null) {
					continue;
				}
				IterativeTaskInformationType iti = opStat.getIterativeTaskInformation();
				if (iti == null) {
					continue;
				}
				int count = iti.getTotalSuccessCount();
				display("Task " + task + ": " + count + " items processed");
				total += count;
			}
			return total;
		} catch (Throwable t) {
			throw new AssertionError("Unexpected exception", t);
		}
	}

	protected void assertNumberOfBuckets(TaskQuartzImpl task, Integer expectedNumber) {
		assertEquals("Wrong # of expected buckets", expectedNumber, task.getWorkState().getNumberOfBuckets());
	}
}
