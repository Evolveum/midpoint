/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import static com.evolveum.midpoint.util.MiscUtil.or0;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.waitFor;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

import com.evolveum.midpoint.schema.util.TaskTypeUtil;
import com.evolveum.midpoint.task.quartzimpl.quartz.LocalScheduler;
import com.evolveum.midpoint.task.quartzimpl.tasks.TaskStateManager;

import com.evolveum.midpoint.test.TestResource;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeSuite;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskDebugUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.AbstractSpringTest;
import com.evolveum.midpoint.test.util.InfraTestMixin;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class AbstractTaskManagerTest extends AbstractSpringTest implements InfraTestMixin {

    private static final String CYCLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/cycle-task-handler";
    static final String SINGLE_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-task-handler";
    private static final String SINGLE_TASK_HANDLER_2_URI = "http://midpoint.evolveum.com/test/single-task-handler-2";
    private static final String SINGLE_TASK_HANDLER_3_URI = "http://midpoint.evolveum.com/test/single-task-handler-3";
    private static final String SINGLE_WB_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/single-wb-task-handler";
    private static final String PARTITIONED_WB_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/partitioned-wb-task-handler";
    private static final String PARTITIONED_WB_TASK_HANDLER_URI_1 = PARTITIONED_WB_TASK_HANDLER_URI + "#1";
    private static final String PARTITIONED_WB_TASK_HANDLER_URI_2 = PARTITIONED_WB_TASK_HANDLER_URI + "#2";
    private static final String PARTITIONED_WB_TASK_HANDLER_URI_3 = PARTITIONED_WB_TASK_HANDLER_URI + "#3";
    private static final String L1_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l1-task-handler";
    static final String L2_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l2-task-handler";
    static final String L3_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/l3-task-handler";
    private static final String PARALLEL_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/parallel-task-handler";
    private static final String LONG_TASK_HANDLER_URI = "http://midpoint.evolveum.com/test/long-task-handler";

    public static final String COMMON_DIR = "src/test/resources/common";
    private static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");

    // TODO make configurable. Due to a race condition there can be a small number of unoptimized complete buckets
    // (it should not exceed the number of workers ... at least not by much amount :)
    private static final int OPTIMIZED_BUCKETS_THRESHOLD = 8;

    @Autowired protected RepositoryService repositoryService;
    @Autowired protected TaskManagerQuartzImpl taskManager;
    @Autowired protected TaskStateManager taskStateManager;
    @Autowired protected LocalScheduler localScheduler;
    @Autowired protected PrismContext prismContext;
    @Autowired protected SchemaHelper schemaHelper;

    MockSingleTaskHandler singleHandler1, singleHandler2, singleHandler3;
    MockWorkBucketsTaskHandler workBucketsTaskHandler;
    MockWorkBucketsTaskHandler partitionedWorkBucketsTaskHandler;
    MockSingleTaskHandler l1Handler, l2Handler, l3Handler;
    MockParallelTaskHandler parallelTaskHandler;

    private void initHandlers() {
        MockCycleTaskHandler cycleHandler = new MockCycleTaskHandler();
        taskManager.registerHandler(CYCLE_TASK_HANDLER_URI, cycleHandler);

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

        parallelTaskHandler = new MockParallelTaskHandler("1", taskManager);
        taskManager.registerHandler(PARALLEL_TASK_HANDLER_URI, parallelTaskHandler);
        MockLongTaskHandler longTaskHandler = new MockLongTaskHandler("1", taskManager);
        taskManager.registerHandler(LONG_TASK_HANDLER_URI, longTaskHandler);
    }

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    public void initialize() throws Exception {
        initHandlers();
        addObjectFromFile(USER_ADMINISTRATOR_FILE.getPath());
    }

    <T extends ObjectType> PrismObject<T> add(TestResource<T> testResource, OperationResult result) throws Exception {
        return addObjectFromFile(testResource.file.getAbsolutePath(), result);
    }

    <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath) throws Exception {
        return addObjectFromFile(filePath, createOperationResult("addObjectFromFile"));
    }

    <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath, OperationResult result) throws Exception {
        PrismObject<T> object = PrismTestUtil.parseObject(new File(filePath));
        try {
            add(object, result);
        } catch (ObjectAlreadyExistsException e) {
            delete(object, result);
            add(object, result);
        }
        logger.trace("Object from {} added to repository.", filePath);
        return object;
    }

    protected void add(PrismObject<? extends ObjectType> object, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        if (object.canRepresent(TaskType.class)) {
            //noinspection unchecked,rawtypes
            taskManager.addTask((PrismObject) object, result);
        } else {
            repositoryService.addObject(object, null, result);
        }
    }

    protected void delete(PrismObject<? extends ObjectType> object, OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (object.canRepresent(TaskType.class)) {
            taskManager.deleteTask(object.getOid(), result);
        } else {
            repositoryService.deleteObject(ObjectType.class, object.getOid(), result);            // correct?
        }
    }

    void waitForTaskClose(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval)
            throws CommonException {
        waitFor("Waiting for task to close", () -> {
            Task task = taskManager.getTaskWithResult(taskOid, result);
            IntegrationTestTools.display("Task while waiting for it to close", task);
            return task.getSchedulingState() == TaskSchedulingStateType.CLOSED;
        }, timeoutInterval, sleepInterval);
    }

    void waitForTaskCloseOrDelete(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval)
            throws CommonException {
        waitFor("Waiting for task to close", () -> {
            try {
                Task task = taskManager.getTaskWithResult(taskOid, result);
                IntegrationTestTools.display("Task while waiting for it to close", task);
                return task.getSchedulingState() == TaskSchedulingStateType.CLOSED;
            } catch (ObjectNotFoundException e) {
                return true;
            }
        }, timeoutInterval, sleepInterval);
    }

    @SuppressWarnings("SameParameterValue")
    void waitForTaskReady(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval) throws
            CommonException {
        waitFor("Waiting for task to become runnable", () -> {
            Task task = taskManager.getTaskWithResult(taskOid, result);
            IntegrationTestTools.display("Task while waiting for it to become ready", task);
            return task.isReady();
        }, timeoutInterval, sleepInterval);
    }

    void waitForTaskWaiting(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval) throws
            CommonException {
        waitFor("Waiting for task to become waiting", () -> {
            Task task = taskManager.getTaskWithResult(taskOid, result);
            IntegrationTestTools.display("Task while waiting for it to become waiting", task);
            return task.isWaiting();
        }, timeoutInterval, sleepInterval);
    }

    @SuppressWarnings("SameParameterValue")
    void waitForTaskCloseCheckingSubtasks(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval) throws
            CommonException {
        waitFor("Waiting for task manager to execute the task", () -> {
            Task task = taskManager.getTaskWithResult(taskOid, result);
            displayValue("Task tree while waiting", TaskDebugUtil.dumpTaskTree(task, result));
            if (task.isClosed()) {
                display("Task is closed, finishing waiting: " + task);
                return true;
            }
            List<? extends Task> subtasks = task.listSubtasksDeeply(result);
            for (Task subtask : subtasks) {
                if (subtask.getResultStatus() == OperationResultStatusType.FATAL_ERROR
                        || subtask.getResultStatus() == OperationResultStatusType.PARTIAL_ERROR) {
                    display("Error detected in subtask, finishing waiting: " + subtask);
                    return true;
                }
            }
            return false;
        }, timeoutInterval, sleepInterval);
    }

    protected void waitForTaskStart(String oid, OperationResult result, long timeoutInterval, long sleepInterval) throws CommonException {
        waitFor("Waiting for task manager to start the task", () -> {
            Task task = taskManager.getTaskWithResult(oid, result);
            IntegrationTestTools.display("Task while waiting for task manager to start the task", task);
            return task.getLastRunStartTimestamp() != null && task.getLastRunStartTimestamp() != 0L;
        }, timeoutInterval, sleepInterval);
    }

    void waitForTaskProgress(String taskOid, OperationResult result, long timeoutInterval, long sleepInterval,
            int threshold) throws CommonException {
        waitFor("Waiting for task progress reaching " + threshold, () -> {
            Task task = taskManager.getTaskWithResult(taskOid, result);
            IntegrationTestTools.display("Task while waiting for progress reaching " + threshold, task);
            return task.getProgress() >= threshold;
        }, timeoutInterval, sleepInterval);
    }

    void suspendAndDeleteTasks(String... oids) {
        taskManager.suspendAndDeleteTasks(Arrays.asList(oids), 20000L, true, new OperationResult("dummy"));
    }

    void sleepChecked(long delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            // nothing to do here
        }
    }

    void assertTotalSuccessCount(int expectedCount, Collection<? extends Task> workers) {
        int successCount = workers.stream()
                .mapToInt(w -> TaskTypeUtil.getItemsProcessedWithSuccess(w.getStoredOperationStats()))
                .sum();
        assertThat(successCount).isEqualTo(expectedCount);
    }

    void assertNoWorkBuckets(TaskWorkStateType ws) {
        assertTrue(ws == null || ws.getBucket().isEmpty());
    }

    void assertNumericBucket(WorkBucketType bucket, WorkBucketStateType state, int seqNumber, Integer start, Integer end) {
        assertBucket(bucket, state, seqNumber);
        AbstractWorkBucketContentType content = bucket.getContent();
        assertEquals("Wrong bucket content class", NumericIntervalWorkBucketContentType.class, content.getClass());
        NumericIntervalWorkBucketContentType numContent = (NumericIntervalWorkBucketContentType) content;
        assertEquals("Wrong bucket start", toBig(start), numContent.getFrom());
        assertEquals("Wrong bucket end", toBig(end), numContent.getTo());
    }

    void assertBucket(WorkBucketType bucket, WorkBucketStateType state, int seqNumber) {
        if (state != null) {
            assertEquals("Wrong bucket state", state, bucket.getState());
        }
        assertBucketWorkerRefSanity(bucket);
        assertEquals("Wrong bucket seq number", seqNumber, bucket.getSequentialNumber());
    }

    private void assertBucketWorkerRefSanity(WorkBucketType bucket) {
        switch (defaultIfNull(bucket.getState(), WorkBucketStateType.READY)) {
            case READY:
                assertNull("workerRef present in " + bucket, bucket.getWorkerRef());
                break;
            case DELEGATED:
                assertNotNull("workerRef not present in " + bucket, bucket.getWorkerRef());
                break;
            case COMPLETE:
                break;      // either one is OK
            default:
                fail("Wrong state: " + bucket.getState());
        }
    }

    private BigInteger toBig(Integer integer) {
        return integer != null ? BigInteger.valueOf(integer) : null;
    }

    void assertOptimizedCompletedBuckets(TaskQuartzImpl task) {
        if (task.getWorkState() == null) {
            return;
        }
        long completed = task.getWorkState().getBucket().stream()
                .filter(b -> b.getState() == WorkBucketStateType.COMPLETE)
                .count();
        if (completed > OPTIMIZED_BUCKETS_THRESHOLD) {
            displayDumpable("Task with more than one completed bucket", task);
            fail("More than one completed bucket found in task: " + completed + " in " + task);
        }
    }

    int getTotalItemsProcessed(String coordinatorTaskOid) {
        OperationResult result = new OperationResult("getTotalItemsProcessed");
        try {
            Task coordinatorTask = taskManager.getTaskPlain(coordinatorTaskOid, result);
            List<? extends Task> tasks = coordinatorTask.listSubtasks(result);
            int total = 0;
            for (Task task : tasks) {
                int count = or0(TaskTypeUtil.getItemsProcessed(task.getStoredOperationStats()));
                display("Task " + task + ": " + count + " items processed");
                total += count;
            }
            return total;
        } catch (Throwable t) {
            throw new AssertionError("Unexpected exception", t);
        }
    }

    void assertNumberOfBuckets(TaskQuartzImpl task, Integer expectedNumber) {
        assertEquals("Wrong # of expected buckets", expectedNumber, task.getWorkState().getNumberOfBuckets());
    }

    Collection<SelectorOptions<GetOperationOptions>> retrieveItemsNamed(Object... items) {
        return schemaHelper.getOperationOptionsBuilder()
                .items(items).retrieve()
                .build();
    }

    void assertCachingProfiles(Task task, String... expectedProfiles) {
        Set<String> realProfiles = getCachingProfiles(task);
        assertEquals("Wrong caching profiles in " + task, new HashSet<>(Arrays.asList(expectedProfiles)), realProfiles);
    }

    private Set<String> getCachingProfiles(Task task) {
        TaskExecutionEnvironmentType env = task.getExecutionEnvironment();
        return env != null ? new HashSet<>(env.getCachingProfile()) : Collections.emptySet();
    }

    public void displayValue(String title, Object value) {
        PrismTestUtil.display(title, value);
    }

    @NotNull
    TaskQuartzImpl createTaskFromFile(String filePath, OperationResult result) throws Exception {
        return taskManager.createTaskInstance(addObjectFromFile(filePath), result);
    }
}
