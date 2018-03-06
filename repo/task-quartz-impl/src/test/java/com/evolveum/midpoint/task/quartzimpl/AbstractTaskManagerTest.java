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
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;

import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;

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
	protected static final String L1_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l1-task-handler";
	protected static final String L2_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l2-task-handler";
	protected static final String L3_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l3-task-handler";
	protected static final String WAIT_FOR_SUBTASKS_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/wait-for-subtasks-task-handler";
	protected static final String PARALLEL_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/parallel-task-handler";
	protected static final String LONG_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/long-task-handler";

	private static final String USER_ADMINISTRATOR_FILE = "src/test/resources/common/user-administrator.xml";

	@Autowired protected RepositoryService repositoryService;
	@Autowired protected TaskManagerQuartzImpl taskManager;
	@Autowired protected PrismContext prismContext;

	protected MockSingleTaskHandler singleHandler1, singleHandler2, singleHandler3;
	protected MockWorkBucketsTaskHandler workBucketsTaskHandler1;
	protected MockSingleTaskHandler l1Handler, l2Handler, l3Handler;
	protected MockSingleTaskHandler waitForSubtasksTaskHandler;
	protected MockCycleTaskHandler cycleFinishingHandler;
	protected MockParallelTaskHandler parallelTaskHandler;
	protected MockLongTaskHandler longTaskHandler;

	protected static OperationResult createResult(String test, Trace logger) {
		System.out.println("===[ test"+test+" ]===");
		logger.info("===[ test"+test+" ]===");
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

		workBucketsTaskHandler1 = new MockWorkBucketsTaskHandler("1", taskManager);
		taskManager.registerHandler(SINGLE_WB_TASK_HANDLER_URI, workBucketsTaskHandler1);

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

	protected void waitForTaskClose(String taskOid, OperationResult result, int timeoutInterval, int sleepInterval) throws
			CommonException {
	    waitFor("Waiting for task manager to execute the task", () -> {
	        Task task = taskManager.getTask(taskOid, result);
	        IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
	        return task.getExecutionStatus() == TaskExecutionStatus.CLOSED;
	    }, timeoutInterval, sleepInterval);
	}

	protected void waitForTaskStart(String oid, OperationResult result, int timeoutInterval, int sleepInterval) throws CommonException {
			waitFor("Waiting for task manager to start the task", () -> {
				Task task = taskManager.getTask(oid, result);
				IntegrationTestTools.display("Task while waiting for task manager to start the task", task);
				return task.getLastRunStartTimestamp() != null && task.getLastRunStartTimestamp() != 0L;
			}, timeoutInterval, sleepInterval);
		}

	protected void waitForTaskProgress(String taskOid, OperationResult result, int timeoutInterval, int sleepInterval,
	        int threshold) throws CommonException {
	    waitFor("Waiting for task manager to execute the task", () -> {
	        Task task = taskManager.getTask(taskOid, result);
	        IntegrationTestTools.display("Task while waiting for task manager to execute the task", task);
	        return task.getProgress() >= threshold;
	    }, timeoutInterval, sleepInterval);
	}
}
