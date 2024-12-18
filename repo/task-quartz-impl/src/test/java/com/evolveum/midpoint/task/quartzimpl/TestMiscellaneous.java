/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.xml.namespace.QName;

import org.quartz.JobBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManagerConfigurationException;
import com.evolveum.midpoint.task.quartzimpl.cluster.NodeRegistrar;
import com.evolveum.midpoint.task.quartzimpl.quartz.QuartzUtil;
import com.evolveum.midpoint.task.quartzimpl.quartz.TaskSynchronizer;
import com.evolveum.midpoint.task.quartzimpl.run.JobExecutor;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.TestTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends AbstractTaskManagerTest {

    private static final File TEST_DIR = new File("src/test/resources/miscellaneous");
    private static final TestObject<TaskType> TASK_42_RUNNABLE = TestObject.file(TEST_DIR, "task-42-runnable.xml", "e3a64c81-5dd0-4b66-bc5a-572425eb5a63");
    private static final TestObject<TaskType> TASK_42_SUSPENDED = TestObject.file(TEST_DIR, "task-42-suspended.xml", "12125ca4-7107-437d-a448-facae780a306");
    private static final TestObject<TaskType> TASK_42_CLOSED = TestObject.file(TEST_DIR, "task-42-closed.xml", "c56bf227-8e03-4a52-b873-0ac651b95ed6");
    private static final TestObject<TaskType> TASK_42_WAITING = TestObject.file(TEST_DIR, "task-42-waiting.xml", "c9bdc85b-27d0-43f7-8b2a-1e44d1d23594");

    @Autowired private TaskSynchronizer taskSynchronizer;

    @Test
    public void test010SynchronizationOfTasksMissingInQuartz()
            throws SchemaException, ObjectAlreadyExistsException, SchedulerException, ObjectNotFoundException {
        given("Two tasks in repo, one task in Quartz");
        Task task = getTestTask();
        OperationResult result = task.getResult();
        Set<String> tasksOids = addDummyFutureTasks(2, result);
        final Scheduler scheduler = this.localScheduler.getQuartzScheduler();
        // Delete one of tasks from quartz only
        scheduler.deleteJob(QuartzUtil.createJobKeyForTaskOid(tasksOids.iterator().next()));

        when("Task synchronization finishes");
        this.taskManager.synchronizeTasks(result);

        then("Quartz scheduler should have two tasks");
        int jobKeysInQuartz = scheduler
                .getJobKeys(GroupMatcher.jobGroupEquals(JobKey.DEFAULT_GROUP)).size();
        assertThat(jobKeysInQuartz).isEqualTo(2);

        // Cleanup
        for (String oid : tasksOids) {
            this.taskManager.deleteTask(oid, result);
        }
    }

    @Test
    public void test020SynchronizationOfTasksMissingInRepository()
            throws SchemaException, ObjectAlreadyExistsException, SchedulerException, ObjectNotFoundException {
        given("One task in repo, two tasks in Quartz");
        Task task = getTestTask();
        OperationResult result = task.getResult();
        final Set<String> tasksOids = addDummyFutureTasks(1, result);
        final Scheduler scheduler = this.localScheduler.getQuartzScheduler();
        // Add one more task, but only to Quartz
        scheduler.addJob(JobBuilder.newJob(JobExecutor.class)
                .withIdentity(UUID.randomUUID().toString())
                .storeDurably()
                .requestRecovery()
                .build(), false);

        when("Task synchronization finishes");
        this.taskManager.synchronizeTasks(result);

        then("Quartz scheduler should have only one task");
        int jobKeysInQuartz = scheduler
                .getJobKeys(GroupMatcher.jobGroupEquals(JobKey.DEFAULT_GROUP)).size();
        assertThat(jobKeysInQuartz).isEqualTo(1);

        // Cleanup
        for (String oid : tasksOids) {
            this.taskManager.deleteTask(oid, result);
        }
    }

    /**
     * Test of tasks synchronization when there are more than 10 000 tasks
     *
     * Reproduces MID-10213
     */
    @Test(enabled = false, description = "Used only to reproduce MID-10213. It's a long running test > 1 minute")
    public void test030SynchronizationOfBigNumberOfTasks() throws CommonException, SchedulerException {
        given("More than 10 000 tasks are added");
        Task task = getTestTask();
        OperationResult result = task.getResult();
        int numberOfDummyTasks = 11000;
        final Set<String> tasksOids = addDummyFutureTasks(numberOfDummyTasks, result);

        when("Task synchronization finishes");
        this.taskManager.synchronizeTasks(result);

        then("Quartz scheduler should have the same amount of tasks as were added");
        int jobKeysInQuartz = this.localScheduler.getQuartzScheduler()
                .getJobKeys(GroupMatcher.jobGroupEquals(JobKey.DEFAULT_GROUP)).size();
        assertThat(jobKeysInQuartz).isEqualTo(numberOfDummyTasks);

        // Cleanup
        for (String oid : tasksOids) {
            this.taskManager.deleteTask(oid, result);
        }
    }

    @Test
    public void test100ParsingTaskExecutionLimitations() throws TaskManagerConfigurationException, SchemaException {
        assertLimitationsParsed(" ", emptyList());
        assertLimitationsParsed("_   ", singletonList(new TaskGroupExecutionLimitationType().groupName("_")));
        assertLimitationsParsed("#,   _   ", Arrays.asList(new TaskGroupExecutionLimitationType().groupName("#"), new TaskGroupExecutionLimitationType().groupName("_")));
        assertLimitationsParsed(":    0", singletonList(new TaskGroupExecutionLimitationType().groupName("").limit(0)));
        assertLimitationsParsed("_:0", singletonList(new TaskGroupExecutionLimitationType().groupName("_").limit(0)));
        assertLimitationsParsed("_:*", singletonList(new TaskGroupExecutionLimitationType().groupName("_").limit(null)));
        assertLimitationsParsed("admin-node : 2 , sync-jobs    : 4    ", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("admin-node").limit(2),
                new TaskGroupExecutionLimitationType().groupName("sync-jobs").limit(4)));
        assertLimitationsParsed("admin-node:2,sync-jobs:4,#:0,_:0,*:*", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("admin-node").limit(2),
                new TaskGroupExecutionLimitationType().groupName("sync-jobs").limit(4),
                new TaskGroupExecutionLimitationType().groupName("#").limit(0),
                new TaskGroupExecutionLimitationType().groupName("_").limit(0),
                new TaskGroupExecutionLimitationType().groupName("*").limit(null)));
    }

    @Test
    public void test110ComputingLimitations() throws TaskManagerConfigurationException, SchemaException {
        assertLimitationsComputed("", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("_", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("_:1", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("").limit(1),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("#,_", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed(":0", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("").limit(0),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("_:0", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("").limit(0),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("_:*", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("admin-node:2,sync-jobs:4", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("admin-node").limit(2),
                new TaskGroupExecutionLimitationType().groupName("sync-jobs").limit(4),
                new TaskGroupExecutionLimitationType().groupName(""),
                new TaskGroupExecutionLimitationType().groupName("NODE"),
                new TaskGroupExecutionLimitationType().groupName("*").limit(0)
        ));
        assertLimitationsComputed("admin-node:2,sync-jobs:4,#:0,_:0,*:*", Arrays.asList(
                new TaskGroupExecutionLimitationType().groupName("admin-node").limit(2),
                new TaskGroupExecutionLimitationType().groupName("sync-jobs").limit(4),
                new TaskGroupExecutionLimitationType().groupName("NODE").limit(0),
                new TaskGroupExecutionLimitationType().groupName("").limit(0),
                new TaskGroupExecutionLimitationType().groupName("*")));
    }

    private void assertLimitationsParsed(String value, List<TaskGroupExecutionLimitationType> expected) throws TaskManagerConfigurationException, SchemaException {
        TaskExecutionLimitationsType parsed = TaskManagerConfiguration.parseExecutionLimitations(value);
        displayValue("parsed value of '" + value + "'", serialize(parsed));
        assertEquals("Wrong parsed value for '" + value + "'", expected, parsed.getGroupLimitation());
    }

    private void assertLimitationsComputed(String value, List<TaskGroupExecutionLimitationType> expected) throws TaskManagerConfigurationException, SchemaException {
        TaskExecutionLimitationsType parsed = TaskManagerConfiguration.parseExecutionLimitations(value);
        displayValue("parsed value of '" + value + "'", serialize(parsed));
        TaskExecutionLimitationsType computed = NodeRegistrar.computeTaskExecutionLimitations(parsed, "NODE");
        assertEquals("Wrong computed value for '" + value + "'", expected, computed.getGroupLimitation());
    }

    @Test
    public void test200TaskMigrationOnStart() throws Exception {
        given();
        OperationResult result = createOperationResult();

        repoAdd(TASK_42_RUNNABLE, result);
        repoAdd(TASK_42_SUSPENDED, result);
        repoAdd(TASK_42_CLOSED, result);
        repoAdd(TASK_42_WAITING, result);

        when();
        taskSynchronizer.synchronizeJobStores(result);

        then();
        Task suspended = taskManager.getTaskPlain(TASK_42_SUSPENDED.oid, result);
        Task waiting = taskManager.getTaskPlain(TASK_42_WAITING.oid, result);
        Task closed = taskManager.getTaskPlain(TASK_42_CLOSED.oid, result);
        assertSchedulingState(suspended, TaskSchedulingStateType.SUSPENDED);
        assertSchedulingState(waiting, TaskSchedulingStateType.WAITING);
        assertSchedulingState(closed, TaskSchedulingStateType.CLOSED);

        // this one should be started, so wa cannot assert its state (can be ready or closed)
        waitForTaskClose(TASK_42_RUNNABLE.oid, result, 10000);
    }

    /**
     * Here we check if we preserve resource names in provisioning statistics.
     *
     * See MID-7771.
     */
    @Test
    public void test210ResourceNamesInStatistics() throws Exception {
        OperationResult result = createOperationResult();

        given("a task with resource name in statistics");
        ObjectReferenceType resourceRef = new ObjectReferenceType()
                .oid("216bdb8a-1fcc-47be-a227-7184972a3b3e")
                .type(ResourceType.COMPLEX_TYPE)
                .targetName("resource1");
        TaskType task = new TaskType()
                .name("task1")
                .ownerRef(SystemObjectsType.USER_ADMINISTRATOR.value(), UserType.COMPLEX_TYPE)
                .operationStats(new OperationStatsType()
                        .environmentalPerformanceInformation(new EnvironmentalPerformanceInformationType()
                                .provisioningStatistics(new ProvisioningStatisticsType()
                                        .entry(new ProvisioningStatisticsEntryType()
                                                .resourceRef(resourceRef)))));

        when("task is stored in repo and retrieved");
        String oid = taskManager.addTask(task.asPrismObject(), null, result);
        Task retrieved = taskManager.getTaskPlain(oid, result);

        then("the resource name should be preserved");
        assertThat(getOrig(retrieved.getStoredOperationStatsOrClone()
                .getEnvironmentalPerformanceInformation()
                .getProvisioningStatistics()
                .getEntry().get(0).getResourceRef().getTargetName()))
                .as("stored resource name")
                .isEqualTo("resource1");
    }

    private String serialize(TaskExecutionLimitationsType parsed) throws SchemaException {
        return getPrismContext().xmlSerializer().serializeRealValue(parsed, new QName(SchemaConstants.NS_C, "value"));
    }

    /** MID-9423. */
    @Test
    public void test220TaskWithoutIdentifier() throws Exception {
        var result = createOperationResult();

        when("task without identifier is added");
        TaskType task = new TaskType()
                .name(getTestNameShort())
                .ownerRef(SystemObjectsType.USER_ADMINISTRATOR.value(), UserType.COMPLEX_TYPE)
                .executionState(TaskExecutionStateType.RUNNABLE)
                .handlerUri(MOCK_TASK_HANDLER_URI);
        repositoryService.addObject(task.asPrismObject(), null, result);

        and("tasks are synchronized");
        taskManager.synchronizeTasks(result);

        then("task is started and correctly finishes");
        waitForTaskCloseOrSuspend(task.getOid(), 10000);

        assertTask(task.getOid(), "after")
                .display()
                .assertClosed()
                .assertSuccess();
    }

    private Set<String> addDummyFutureTasks(int numberOfTasks, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        Set<String> oids = new HashSet<>();
        for (int i = 0; i < numberOfTasks; i++) {
            final TaskType taskType = new TaskType();
            final String uuid = UUID.randomUUID().toString();
            taskType.oid(uuid);
            taskType.ownerRef(SystemObjectsType.USER_ADMINISTRATOR.value(), UserType.COMPLEX_TYPE);
            taskType.name(uuid);
            taskType.schedulingState(TaskSchedulingStateType.READY);
            // Schedule task for execution 48 hours from now, to prevent Quartz executing it immediately.
            taskType.schedule(new ScheduleType().earliestStartTime(
                    Instant.now().plus(48, ChronoUnit.HOURS).toString()));
            final TestTask testTask = TestTask.of(taskType);
            this.taskManager.addTask(testTask.getFresh(), result);
            oids.add(uuid);
        }
        return oids;
    }
}
