/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;

import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Tests reporting of task state, progress, and errors.
 *
 * UNFINISHED. DO NOT RUN (YET).
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestTaskReporting extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/reporting");

    private static final String ATTR_NUMBER = "number";
    private static final String ATTR_NAME = "name";

    private static final DummyTestResource RESOURCE_DUMMY_SOURCE = new DummyTestResource(TEST_DIR,
            "resource-source.xml", "a1c7dcb8-07f8-4626-bea7-f10d9df7ec9f", "source",
            controller -> {
                // This is extra secondary identifier. We use it to induce schema exceptions during shadow pre-processing.
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        ATTR_NUMBER, Integer.class, false, false);
                controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                        DummyAccount.ATTR_FULLNAME_NAME, String.class, true, false);
            });

    private static final DummyTestResource RESOURCE_DUMMY_TARGET = new DummyTestResource(TEST_DIR,
            "resource-target.xml", "f1859897-0c10-430e-aefe-7ced49d14a23", "target");
    private static final TestResource<RoleType> ROLE_TARGET = new TestResource<>(TEST_DIR, "role-target.xml", "fdcd5c7a-86c0-4a0e-8b22-dda79183fcf3");
    private static final TestResource<TaskType> TASK_IMPORT = new TestResource<>(TEST_DIR, "task-import.xml", "e06f3f5c-4acc-4c6a-baa3-5c7a954ce4e9");

    private static final int USERS = 10;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_SOURCE, initTask, initResult);
        initDummyResource(RESOURCE_DUMMY_TARGET, initTask, initResult);
        addObject(ROLE_TARGET, initTask, initResult);

        assertSuccess(modelService.testResource(RESOURCE_DUMMY_SOURCE.oid, initTask));
        assertSuccess(modelService.testResource(RESOURCE_DUMMY_TARGET.oid, initTask));

        for (int i = 0; i < USERS; i++) {
            String name = String.format("u-%06d", i);
            DummyAccount account = RESOURCE_DUMMY_SOURCE.controller.addAccount(name);
            account.addAttributeValue(ATTR_NUMBER, i);
       }
    }

    /**
     * Tests the state when the searchObject call itself fails.
     * The task has basically nothing to do here: it cannot continue processing objects.
     * (Except for modifying the query to avoid poisoned object or objects.)
     */
    @Test(enabled = false)
    public void test090ImportWithSearchFailing() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = RESOURCE_DUMMY_SOURCE.controller.getDummyResource().getAccountByUsername("u-000003");
        account.setName(null); // This causes a failure during query execution (not even in the results handler).

        when();
        addObject(TASK_IMPORT, task, result);
        Task importTask = waitForTaskFinish(TASK_IMPORT.oid, true);

        then();
        stabilize();
        assertTask(importTask, "import task after")
                .assertSuccess()
                .display();
    }

    @Test
    public void test100Import() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();
        addObject(TASK_IMPORT, task, result);
        Task importTask = waitForTaskFinish(TASK_IMPORT.oid, true);

        then();
        stabilize();
        assertTask(importTask, "import task after")
                .display()
                .assertSuccess();
    }

    @Test
    public void test110ImportWithBrokenAccount() throws Exception {
        given();
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = RESOURCE_DUMMY_SOURCE.controller.getDummyResource().getAccountByUsername("u-000003");
        account.replaceAttributeValue(ATTR_NUMBER, "WRONG"); // Will cause problem when updating shadow

        when();
        rerunTask(TASK_IMPORT.oid, result);

        then();
        stabilize();
        assertTask(TASK_IMPORT.oid, "import task after")
                .display()
                .assertSuccess();
    }
}
