/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.task.quartzimpl;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.quartzimpl.quartz.TaskSynchronizer;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.TaskManagerConfigurationException;
import com.evolveum.midpoint.task.quartzimpl.cluster.NodeRegistrar;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

@ContextConfiguration(locations = { "classpath:ctx-task-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends AbstractTaskManagerTest {

    private static final File TEST_DIR = new File("src/test/resources/miscellaneous");
    private static final TestObject<TaskType> TASK_42_RUNNABLE = TestObject.file(TEST_DIR, "task-42-runnable.xml", "e3a64c81-5dd0-4b66-bc5a-572425eb5a63");
    private static final TestObject<TaskType> TASK_42_SUSPENDED = TestObject.file(TEST_DIR, "task-42-suspended.xml", "12125ca4-7107-437d-a448-facae780a306");
    private static final TestObject<TaskType> TASK_42_CLOSED = TestObject.file(TEST_DIR, "task-42-closed.xml", "c56bf227-8e03-4a52-b873-0ac651b95ed6");
    private static final TestObject<TaskType> TASK_42_WAITING = TestObject.file(TEST_DIR, "task-42-waiting.xml", "c9bdc85b-27d0-43f7-8b2a-1e44d1d23594");

    @Autowired private TaskSynchronizer taskSynchronizer;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
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
}
