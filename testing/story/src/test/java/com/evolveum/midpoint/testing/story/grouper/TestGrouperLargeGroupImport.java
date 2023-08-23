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
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

/**
 * Test import of large groups.
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestGrouperLargeGroupImport extends AbstractGrouperTest {

    private static final int ALUMNI_USERS = 10;
    private static final int STAFF_USERS = 100000;

    private static final TestResource<TaskType> TASK_IMPORT_GROUPS = new TestResource<>(TEST_DIR, "task-import-groups.xml", "e2edaa90-7624-4a82-8a7c-92e00c78d8d3");

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
    public void test100ImportGroups() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();
        task.setOwner(userAdministrator);

        addObject(TASK_IMPORT_GROUPS, task, result);

        Thread.sleep(60000L);           // leave the import task alone ... at least for a minute

        Task importTaskAfter = waitForTaskFinish(TASK_IMPORT_GROUPS.oid, 120000);
        assertSuccess(importTaskAfter.getResult());
    }
}
