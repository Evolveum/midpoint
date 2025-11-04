/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.tasks;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskActivityPolicies extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/tasks/thresholds");

    private static final TestObject<TaskType> TASK_NON_ITERATIVE_RESTART =
            TestObject.file(TEST_DIR, "task-non-iterative-restart.xml", "d5c0d175-ebda-4506-821d-6205eeae85cf");

    private static final TestObject<RoleType> TASK_RECONCILIATION_COMPOSITE =
            TestObject.file(TEST_DIR, "task-reconciliation-composite.xml", "ccd6df6c-123a-4d9d-b48e-e4de9bf3f2e2");

    private static final DummyTestResource RESOURCE_DUMMY = new DummyTestResource(
            TEST_DIR,
            "dummy.xml",
            "8f82e457-6c6e-42d7-a433-1a346b1899ee",
            "resource-dummy",
            TestTaskActivityPolicies::populateWithSchema);

    private DummyResourceContoller dummyResourceCtl;

    private static final String DUMMY_NOTIFICATION_TRANSPORT = "activityPolicyRuleNotifier";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        dummyResourceCtl = initDummyResource(RESOURCE_DUMMY, initTask, initResult);

        dummyResourceCtl.addAccount("jdoe", "John Doe");
        dummyResourceCtl.addAccount("jsmith", "Jim Smith");
        dummyResourceCtl.addAccount("jblack", "Jack Black");
        dummyResourceCtl.addAccount("wwhite", "William White");

        SimpleActivityPolicyRuleNotifierType notifier = new SimpleActivityPolicyRuleNotifierType();
        notifier.getTransport().add("dummy:" + DUMMY_NOTIFICATION_TRANSPORT);
        EventHandlerType handler = new EventHandlerType();
        handler.getSimpleActivityPolicyRuleNotifier().add(notifier);

        modifyObjectAddContainer(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                ItemPath.create(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION, NotificationConfigurationType.F_HANDLER),
                initTask,
                initResult,
                handler);
    }

    private static void populateWithSchema(DummyResourceContoller controller) throws Exception {
        controller.populateWithDefaultSchema();
    }

    @BeforeMethod
    public void beforeMethod() {
        prepareNotifications();
    }

    /**
     * This test fails on OSX, when JDBC store is used for quartz via `midpoint.taskManager.clustered=true`,
     * `midpoint.taskManager.jdbcJobStore=true` and `midpoint.nodeId=node1` is defined.
     *
     * Quartz seems to ignore {@link org.quartz.DisallowConcurrentExecution} and starts
     * {@link com.evolveum.midpoint.task.quartzimpl.run.JobExecutor} twice in parallel (under 10millis) for the same task.
     *
     * JobExecutor is started twice because of two simple triggers created few millis apart via
     * {@link com.evolveum.midpoint.task.quartzimpl.tasks.ScheduleNowHelper#scheduleWaitingTaskNow(TaskQuartzImpl, boolean, OperationResult)}.
     */
    @Test(enabled = false)
    public void test200ReconciliationComposite() throws Exception {
        clearAfterReconciliation();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        TASK_RECONCILIATION_COMPOSITE.reset();
        deleteIfPresent(TASK_RECONCILIATION_COMPOSITE, result);

        addObject(TASK_RECONCILIATION_COMPOSITE, task, result);

        waitForTaskCloseOrSuspend(TASK_RECONCILIATION_COMPOSITE.oid, 300000L);

        System.out.println(PrismTestUtil.serializeAnyData(getTask(TASK_RECONCILIATION_COMPOSITE.oid).asObjectable().getActivityState(), TaskType.F_ACTIVITY_STATE));
    }

    private void clearAfterReconciliation() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = PrismTestUtil.getPrismContext()
                .queryFor(UserType.class)
                .item(UserType.F_SUBTYPE).eq("reconciliation")
                .build();

        AtomicInteger counter = new AtomicInteger(0);
        repositoryService.searchObjects(UserType.class, query, null, result)
                .forEach(user -> {
                    try {
                        deleteObjectRaw(UserType.class, user.getOid(), task, result);
                        counter.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        repositoryService.searchObjects(ShadowType.class, null, null, result)
                .forEach(shadow -> {
                    try {
                        deleteObjectRaw(ShadowType.class, shadow.getOid(), task, result);
                        counter.incrementAndGet();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        System.out.println("Deleted " + counter.get() + " objects after reconciliation.");
    }
}
