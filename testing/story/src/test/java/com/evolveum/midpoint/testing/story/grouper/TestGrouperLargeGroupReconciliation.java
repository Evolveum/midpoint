/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.grouper;

import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * Test reconciliation of large groups.
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestGrouperLargeGroupReconciliation extends AbstractGrouperTest {

    private static final int ALUMNI_USERS = 10;
    private static final int STAFF_USERS = 100000;

    private static final TestResource TASK_RECONCILE_GROUPS = new TestResource(TEST_DIR, "task-reconcile-groups.xml", "1fde833d-7105-40fb-b59a-c863a1f53609");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        InternalsConfig.setConsistencyChecks(false);
        createGroup(ALUMNI_ID, ALUMNI_NAME, ALUMNI_USERS);
        createGroup(STAFF_ID, STAFF_NAME, STAFF_USERS);
        //setGlobalTracingOverride(createPerformanceTracingProfile());
        //setGlobalTracingOverride(createDefaultTracingProfile());
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) {
        // we don't need these here
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        assertSuccess(modelService.testResource(RESOURCE_LDAP.oid, task, task.getResult()));
        assertSuccess(modelService.testResource(RESOURCE_GROUPER.oid, task, task.getResult()));
    }

    @Test
    public void test100ReconcileGroups() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        task.setOwner(userAdministrator);

        addObject(TASK_RECONCILE_GROUPS, task, result);

        Thread.sleep(5000L);           // leave the reconciliation task alone ... at least for a minute

        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_GROUPS.oid, 180000);
        assertSuccess(taskAfter.getResult());
    }

    @Test
    public void test110ReconcileGroupsAgain() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        task.setOwner(userAdministrator);

        deleteGroupMember(ALUMNI_NAME, 3);
        addGroupMember(ALUMNI_NAME, 13);
        deleteGroupMember(STAFF_NAME, 4);
        addGroupMember(STAFF_NAME, STAFF_USERS + 4);

        restartTask(TASK_RECONCILE_GROUPS.oid);

        Thread.sleep(5000L);           // leave the reconciliation task alone ... at least for a minute

        Task taskAfter = waitForTaskFinish(TASK_RECONCILE_GROUPS.oid, 180000);
        assertSuccess(taskAfter.getResult());
    }
}
